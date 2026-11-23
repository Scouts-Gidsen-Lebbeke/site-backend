package be.sgl.backend.dto.registrable.event

import be.sgl.backend.entity.registrable.event.EventRegistration
import io.swagger.v3.oas.annotations.media.Schema

// read-only
@Schema(name = "EventRegistration")
data class EventRegistrationDTO(
    val id: Int?,
    val price: Double,
    val paid: Boolean,
    val completed: Boolean,
    val additionalData: String?,
    val name: String,
    val firstName: String,
    val email: String,
    val mobile: String?,
    val subscribable: EventBaseDTO
) {
    companion object {
        fun of(registration: EventRegistration): EventRegistrationDTO {
            return EventRegistrationDTO(registration.id, registration.price, registration.paid, registration.completed,
                registration.additionalData, registration.name, registration.firstName, registration.email,
                registration.mobile, EventBaseDTO.of(registration.subscribable))
        }
    }
}
