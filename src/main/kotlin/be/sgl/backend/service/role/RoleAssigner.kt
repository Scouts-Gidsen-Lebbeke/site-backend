package be.sgl.backend.service.role

import be.sgl.backend.access.AccessApi
import be.sgl.backend.organization.domain.Role
import be.sgl.backend.people.domain.User
import be.sgl.backend.shared.util.ForInternalOrganization
import mu.KotlinLogging
import org.springframework.stereotype.Component

@Component
@ForInternalOrganization
open class RoleAssigner(private val accessApi: AccessApi) {

    private val logger = KotlinLogging.logger {}

    open fun startRole(user: User, role: Role): Boolean {
        logger.debug { "Starting role ${role.name} for ${user.username}..." }
        if (!accessApi.assign(user.id!!, role.id!!)) {
            logger.warn { "${user.username} already has the role ${role.name}! Starting aborted." }
            return false
        }
        return true
    }

    open fun endRole(user: User, role: Role) {
        logger.debug { "Ending role ${role.name} for ${user.username}..." }
        accessApi.revoke(user.id!!, role.id!!)
    }
}
