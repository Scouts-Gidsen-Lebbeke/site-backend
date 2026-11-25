package be.sgl.backend.dto.organization

import be.sgl.backend.shared.address.AddressDTO
import be.sgl.backend.entity.organization.Organization
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
) {

    fun toEntity() = Organization().also {
        it.name = name
        it.type = type
        it.kbo = kbo
        it.address = address.toEntity()
        it.image = image
        it.description = description
        it.contactMethods.addAll(contactMethods.map { method -> method.toEntity(it) })
    }

    companion object {
        fun of(organization: Organization): OrganizationDTO {
            return OrganizationDTO(organization.id!!, organization.name, organization.type, organization.kbo,
                AddressDTO.of(organization.address), organization.contactMethods.map(ContactMethodDTO::of),
                organization.image, organization.description)
        }
    }
}
