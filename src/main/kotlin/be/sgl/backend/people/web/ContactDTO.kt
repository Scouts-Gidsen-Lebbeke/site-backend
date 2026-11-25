package be.sgl.backend.people.web

import be.sgl.backend.shared.address.AddressDTO
import be.sgl.backend.people.domain.Contact
import be.sgl.backend.people.domain.ContactRole
import be.sgl.backend.shared.util.Nis
import be.sgl.backend.shared.util.PhoneNumber
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

@Schema(name = "Contact")
data class ContactDTO(
    val id: Int?,
    @field:NotBlank
    val name: String?,
    @field:NotBlank
    val firstName: String?,
    @field:NotNull
    val role: ContactRole?,
    @field:PhoneNumber
    val mobile: String?,
    @field:Email
    val email: String?,
    @field:Nis
    val nis: String?,
    val address: AddressDTO?
) {
    companion object {
        fun of(contact: Contact): ContactDTO {
            return ContactDTO(contact.id, contact.name, contact.firstName, contact.role, contact.mobile, contact.email,
                contact.nis, contact.address?.let(AddressDTO::of))
        }
    }
}
