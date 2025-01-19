package be.sgl.backend.dto

import be.sgl.backend.entity.organization.ContactMethodType

data class OrganizationDTO(
    val id: Int?,
    val name: String,
    val kbo: String?,
    val address: AddressDTO,
    val contactMethods: List<ContactMethodDTO>,
    val image: String?,
    val description: String?
)

data class ContactMethodDTO(
    val id: Int?,
    val value: String,
    val type: ContactMethodType
)