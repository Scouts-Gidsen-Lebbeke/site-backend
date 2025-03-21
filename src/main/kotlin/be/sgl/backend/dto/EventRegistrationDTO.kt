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
    @field:NotBlank(message = "{NotBlank.event.registration.name}")
    var name: String,
    @field:NotBlank(message = "{NotBlank.event.registration.firstName}")
    var firstName: String,
    @field:NotBlank(message = "{NotBlank.event.registration.email}")
    @field:Email(message = "{Email.event.registration.email}")
    var email: String,
    @field:NotBlank(message = "{NotBlank.event.registration.mobile}")
    @PhoneNumber(message = "{PhoneNumber.event.registration.mobile}")
    var mobile: String?,
    var additionalData: String?
)