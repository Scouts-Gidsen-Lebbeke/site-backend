package be.sgl.backend.dto

import be.sgl.backend.entity.organization.ContactMethodType
import be.sgl.backend.entity.organization.OrganizationType
import be.sgl.backend.util.Kbo
import jakarta.validation.constraints.NotBlank

data class OrganizationDTO(
    val id: Int?,
    @NotBlank
    val name: String,
    val type: OrganizationType,
    @Kbo
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