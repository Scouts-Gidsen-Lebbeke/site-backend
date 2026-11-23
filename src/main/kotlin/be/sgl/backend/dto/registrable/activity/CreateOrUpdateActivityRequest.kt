package be.sgl.backend.dto.registrable.activity

import be.sgl.backend.dto.AddressDTO
import be.sgl.backend.entity.registrable.activity.Activity
import jakarta.validation.constraints.*
import java.time.LocalDateTime

class CreateOrUpdateActivityRequest(
    @field:NotBlank(message = "{NotBlank.activity.name}")
    @field:Size(max = 50, message = "{Size.activity.name}")
    var name: String,
    @field:NotNull(message = "{NotNull.activity.start}")
    var start: LocalDateTime,
    @field:NotNull(message = "{NotNull.activity.end}")
    var end: LocalDateTime,
    @field:NotNull(message = "{NotNull.activity.closed}")
    var closed: LocalDateTime,
    @field:NotNull(message = "{NotNull.activity.cancellable}")
    var cancellable: Boolean,
    @field:NotBlank(message = "{NotBlank.activity.description}")
    var description: String,
    @field:NotNull(message = "{NotNull.activity.open}")
    var open: LocalDateTime,
    @field:NotNull(message = "{NotNull.activity.price}")
    @field:PositiveOrZero(message = "{PositiveOrZero.activity.price}")
    var price: Double,
    @field:Positive(message = "{Positive.activity.registrationLimit}")
    var registrationLimit: Int?,
    var address: AddressDTO?,
    var additionalForm: String?,
    @field:Size(max = 255, message = "{Size.activity.additionalFormRule}")
    var additionalFormRule: String?,
    @field:NotNull(message = "{NotNull.activity.sendConfirmation}")
    var sendConfirmation: Boolean,
    @field:NotNull(message = "{NotNull.activity.sendCompleteConfirmation}")
    var sendCompleteConfirmation: Boolean,
    @field:Email(message = "{Email.activity.communicationCC}")
    var communicationCC: String?,
    @field:NotNull(message = "{NotNull.activity.reductionFactor}")
    var reductionFactor: Double,
    @field:NotNull(message = "{NotNull.activity.siblingReduction}")
    var siblingReduction: Double,
    @field:NotEmpty(message = "{NotNull.activity.restrictions}")
    var restrictions: List<ActivityRestrictionDTO>,
    var cancelled: Boolean
) {

    /**
     * The activity of the restrictions is not set, as the activity is not persisted yet.
     */
    fun toEntity() = Activity().also {
        it.name = name
        it.description = description
        it.open = open
        it.closed = closed
        it.start = start
        it.end = end
        it.price = price
        it.registrationLimit = registrationLimit
        it.address = address?.toEntity()
        it.additionalForm = additionalForm
        it.additionalFormRule = additionalFormRule
        it.cancellable = cancellable
        it.sendConfirmation = sendConfirmation
        it.sendCompleteConfirmation = sendCompleteConfirmation
        it.communicationCC = communicationCC
        it.cancelled = cancelled
        it.reductionFactor = reductionFactor
        it.siblingReduction = siblingReduction
        it.restrictions = restrictions.map(ActivityRestrictionDTO::toEntity).toMutableList()
    }
}
