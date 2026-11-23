package be.sgl.backend.dto.registrable.event

import be.sgl.backend.dto.AddressDTO
import be.sgl.backend.entity.registrable.event.Event
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

// read-only
@Schema(name = "Event", description = "The complete event configuration.")
data class EventDTO(
    val id: Int,
    val name: String,
    val start: LocalDateTime,
    val end: LocalDateTime,
    val closed: LocalDateTime,
    val cancellable: Boolean,
    val description: String,
    val open: LocalDateTime,
    val price: Double,
    val registrationLimit: Int?,
    val address: AddressDTO?,
    val additionalForm: String?,
    val additionalFormRule: String?,
    val sendConfirmation: Boolean,
    val sendCompleteConfirmation: Boolean,
    val communicationCC: String?,
    val needsMobile: Boolean
) {
    companion object {
        fun of(event: Event): EventDTO {
            return EventDTO(event.id!!, event.name, event.start, event.end, event.closed, event.cancellable,
                event.description, event.open, event.price, event.registrationLimit, event.address?.let(AddressDTO::of),
                event.additionalForm, event.additionalFormRule, event.sendConfirmation, event.sendCompleteConfirmation,
                event.communicationCC, event.needsMobile)
        }
    }
}
