package be.sgl.backend.dto.registrable.event

import be.sgl.backend.entity.registrable.RegistrableStatus
import be.sgl.backend.entity.registrable.RegistrableStatus.Companion.getStatus
import be.sgl.backend.entity.registrable.event.Event
import java.time.LocalDateTime

// read-only
data class EventResult(
    val id: Int,
    val name: String,
    val start: LocalDateTime,
    val end: LocalDateTime,
    val closed: LocalDateTime,
    val cancellable: Boolean,
    val registrationCount: Int,
    val totalPrice: Double,
    val status: RegistrableStatus
) {
    companion object {
        fun of(event: Event, registrations: List<Double>): EventResult {
            return EventResult(event.id!!, event.name, event.start, event.end, event.closed, event.cancellable,
                registrations.count(), registrations.sum(), event.getStatus())
        }
    }
}