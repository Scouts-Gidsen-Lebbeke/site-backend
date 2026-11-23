package be.sgl.backend.dto.registrable.event

import be.sgl.backend.entity.registrable.event.Event
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

// read-only
@Schema(name = "EventBase", description = "Basic information about an event.")
data class EventBaseDTO(
    val id: Int,
    val name: String,
    val start: LocalDateTime,
    val end: LocalDateTime,
    val closed: LocalDateTime,
    val cancellable: Boolean
) {
    companion object {
        fun of(event: Event): EventBaseDTO {
            return EventBaseDTO(event.id!!, event.name, event.start, event.end, event.closed, event.cancellable)
        }
    }
}
