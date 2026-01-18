package be.sgl.backend.mapper.user

import be.sgl.backend.dto.user.ContactDTO
import be.sgl.backend.dto.user.CreateOrUpdateContactRequest
import be.sgl.backend.entity.user.Contact
import org.mapstruct.Mapper

@Mapper(componentModel = "spring")
interface ContactMapper {
    fun toDto(contact: Contact): ContactDTO
    fun toEntity(request: CreateOrUpdateContactRequest): Contact
}