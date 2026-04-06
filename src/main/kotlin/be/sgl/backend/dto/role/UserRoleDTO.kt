package be.sgl.backend.dto.role

import be.sgl.backend.dto.user.UserDTO

// read-only
data class UserRoleDTO(
    var id: Int?,
    var user: UserDTO,
    var role: RoleDTO
)