package be.sgl.backend.dto.user

import be.sgl.backend.entity.user.Contact
import be.sgl.backend.entity.user.ContactRole
import be.sgl.backend.shared.util.Nis
import be.sgl.backend.shared.util.PhoneNumber
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

data class CreateOrUpdateContactRequest(
    @field:NotNull(message = "{NotNull.contact.user}")
    var userId: Int?,
    @field:NotBlank(message = "{NotBlank.contact.firstName}")
    var firstName: String?,
    @field:NotBlank(message = "{NotBlank.contact.name}")
    var name: String?,
    @field:NotNull(message = "{NotNull.contact.role}")
    var role: ContactRole?,
    @field:PhoneNumber(message = "{PhoneNumber.contact.mobile}")
    var mobile: String?,
    @field:Email(message = "{Email.contact.email}")
    var email: String?,
    @field:Nis(message = "{Nis.contact.nis}")
    var nis: String?,
    var addressId: Int?
) {

    /**
     * The user and address are not set, as they need to be resolved by id.
     */
    fun toEntity() = Contact().also {
        it.name = name!!
        it.firstName = firstName!!
        it.role = role!!
        it.mobile = mobile
        it.email = email
        it.nis = nis
    }
}
