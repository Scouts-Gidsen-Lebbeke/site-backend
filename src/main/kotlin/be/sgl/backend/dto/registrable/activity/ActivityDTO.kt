package be.sgl.backend.dto.registrable.activity

import be.sgl.backend.dto.AddressDTO
import be.sgl.backend.dto.branch.BranchBaseDTO
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.*
import java.io.Serializable
import java.time.LocalDateTime

// DTO for a list overview of activities
@Schema(name = "ActivityBase", description = "Basic information about an activity.")
open class ActivityBaseDTO(
    val id: Int?,
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
    var cancellable: Boolean
) : Serializable

// DTO for registration page and CRUD
@Schema(name = "Activity", description = "The complete activity configuration.")
class ActivityDTO(
    id: Int?,
    name: String,
    start: LocalDateTime,
    end: LocalDateTime,
    closed: LocalDateTime,
    cancellable: Boolean,
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
) : ActivityBaseDTO(id, name, start, end, closed, cancellable)

@Schema(name = "ActivityRestriction", description = "A limitation on the activity registration ability for a branch.")
data class ActivityRestrictionDTO(
    val id: Int?,
    val branch: BranchBaseDTO,
    val name: String?,
    val alternativeStart: LocalDateTime?,
    val alternativeEnd: LocalDateTime?,
    var alternativePrice: Double?,
    val alternativeLimit: Int?
)

data class ActivityRegistrationStatusDTO(
    var currentRegistration: ActivityRegistrationDTO? = null,
    var activeMembership: Boolean = true,
    var openOptions: MutableList<ActivityRestrictionDTO> = mutableListOf(),
    var closedOptions: MutableList<ActivityRestrictionDTO> = mutableListOf(),
    var medicsDate: LocalDateTime? = null,
    var medicalsUpToDate: Boolean = false
)