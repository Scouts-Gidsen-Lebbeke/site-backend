package be.sgl.backend.dto

import be.sgl.backend.entity.user.Sex
import be.sgl.backend.util.PhoneNumber
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.time.LocalDate

data class UserRegistrationDTO(
    @field:NotBlank(message = "{NotBlank.userRegistration.name}")
    var name: String?,
    @field:NotBlank(message = "{NotBlank.userRegistration.firstName}")
    var firstName: String?,
    var birthdate: LocalDate?,
    @field:NotNull(message = "{NotNull.userRegistration.email}")
    @field:Email(message = "{Email.userRegistration.email}")
    var email: String?,
    @field:NotNull(message = "{NotNull.userRegistration.mobile}")
    @field:PhoneNumber(message = "{PhoneNumber.userRegistration.mobile}")
    var mobile: String?,
    @field:NotNull(message = "{NotNull.userRegistration.sex}")
    var sex: Sex?,
    var hasReduction: Boolean = false,
    var hasHandicap: Boolean = false,
    @field:NotNull(message = "{NotNull.userRegistration.address}")
    var address: AddressDTO?
)