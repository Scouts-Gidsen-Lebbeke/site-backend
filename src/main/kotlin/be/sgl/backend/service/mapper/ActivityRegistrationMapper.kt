package be.sgl.backend.service.mapper

import be.sgl.backend.dto.ActivityRegistrationDTO
import be.sgl.backend.entity.registrable.activity.ActivityRegistration
import org.mapstruct.Mapper

@Mapper(componentModel = "spring")
interface ActivityRegistrationMapper {
    fun toDto(registration: ActivityRegistration): ActivityRegistrationDTO
    fun toEntity(dto: ActivityRegistrationDTO): ActivityRegistration
}