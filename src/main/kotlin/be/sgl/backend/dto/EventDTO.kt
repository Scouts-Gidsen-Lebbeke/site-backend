package be.sgl.backend.dto

import be.sgl.backend.entity.registrable.RegistrableStatus
import be.sgl.backend.entity.registrable.RegistrableStatus.Companion.getStatus
import be.sgl.backend.entity.registrable.event.Event
import be.sgl.backend.entity.registrable.event.EventRegistration
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.*
import java.io.Serializable
import java.time.LocalDateTime

// DTO for a list overview of events
@Schema(description = "Basic information about an event.")
open class EventBaseDTO(
    val id: Int?,
    @NotBlank(message = "{NotBlank.event.name}")
    @Size(max = 50, message = "{Size.event.name}")
    val name: String,
    @NotNull(message = "{NotNull.event.start}")
    var start: LocalDateTime,
    @NotNull(message = "{NotNull.event.end}")
    var end: LocalDateTime
) : Serializable

// DTO for registration page and CRUD
@Schema(description = "The complete event configuration.")
class EventDTO(
    id: Int?,
    name: String,
    start: LocalDateTime,
    end: LocalDateTime,
    @NotBlank(message = "{NotBlank.event.description}")
    var description: String,
    @NotNull(message = "{NotNull.event.open}")
    var open: LocalDateTime,
    @NotNull(message = "{NotNull.event.closed}")
    var closed: LocalDateTime,
    @NotNull(message = "{NotNull.event.price}")
    @PositiveOrZero(message = "{PositiveOrZero.event.price}")
    var price: Double,
    @Positive(message = "{Positive.event.registrationLimit}")
    var registrationLimit: Int?,
    @NotNull(message = "{NotNull.event.address}")
    var address: AddressDTO,
    var additionalForm: String?,
    @Size(max = 255, message = "{Size.event.additionalFormRule}")
    var additionalFormRule: String?,
    var cancellable: Boolean,
    var sendConfirmation: Boolean,
    var sendCompleteConfirmation: Boolean,
    @Email(message = "{Email.event.communicationCC}")
    var communicationCC: String?,
    var needsMobile: Boolean
) : EventBaseDTO(id, name, start, end)

// DTO for statistics list overview
class EventResultDTO(
    id: Int?,
    name: String,
    start: LocalDateTime,
    end: LocalDateTime,
    var registrationCount: Int,
    var totalPrice: Double,
    var status: RegistrableStatus
) : EventBaseDTO(id, name, start, end) {
    constructor(event: Event, registrations: List<EventRegistration>) :
            this(event.id, event.name, event.start, event.end, registrations.count(), registrations.sumOf { it.price }, event.getStatus())
}