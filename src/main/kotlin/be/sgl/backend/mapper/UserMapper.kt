package be.sgl.backend.mapper

import be.sgl.backend.dto.BranchDTO
import be.sgl.backend.dto.UserDTO
import be.sgl.backend.entity.branch.Branch
import be.sgl.backend.entity.user.User
import org.mapstruct.Mapper

@Mapper(componentModel = "spring")
interface UserMapper {
    fun toDto(user: User): UserDTO
    fun toEntity(dto: UserDTO): User
    fun toDto(branch: Branch): BranchDTO
}