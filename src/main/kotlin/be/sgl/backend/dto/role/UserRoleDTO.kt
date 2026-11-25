package be.sgl.backend.dto.role

import be.sgl.backend.dto.user.UserDTO
import be.sgl.backend.entity.user.User
import be.sgl.backend.organization.domain.Role
import be.sgl.backend.organization.web.RoleDTO

// read-only
data class UserRoleDTO(
    var id: Int?,
    var user: UserDTO,
    var role: RoleDTO
) {
    companion object {
        fun of(user: User, role: Role): UserRoleDTO {
            return UserRoleDTO(null, UserDTO.of(user), RoleDTO.of(role))
        }
    }
}
