package be.sgl.backend.access.application

import be.sgl.backend.access.domain.UserRoleRepository
import be.sgl.backend.organization.RoleDeleting
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class DeleteAssignmentsOnRoleDeleting(private val userRoleRepository: UserRoleRepository) {

    @EventListener
    fun on(event: RoleDeleting) {
        userRoleRepository.deleteByRoleId(event.roleId)
    }
}
