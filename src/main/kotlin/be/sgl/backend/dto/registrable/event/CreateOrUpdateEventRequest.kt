package be.sgl.backend.dto.registrable.event

import be.sgl.backend.dto.AddressDTO
import jakarta.validation.constraints.*
import java.time.LocalDateTime

data class CreateOrUpdateEventRequest(
    @field:NotBlank(message = "{NotBlank.event.name}")
    @field:Size(max = 50, message = "{Size.event.name}")
    val name: String?,
    @field:NotNull(message = "{NotNull.event.start}")
    var start: LocalDateTime?,
    @field:NotNull(message = "{NotNull.event.end}")
    var end: LocalDateTime?,
    @field:NotNull(message = "{NotNull.event.closed}")
    var closed: LocalDateTime?,
    var cancellable: Boolean,
    @field:NotBlank(message = "{NotBlank.event.description}")
    var description: String?,
    @field:NotNull(message = "{NotNull.event.open}")
    var open: LocalDateTime,
    @field:NotNull(message = "{NotNull.event.price}")
    @field:PositiveOrZero(message = "{PositiveOrZero.event.price}")
    var price: Double,
    @field:Positive(message = "{Positive.event.registrationLimit}")
    var registrationLimit: Int?,
    @field:NotNull(message = "{NotNull.event.address}")
    var address: AddressDTO?,
    var additionalForm: String?,
    @field:Size(max = 255, message = "{Size.event.additionalFormRule}")
    var additionalFormRule: String?,
    var sendConfirmation: Boolean,
    var sendCompleteConfirmation: Boolean,
    @field:Email(message = "{Email.event.communicationCC}")
    var communicationCC: String?,
    var needsMobile: Boolean
)