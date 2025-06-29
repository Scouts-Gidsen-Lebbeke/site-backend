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
    private lateinit var removeExternalFunctions: RemoveExternalFunctions
    @Autowired
    private lateinit var checkMissingExternalFunctions: CheckMissingExternalFunctions
    @Autowired
    private lateinit var alertLogger: AlertLogger

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
                val missingFunctions = checkMissingExternalFunctions.execute(user, true)
                if (missingFunctions.isNotEmpty()) {
                    sseEmitter.send(i18n("sync.members.external.functions.assigned", missingFunctions.size))
                } else {
                    sseEmitter.send(i18n("sync.members.external.functions.okay"))
                }
            }
        }
        for ((externalId, externalMember) in externalMembers) {
            sseEmitter.send(i18n("sync.members.no.membership", externalId))
            removeExternalFunctions.execute(externalId, externalMember)
            sseEmitter.send(i18n("sync.members.external.functions.removed"))
        }
    }

    fun getUnsyncedMembers(): Map<ExternalMember, SyncState> {
        val usersWithSyncState = mutableMapOf<ExternalMember, SyncState>()
        val externalMembers = fetchExternalMembersById.execute()
        val usersWithOpenRegistrations = fetchUsersWithExternalOpenRegistrations.execute()
        membershipRepository.getCurrent().forEach { membership ->
            val user = membership.user
            if (user.username == null) {
                usersWithOpenRegistrations[user]?.let { _ ->
                    usersWithSyncState[ExternalMember.fromUser(user)] = HAS_EXTERNAL_OPEN_REGISTRATION
                    return@forEach
                }
                if (user.externalId == null || externalMembers.remove(user.externalId) == null) {
                    alertLogger.alert(AlertCode.ACTIVE_MEMBERSHIP_NO_EXTERNAL_LINK) {
                        "User #${user.id} with active membership and no username could not be found externally!" }
                    return@forEach
                }
                if (user.memberId != null) {
                    usersWithSyncState[ExternalMember.fromUser(user)] = HAS_EXTERNAL_MEMBER_ID_BUT_NO_ACCOUNT
                } else if (checkForNewMemberId.execute(user, false)) {
                    usersWithSyncState[ExternalMember.fromUser(user)] = HAS_NEW_EXTERNAL_MEMBER_ID
                }
            } else {
                if (user.externalId == null || externalMembers.remove(user.externalId) == null) {
                    alertLogger.alert(AlertCode.ACTIVE_MEMBERSHIP_NO_EXTERNAL_LINK) {
                        "User #${user.id} with active membership and username ${user.username} could not be found externally!" }
                    return@forEach
                }
                val missingFunctions = checkMissingExternalFunctions.execute(user, false)
                if (missingFunctions.isNotEmpty()) {
                    usersWithSyncState[ExternalMember.fromUser(user)] = HAS_UNMATCHED_EXTERNAL_FUNCTIONS
                }
            }
        }
        for ((_, externalMember) in externalMembers) {
            usersWithSyncState[externalMember] = HAS_NO_ACTIVE_MEMBERSHIP
        }
        return usersWithSyncState
    }

    fun syncUser(externalMember: ExternalMember) {
        val unsyncedMembers = getUnsyncedMembers()
        when (unsyncedMembers[externalMember]) {
            HAS_EXTERNAL_OPEN_REGISTRATION -> {
                // accept registration
                // acceptExternalMembershipRequest.execute(user, requestId)
            }
            HAS_NEW_EXTERNAL_MEMBER_ID -> {
                // link new member id
                // checkForNewMemberId.execute(user, true)
            }
            HAS_UNMATCHED_EXTERNAL_FUNCTIONS -> {
                // assign external functions
                // checkMissingExternalFunctions.execute(user, true)
            }
            HAS_NO_ACTIVE_MEMBERSHIP -> {
                // unassign external functions
                // removeExternalFunctions.execute(externalId, externalMember)
            }
            HAS_EXTERNAL_MEMBER_ID_BUT_NO_ACCOUNT, null -> {
                // nothing to do
            }
        }
    }
}