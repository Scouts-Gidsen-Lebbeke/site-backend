package be.sgl.backend.dto.registrable.event

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
)