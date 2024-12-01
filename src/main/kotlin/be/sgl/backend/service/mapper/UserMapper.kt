package be.sgl.backend.service.mapper

import be.sgl.backend.dto.ExtendedUserDTO
import be.sgl.backend.dto.StaffDTO
import be.sgl.backend.dto.UserDTO
import be.sgl.backend.entity.user.User
import org.mapstruct.Mapper

@Mapper(componentModel = "spring")
interface UserMapper {
    fun toDto(user: User): UserDTO
    fun toEntity(dto: UserDTO): User
    fun toStaffDto(user: User): StaffDTO
    fun toEntity(dto: StaffDTO): User
    fun toExtendedDto(user: User): ExtendedUserDTO
    fun toEntity(dto: ExtendedUserDTO): User
}