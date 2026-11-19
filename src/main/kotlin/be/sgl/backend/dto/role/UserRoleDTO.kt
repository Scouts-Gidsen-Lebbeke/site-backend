package be.sgl.backend.dto.role

import be.sgl.backend.dto.user.UserDTO
import be.sgl.backend.entity.role.UserRole

// read-only
data class UserRoleDTO(
    var id: Int?,
    var user: UserDTO,
    var role: RoleDTO
) {
    companion object {
        fun of(userRole: UserRole): UserRoleDTO {
            return UserRoleDTO(null, UserDTO.of(userRole.user), RoleDTO.of(userRole.role))
        }
    }
}
