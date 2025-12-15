package be.sgl.backend.service.user.sync

import be.sgl.backend.alert.AlertCode
import be.sgl.backend.alert.AlertLogger
import be.sgl.backend.dto.user.ExternalMember
import be.sgl.backend.repository.membership.MembershipRepository
import be.sgl.backend.repository.user.UserRepository
import be.sgl.backend.exception.UserNotFoundException
import be.sgl.backend.util.ForExternalOrganization
import be.sgl.backend.util.I18nUtil.Companion.i18n
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter

@Component
@ForExternalOrganization
class SyncService {

    private val logger = KotlinLogging.logger {}

    @Autowired
    private lateinit var fetchExternalData: FetchExternalData
    @Autowired
    private lateinit var createUserForExternalMember: CreateUserForExternalMember
    @Autowired
    private lateinit var fetchExternalMembersById: FetchExternalMembersById
    @Autowired
    private lateinit var fetchUsersWithExternalOpenRegistrations: FetchUsersWithExternalOpenRegistrations
    @Autowired
    private lateinit var membershipRepository: MembershipRepository
    @Autowired
    private lateinit var checkForNewMemberId: CheckForNewMemberId
    @Autowired
    private lateinit var acceptExternalMembershipRequest: AcceptExternalMembershipRequest
    @Autowired
    private lateinit var removeAllExternalFunctions: RemoveAllExternalFunctions
    @Autowired
    private lateinit var checkOutOfSyncExternalFunctions: CheckOutOfSyncExternalFunctions
    @Autowired
    private lateinit var userRepository: UserRepository
    @Autowired
    private lateinit var alertLogger: AlertLogger

    fun syncUser(username: String) {
        val user = userRepository.findByUsername(username) ?: throw UserNotFoundException(username)
        fetchExternalData.execute(user)
    }

    fun syncUsers(sseEmitter: SseEmitter) {
        logger.info { "Syncing users..." }
        val externalMembers = fetchExternalMembersById.execute()
        userRepository.findAll().forEach { user ->
            sseEmitter.send(i18n("sync.users.update.user", user.id))
            fetchExternalData.execute(user)
            user.externalId?.let(externalMembers::remove)
        }
        externalMembers.forEach { (externalId, _) ->
            sseEmitter.send(i18n("sync.users.create.user", externalId))
            createUserForExternalMember.execute(externalId)
        }
    }

    fun syncMembers(sseEmitter: SseEmitter) {
        logger.info { "Syncing members..." }
        val externalMembers = fetchExternalMembersById.execute()
        val usersWithOpenRegistrations = fetchUsersWithExternalOpenRegistrations.execute()
        membershipRepository.getCurrent().forEach { membership ->
            val user = membership.user
            sseEmitter.send(i18n("sync.members.check.membership", user.id, membership.id))
            if (user.username == null) {
                sseEmitter.send(i18n("sync.members.check.external.request"))
                usersWithOpenRegistrations[user]?.let { requestId ->
                    sseEmitter.send(i18n("sync.members.external.request.found", requestId))
                    if (acceptExternalMembershipRequest.execute(user, requestId)) {
                        sseEmitter.send(i18n("sync.members.external.request.accepted"))
                    } else {
                        sseEmitter.send(i18n("sync.members.external.request.manual"))
                    }
                    return@forEach
                }
                sseEmitter.send(i18n("sync.members.external.request.not.found"))
                if (user.externalId == null) {
                    alertLogger.alert(AlertCode.ACTIVE_MEMBERSHIP_NO_EXTERNAL_LINK) {
                        "User #${user.id} with active membership and no username could not be found externally!" }
                    return@forEach
                }
                externalMembers.remove(user.externalId)
                if (user.memberId != null) {
                    sseEmitter.send(i18n("sync.members.member.id.already.linked"))
                } else if (checkForNewMemberId.execute(user, true)) {
                    sseEmitter.send(i18n("sync.members.new.member.id"))
                } else {
                    sseEmitter.send(i18n("sync.members.no.new.member.id"))
                }
            } else {
                sseEmitter.send(i18n("sync.members.check.external.functions", user.username))
                if (user.externalId == null) {
                    alertLogger.alert(AlertCode.ACTIVE_MEMBERSHIP_NO_EXTERNAL_LINK) {
                        "User #${user.id} with active membership and username ${user.username} could not be found externally!" }
                    return@forEach
                }
                externalMembers.remove(user.externalId)
                val outOfSyncState = checkOutOfSyncExternalFunctions.execute(user, true)
                if (outOfSyncState.isOutOfSync()) {
                    sseEmitter.send(i18n("sync.members.external.functions.corrected",
                        outOfSyncState.functionsToAssign, outOfSyncState.functionsToDeassign))
                } else {
                    sseEmitter.send(i18n("sync.members.external.functions.okay"))
                }
            }
        }
        for ((externalId, externalMember) in externalMembers) {
            sseEmitter.send(i18n("sync.members.no.membership", externalId))
            removeAllExternalFunctions.execute(externalMember)
            sseEmitter.send(i18n("sync.members.external.functions.removed"))
        }
    }

