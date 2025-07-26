package be.sgl.backend.service.user.sync

import be.sgl.backend.alert.AlertCode
import be.sgl.backend.alert.AlertLogger
import be.sgl.backend.repository.membership.MembershipRepository
import be.sgl.backend.util.ForExternalOrganization
import be.sgl.backend.util.I18nUtil.Companion.i18n
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import be.sgl.backend.service.user.sync.SyncState.*

@Component
@ForExternalOrganization
class SyncService {

    private val logger = KotlinLogging.logger {}

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
    private lateinit var alertLogger: AlertLogger

    fun syncUsers(sseEmitter: SseEmitter) {
        logger.info { "Syncing users..." }
        fetchExternalMembersById.execute().forEach { (externalId, _) ->
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
                if (user.externalId == null || externalMembers.remove(user.externalId) == null) {
                    alertLogger.alert(AlertCode.ACTIVE_MEMBERSHIP_NO_EXTERNAL_LINK) {
                        "User #${user.id} with active membership and no username could not be found externally!" }
                    return@forEach
                }
                if (user.memberId != null) {
                    sseEmitter.send(i18n("sync.members.member.id.already.linked"))
                } else if (checkForNewMemberId.execute(user, true)) {
                    sseEmitter.send(i18n("sync.members.new.member.id"))
                } else {
                    sseEmitter.send(i18n("sync.members.no.new.member.id"))
                }
            } else {
                sseEmitter.send(i18n("sync.members.check.external.functions", user.username))
                if (user.externalId == null || externalMembers.remove(user.externalId) == null) {
                    alertLogger.alert(AlertCode.ACTIVE_MEMBERSHIP_NO_EXTERNAL_LINK) {
                        "User #${user.id} with active membership and username ${user.username} could not be found externally!" }
                    return@forEach
                }
                val (missingFunctions, wrongFunctions) = checkOutOfSyncExternalFunctions.execute(user, true)
                if (missingFunctions == 0 && wrongFunctions == 0) {
                    sseEmitter.send(i18n("sync.members.external.functions.okay"))
                } else {
                    sseEmitter.send(i18n("sync.members.external.functions.corrected", missingFunctions, wrongFunctions))
                }
            }
        }
        for ((externalId, externalMember) in externalMembers) {
            sseEmitter.send(i18n("sync.members.no.membership", externalId))
            removeAllExternalFunctions.execute(externalId, externalMember)
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
                usersWithOpenRegistrations[user]?.let { _ ->
                    unsyncedMembers += ExternalMember.fromUser(user, HAS_EXTERNAL_OPEN_REGISTRATION)
                    return@forEach
                }
                if (user.externalId == null || externalMembers.remove(user.externalId) == null) {
                    alertLogger.alert(AlertCode.ACTIVE_MEMBERSHIP_NO_EXTERNAL_LINK) {
                        "User #${user.id} with active membership and no username could not be found externally!" }
                    return@forEach
                }
                if (user.memberId != null) {
                    unsyncedMembers += ExternalMember.fromUser(user, HAS_EXTERNAL_MEMBER_ID_BUT_NO_ACCOUNT)
                } else if (checkForNewMemberId.execute(user, false)) {
                    unsyncedMembers += ExternalMember.fromUser(user, HAS_NEW_EXTERNAL_MEMBER_ID)
                }
            } else {
                if (user.externalId == null || externalMembers.remove(user.externalId) == null) {
                    alertLogger.alert(AlertCode.ACTIVE_MEMBERSHIP_NO_EXTERNAL_LINK) {
                        "User #${user.id} with active membership and username ${user.username} could not be found externally!" }
                    return@forEach
                }
                val (missingFunctions, wrongFunctions) = checkOutOfSyncExternalFunctions.execute(user, false)
                if (missingFunctions != 0 || wrongFunctions != 0) {
                    unsyncedMembers += ExternalMember.fromUser(user, HAS_UNMATCHED_EXTERNAL_FUNCTIONS)
                }
            }
        }
        for ((_, externalMember) in externalMembers) {
            unsyncedMembers += externalMember.apply { syncState = HAS_NO_ACTIVE_MEMBERSHIP }
        }
        return unsyncedMembers
    }
}