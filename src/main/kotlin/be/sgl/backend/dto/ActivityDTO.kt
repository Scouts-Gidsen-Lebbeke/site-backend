package be.sgl.backend.dto

import be.sgl.backend.entity.registrable.RegistrableStatus
import jakarta.validation.constraints.*
import java.io.Serializable
import java.time.LocalDateTime

// DTO for a list overview of activities
open class ActivityBaseDTO(
    @NotBlank(message = "{NotBlank.activity.name}")
    @Size(max = 50, message = "{Size.activity.name}")
    val name: String,
    @NotNull(message = "{NotNull.activity.start}")
    var start: LocalDateTime,
    @NotNull(message = "{NotNull.activity.end}")
    var end: LocalDateTime
) : Serializable

// DTO for registration page and CRUD
class ActivityDTO(
    name: String,
    start: LocalDateTime,
    end: LocalDateTime,
    @NotBlank(message = "{NotBlank.activity.description}")
    var description: String,
    @NotNull(message = "{NotNull.activity.open}")
    var open: LocalDateTime,
    @NotNull(message = "{NotNull.activity.closed}")
    var closed: LocalDateTime,
    @NotNull(message = "{NotNull.activity.price}")
    @PositiveOrZero(message = "{PositiveOrZero.activity.price}")
    var price: Double,
    @Positive(message = "{Positive.activity.limit}")
    var limit: Int?,
    @NotNull(message = "{NotNull.activity.address}")
    var address: AddressDTO,
    var additionalForm: String?,
    @Size(max = 255, message = "{Size.activity.additionalFormRule}")
    var additionalFormRule: String?,
    @NotNull(message = "{NotNull.activity.cancellable}")
    var cancellable: Boolean,
    @NotNull(message = "{NotNull.activity.sendConfirmation}")
    var sendConfirmation: Boolean,
    @NotNull(message = "{NotNull.activity.sendCompleteConfirmation}")
    var sendCompleteConfirmation: Boolean,
    @Email(message = "{Email.activity.communicationCC}")
    var communicationCC: String?,
    @NotNull(message = "{NotNull.activity.reductionFactor}")
    var reductionFactor: Double,
    @NotNull(message = "{NotNull.activity.siblingReduction}")
    var siblingReduction: Double,
    @NotEmpty(message = "{NotNull.activity.restrictions}")
    var restrictions: List<ActivityRestrictionDTO>
) : ActivityBaseDTO(name, start, end)

data class ActivityRestrictionDTO(
    var branch: BranchBaseDTO,
    var name: String?,
    var alternativeStart: LocalDateTime?,
    var alternativeEnd: LocalDateTime?,
    var alternativePrice: Double?,
    var alternativeLimit: Int?
)

// DTO for statistics list overview
class ActivityResultDTO(
    name: String,
    start: LocalDateTime,
    end: LocalDateTime,
    var registrationCount: Int,
    var totalPrice: Double,
    var status: RegistrableStatus
) : ActivityBaseDTO(name, start, end)