package be.sgl.backend.dto

import be.sgl.backend.util.PhoneNumber
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

// read-only, no validation
data class EventRegistrationDTO(
    val id: Int?,
    val price: Double,
    val paid: Boolean,
    val completed: Boolean,
    val additionalData: String?,
    val name: String,
    val firstName: String,
    val email: String,
    val mobile: String?
)

data class EventRegistrationAttemptData(
    @NotBlank(message = "{NotBlank.event.registration.name}")
    val name: String,
    @NotBlank(message = "{NotBlank.event.registration.firstName}")
    val firstName: String,
    @NotBlank(message = "{NotBlank.event.registration.email}")
    @Email(message = "{Email.event.registration.email}")
    val email: String,
    @NotBlank(message = "{NotBlank.event.registration.mobile}")
    @PhoneNumber(message = "{PhoneNumber.event.registration.mobile}")
    val mobile: String?,
    val additionalData: String?
)