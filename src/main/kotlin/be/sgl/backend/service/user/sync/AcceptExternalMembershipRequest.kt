package be.sgl.backend.service.user.sync

import be.sgl.backend.access.AccessApi
import be.sgl.backend.organization.RoleApi
import be.sgl.backend.organization.RoleSummary
import be.sgl.backend.people.domain.User
import be.sgl.backend.openapi.api.LedenApi
import be.sgl.backend.openapi.api.LidaanvragenApi
import be.sgl.backend.openapi.model.FunctieInstantie
import be.sgl.backend.people.domain.UserRepository
import be.sgl.backend.service.user.ExternalUserDataProvider.Companion.toDto
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import java.time.OffsetDateTime

@ExternalUsecase
class AcceptExternalMembershipRequest(
    @Value("\${organization.external.id}")
    private val externalOrganizationId: String,
    private val userRepository: UserRepository,
    private val lidaanvragenApi: LidaanvragenApi,
    private val ledenApi: LedenApi,
    private val createExternalFunctions: CreateExternalFunctions,
    private val roleApi: RoleApi,
    private val accessApi: AccessApi
) {

    private val logger = KotlinLogging.logger {}

    fun execute(user: User, requestId: String): Boolean {
        logger.info { "User has external membership request $requestId, trying to accept..." }
        val newRole = roleApi.findRoles(accessApi.roleIdsOf(user.id!!)).firstOrNull { it.forExternalSync }
        if (newRole == null) {
            // This should only happen for staff branch memberships, which shouldn't be common for new members
            logger.info { "Internal membership didn't assign role, manual intervention is needed." }
            return false
        }
        logger.info { "Internal membership assigned role ${newRole.name}, passing it for external creation..." }
        user.externalId = createExternalMemberForUser(user, newRole)
        userRepository.save(user)
        lidaanvragenApi.deleteAanvraag(requestId, "ja", false)
        logger.info { "External membership request accepted." }
        return true
    }

    private fun createExternalMemberForUser(user: User, newRole: RoleSummary): String {
        logger.info { "Translating internal user #${user.id} into an external user..." }
        var newLid = user.toDto(emptyList())
        newLid.functies = listOf(
            FunctieInstantie().apply {
                groep = externalOrganizationId
                functie = checkNotNull(newRole.externalId) { "Role ${newRole.name} has no external function to assign!" }
                begin = OffsetDateTime.now()
            }
        )
        newLid = ledenApi.postLid(false, newLid, null)
        logger.info { "Created external member with id ${newLid.id} and external function ${newRole.externalId}" }
        // Upon creation only a single function can be passed
        newRole.backupExternalId?.let {
            logger.info { "${newRole.name} has an additional external function $it to link..." }
            createExternalFunctions.execute(newLid.id, it)
        }
        return newLid.id
    }
}