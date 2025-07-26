package be.sgl.backend.mapper

import be.sgl.backend.dto.RoleDTO
import be.sgl.backend.dto.UserRoleDTO
import be.sgl.backend.entity.user.Role
import be.sgl.backend.entity.user.UserRole
import org.mapstruct.Mapper

@Mapper(componentModel = "spring")
interface RoleMapper {
    fun toDto(role: Role): RoleDTO
    fun toEntity(dto: RoleDTO): Role
    fun toDto(userRole: UserRole): UserRoleDTO
}