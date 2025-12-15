package be.sgl.backend.service.user.sync

import be.sgl.backend.entity.role.Role
import be.sgl.backend.entity.user.User
import be.sgl.backend.openapi.api.LedenApi
import be.sgl.backend.openapi.api.LidaanvragenApi
import be.sgl.backend.openapi.model.FunctieInstantie
import be.sgl.backend.repository.user.UserRepository
import be.sgl.backend.service.user.ExternalUserDataProvider.Companion.toDto
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import java.time.OffsetDateTime

@ExternalUsecase
class AcceptExternalMembershipRequest {

    private val logger = KotlinLogging.logger {}

    @Value("\${organization.external.id}")
    private lateinit var externalOrganizationId: String
    @Autowired
    private lateinit var userRepository: UserRepository
    @Autowired
    private lateinit var lidaanvragenApi: LidaanvragenApi
    @Autowired
    private lateinit var ledenApi: LedenApi
    @Autowired
    private lateinit var createExternalFunctions: CreateExternalFunctions

    fun execute(user: User, requestId: String): Boolean {
        logger.info { "User has external membership request $requestId, trying to accept..." }
        val newRole = user.roles.firstOrNull { it.role.forExternalSync }?.role
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

    private fun createExternalMemberForUser(user: User, newRole: Role): String {
        logger.info { "Translating internal user #${user.id} into an external user..." }
        var newLid = user.toDto()
        newLid.functies = listOf(
            FunctieInstantie().apply {
                groep = externalOrganizationId
                functie = newRole.externalId
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