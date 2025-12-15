package be.sgl.backend.dto.registrable.event

import be.sgl.backend.util.PhoneNumber
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

data class CreateEventRegistrationRequest(
    @field:NotBlank(message = "{NotBlank.event.registration.name}")
    var name: String?,
    @field:NotBlank(message = "{NotBlank.event.registration.firstName}")
    var firstName: String?,
    @field:NotBlank(message = "{NotBlank.event.registration.email}")
    @field:Email(message = "{Email.event.registration.email}")
    var email: String?,
    @field:PhoneNumber(message = "{PhoneNumber.event.registration.mobile}")
    var mobile: String?,
    var additionalData: String?
)