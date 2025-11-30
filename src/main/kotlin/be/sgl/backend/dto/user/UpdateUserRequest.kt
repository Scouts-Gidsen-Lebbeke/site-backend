package be.sgl.backend.dto.user

import be.sgl.backend.entity.user.Sex
import be.sgl.backend.util.Nis
import be.sgl.backend.util.PhoneNumber
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Past
import java.time.LocalDate

data class UpdateUserRequest(
    @field:NotBlank(message = "{NotBlank.updateUserRequest.name}")
    var name: String?,
    @field:NotBlank(message = "{NotBlank.updateUserRequest.firstName}")
    var firstName: String?,
    @field:Email(message = "{Email.updateUserRequest.email}")
    @field:NotBlank(message = "{NotBlank.updateUserRequest.email}")
    var email: String?,
    @field:Past(message = "{Past.updateUserRequest.birthDate}")
    @field:NotNull(message = "{NotNull.updateUserRequest.birthDate}")
    var birthdate: LocalDate?,
    @field:PhoneNumber(message = "{PhoneNumber.updateUserRequest.mobile}")
    var mobile: String?,
    @field:Nis(message = "{Nis.updateUserRequest.nis}")
    var nis: String?,
    var accountNo: String?,
    @field:NotNull(message = "{NotNull.updateUserRequest.sex}")
    var sex: Sex?
)