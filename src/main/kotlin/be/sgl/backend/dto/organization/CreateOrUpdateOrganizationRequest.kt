package be.sgl.backend.dto.organization

import be.sgl.backend.dto.AddressDTO
import be.sgl.backend.entity.organization.OrganizationType
import be.sgl.backend.util.Kbo
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

data class CreateOrUpdateOrganizationRequest(
    @field:NotBlank
    var name: String?,
    @field:NotNull
    var type: OrganizationType?,
    @field:Kbo
    var kbo: String?,
    @field:NotNull
    var address: AddressDTO?,
    var contactMethods: List<ContactMethodDTO>,
    var image: String?,
    var description: String?
)