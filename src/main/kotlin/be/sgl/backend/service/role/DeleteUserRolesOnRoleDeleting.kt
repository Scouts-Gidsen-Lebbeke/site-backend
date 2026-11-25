package be.sgl.backend.service.role

import be.sgl.backend.organization.RoleDeleting
import be.sgl.backend.repository.role.UserRoleRepository
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class DeleteUserRolesOnRoleDeleting(private val userRoleRepository: UserRoleRepository) {

    @EventListener
    fun on(event: RoleDeleting) {
        userRoleRepository.deleteUserRolesByRoleId(event.roleId)
    }
}
