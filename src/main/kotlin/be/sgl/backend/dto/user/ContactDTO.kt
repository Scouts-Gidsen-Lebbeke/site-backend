package be.sgl.backend.dto.user

import be.sgl.backend.dto.AddressDTO
import be.sgl.backend.entity.user.ContactRole
import be.sgl.backend.util.Nis
import be.sgl.backend.util.PhoneNumber
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
)