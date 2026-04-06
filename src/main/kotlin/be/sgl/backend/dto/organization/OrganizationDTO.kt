package be.sgl.backend.dto.organization

import be.sgl.backend.dto.AddressDTO
import be.sgl.backend.entity.organization.OrganizationType
import io.swagger.v3.oas.annotations.media.Schema

// read-only
@Schema(name = "Organization")
data class OrganizationDTO(
    val id: Int,
    val name: String,
    val type: OrganizationType,
    val kbo: String?,
    val address: AddressDTO,
    val contactMethods: List<ContactMethodDTO>,
    val image: String?,
    val description: String?
)