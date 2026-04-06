package be.sgl.backend.mapper.registrable.activity

import be.sgl.backend.dto.registrable.activity.ActivityRegistrationDTO
import be.sgl.backend.dto.registrable.activity.ActivityRegistrationStatus
import be.sgl.backend.dto.registrable.activity.ActivityRegistrationStatusDTO
import be.sgl.backend.entity.registrable.activity.ActivityRegistration
import org.mapstruct.Mapper

@Mapper(componentModel = "spring")
interface ActivityRegistrationMapper {
    fun toDto(registration: ActivityRegistration): ActivityRegistrationDTO
    fun toDto(status: ActivityRegistrationStatus): ActivityRegistrationStatusDTO
}