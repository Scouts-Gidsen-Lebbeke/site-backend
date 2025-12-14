package be.sgl.backend.mapper

import be.sgl.backend.dto.registrable.event.EventBaseDTO
import be.sgl.backend.dto.registrable.event.EventDTO
import be.sgl.backend.dto.registrable.event.EventRegistrationDTO
import be.sgl.backend.entity.registrable.event.Event
import be.sgl.backend.entity.registrable.event.EventRegistration
import org.mapstruct.Mapper

@Mapper(componentModel = "spring")
interface EventMapper {
    fun toDto(activity: Event): EventDTO
    fun toBaseDto(activity: Event): EventBaseDTO
    fun toEntity(dto: EventDTO): Event
    fun toDto(registration: EventRegistration): EventRegistrationDTO
    fun toEntity(dto: EventRegistrationDTO): EventRegistration
}