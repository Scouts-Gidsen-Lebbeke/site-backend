package be.sgl.backend.dto

import be.sgl.backend.entity.user.ContactRole
import be.sgl.backend.util.Nis
import be.sgl.backend.util.PhoneNumber
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

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
)