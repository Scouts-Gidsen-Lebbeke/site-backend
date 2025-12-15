package be.sgl.backend.mapper.registrable.event

import be.sgl.backend.dto.registrable.event.EventRegistrationDTO
import be.sgl.backend.entity.registrable.event.EventRegistration
import org.mapstruct.Mapper

@Mapper(componentModel = "spring")
fun interface EventRegistrationMapper {
    fun toDto(registration: EventRegistration): EventRegistrationDTO
}