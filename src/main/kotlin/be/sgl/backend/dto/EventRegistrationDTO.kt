package be.sgl.backend.dto

import be.sgl.backend.util.PhoneNumber
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

data class EventRegistrationDTO(
    val id: Int?,
    val price: Double,
    val completed: Boolean,
    val additionalData: String?,
    val name: String,
    val firstName: String,
    val email: String,
    val mobile: String?
)

data class EventRegistrationAttemptData(
    @NotBlank
    val name: String,
    @NotBlank
    val firstName: String,
    @NotBlank
    @Email
    val email: String,
    @PhoneNumber
    val mobile: String?,
    val additionalData: String?
)