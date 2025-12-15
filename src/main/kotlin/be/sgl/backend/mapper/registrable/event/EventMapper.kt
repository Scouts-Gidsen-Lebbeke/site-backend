package be.sgl.backend.mapper.registrable.event

import be.sgl.backend.dto.AddressDTO
import be.sgl.backend.dto.registrable.event.CreateOrUpdateEventRequest
import be.sgl.backend.dto.registrable.event.EventBaseDTO
import be.sgl.backend.dto.registrable.event.EventDTO
import be.sgl.backend.entity.Address
import be.sgl.backend.entity.registrable.event.Event
import org.mapstruct.Mapper

@Mapper(componentModel = "spring")
interface EventMapper {
    fun toDto(activity: Event): EventDTO
    fun toBaseDto(activity: Event): EventBaseDTO
    fun toEntity(request: CreateOrUpdateEventRequest): Event
    fun toEntity(dto: AddressDTO): Address
}