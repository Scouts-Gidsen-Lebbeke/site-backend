package be.sgl.backend.organization.web

import be.sgl.backend.organization.domain.ContactMethod
import be.sgl.backend.organization.domain.ContactMethodType
import be.sgl.backend.organization.domain.Organization
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull

@Schema(name = "ContactMethod")
data class ContactMethodDTO(
    var id: Int?,
    @field:NotNull
    var value: String?,
    @field:NotNull
    var type: ContactMethodType?
) {

    fun toEntity(organization: Organization) = ContactMethod(organization, type!!, value!!)

    companion object {
        fun of(contactMethod: ContactMethod): ContactMethodDTO {
            return ContactMethodDTO(contactMethod.id, contactMethod.value, contactMethod.type)
        }
    }
}
