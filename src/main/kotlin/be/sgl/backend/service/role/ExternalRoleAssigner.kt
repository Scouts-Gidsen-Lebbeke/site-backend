package be.sgl.backend.service.role

import be.sgl.backend.access.AccessApi
import be.sgl.backend.organization.domain.Role
import be.sgl.backend.people.domain.User
import be.sgl.backend.service.user.sync.CreateExternalFunctions
import be.sgl.backend.service.user.sync.EndExternalFunctions
import be.sgl.backend.shared.util.ForExternalOrganization
import mu.KotlinLogging
import org.springframework.stereotype.Component

@Component
@ForExternalOrganization
class ExternalRoleAssigner(
    accessApi: AccessApi,
    private val createExternalFunctions: CreateExternalFunctions,
    private val endExternalFunctions: EndExternalFunctions
) : RoleAssigner(accessApi) {

    private val logger = KotlinLogging.logger {}

    override fun startRole(user: User, role: Role): Boolean {
        if (!super.startRole(user, role)) return false
        if (role.memberRole) {
            logger.info { "Starting a role synchronized automatically, skipping external synchronization." }
            return true
        }
        val externalId = user.externalId ?: run {
            logger.warn { "User ${user.username} is not linked to Groepsadmin, skipping external synchronization of role ${role.name}." }
            return true
        }
        createExternalFunctions.execute(externalId, role.externalId, role.backupExternalId)
        return true
    }

    override fun endRole(user: User, role: Role) {
        super.endRole(user, role)
        val externalId = user.externalId ?: run {
            logger.warn { "User ${user.username} is not linked to Groepsadmin, skipping external synchronization of role ${role.name}." }
            return
        }
        endExternalFunctions.execute(externalId, role.externalId, role.backupExternalId)
    }
}
