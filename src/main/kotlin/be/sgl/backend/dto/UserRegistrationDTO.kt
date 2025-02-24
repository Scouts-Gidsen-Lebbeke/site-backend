package be.sgl.backend.dto

import be.sgl.backend.entity.user.Sex
import be.sgl.backend.util.PhoneNumber
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.time.LocalDate

data class UserRegistrationDTO(
    @NotBlank(message = "{NotBlank.userRegistration.name}")
    var name: String,
    @NotBlank(message = "{NotBlank.userRegistration.firstName}")
    var firstName: String,
    var birthdate: LocalDate,
    @Email(message = "{Email.userRegistration.email}")
    var email: String,
    @NotNull(message = "{NotNull.userRegistration.mobile}")
    @PhoneNumber(message = "{PhoneNumber.userRegistration.mobile}")
    var mobile: String,
    var sex: Sex,
    var hasReduction: Boolean = false,
    var hasHandicap: Boolean = false,
    var address: AddressDTO
)