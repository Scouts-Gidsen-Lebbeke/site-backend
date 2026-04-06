package be.sgl.backend.mapper.role

import be.sgl.backend.dto.role.RoleDTO
import be.sgl.backend.dto.role.UserRoleDTO
import be.sgl.backend.entity.role.Role
import be.sgl.backend.entity.role.UserRole
import org.mapstruct.Mapper

@Mapper(componentModel = "spring")
interface RoleMapper {
    fun toDto(role: Role): RoleDTO
    fun toEntity(dto: RoleDTO): Role
    fun toDto(userRole: UserRole): UserRoleDTO
}