package be.sgl.backend.service.mapper

import be.sgl.backend.dto.UserDTO
import be.sgl.backend.entity.User
import org.mapstruct.Mapper

@Mapper(componentModel = "spring")
interface UserMapper {
    fun toDto(user: User): UserDTO
    fun toEntity(dto: UserDTO): User
}