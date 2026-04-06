package be.sgl.backend.mapper.user

import be.sgl.backend.dto.user.UserDTO
import be.sgl.backend.entity.user.User
import org.mapstruct.Mapper

@Mapper(componentModel = "spring")
interface UserMapper {
    fun toDto(user: User): UserDTO
}