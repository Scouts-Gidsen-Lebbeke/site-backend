package be.sgl.backend.mapper

import be.sgl.backend.dto.EventBaseDTO
import be.sgl.backend.dto.EventDTO
import be.sgl.backend.dto.EventRegistrationDTO
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