package be.sgl.backend.dto

import be.sgl.backend.entity.user.ContactRole
import be.sgl.backend.util.Nis
import be.sgl.backend.util.PhoneNumber
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

data class ContactDTO(
    val id: Int?,
    @NotBlank
    val name: String,
    @NotBlank
    val firstName: String,
    val role: ContactRole,
    @PhoneNumber
    val mobile: String?,
    @Email
    val email: String?,
    @Nis
    val nis: String?,
    val address: AddressDTO?
)