    fun getUnsyncedMembers(): List<ExternalMember> {
        val unsyncedMembers = mutableListOf<ExternalMember>()
        val externalMembers = fetchExternalMembersById.execute()
        val usersWithOpenRegistrations = fetchUsersWithExternalOpenRegistrations.execute()
        membershipRepository.getCurrent().forEach { membership ->
            val user = membership.user
            if (user.username == null) {
                usersWithOpenRegistrations[user]?.let { requestId ->
                    unsyncedMembers += ExternalMember.fromUser(user, HasExternalOpenRegistration(requestId))
                    return@forEach
                }
                if (user.externalId == null) {
                    alertLogger.alert(AlertCode.ACTIVE_MEMBERSHIP_NO_EXTERNAL_LINK) {
                        "User #${user.id} with active membership and no username could not be found externally!" }
                    return@forEach
                }
                externalMembers.remove(user.externalId)
                if (user.memberId != null) {
                    unsyncedMembers += ExternalMember.fromUser(user, HasExternalMemberIdButNoAccount())
                } else if (checkForNewMemberId.execute(user, false)) {
                    unsyncedMembers += ExternalMember.fromUser(user, HasNewExternalMemberId())
                } else {
                    unsyncedMembers += ExternalMember.fromUser(user, HasNoExternalMemberIdYet())
                }
            } else {
                if (user.externalId == null) {
                    alertLogger.alert(AlertCode.ACTIVE_MEMBERSHIP_NO_EXTERNAL_LINK) {
                        "User #${user.id} with active membership and username ${user.username} could not be found externally!" }
                    return@forEach
                }
                externalMembers.remove(user.externalId)
                val outOfSyncState = checkOutOfSyncExternalFunctions.execute(user, false)
                if (outOfSyncState.isOutOfSync()) {
                    unsyncedMembers += ExternalMember.fromUser(user, HasUnmatchedExternalFunctions(
                        outOfSyncState.functionsToAssign, outOfSyncState.functionsToDeassign))
                }
            }
        }
        for ((_, externalMember) in externalMembers) {
            unsyncedMembers += externalMember.apply { syncState = HasNoActiveMembership() }
        }
        return unsyncedMembers
    }

    fun syncMemberWithExternalOpenRegistration(userId: Int, requestId: String): Boolean {
        val user = userRepository.findById(userId).orElseThrow { UserNotFoundException(userId.toString()) }
        return acceptExternalMembershipRequest.execute(user, requestId)
    }

    fun synMemberWithNewExternalMemberId(userId: Int): Boolean {
        val user = userRepository.findById(userId).orElseThrow { UserNotFoundException(userId.toString()) }
        return checkForNewMemberId.execute(user, true)
    }

    fun syncMemberWithUnmatchedExternalFunctions(userId: Int) {
        val user = userRepository.findById(userId).orElseThrow { UserNotFoundException(userId.toString()) }
        checkOutOfSyncExternalFunctions.execute(user, true)
    }

    fun syncMemberWithNoActiveMembership(externalMember: ExternalMember) {
        removeAllExternalFunctions.execute(externalMember)
    }
}