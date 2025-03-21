package be.sgl.backend.dto

import be.sgl.backend.entity.registrable.RegistrableStatus
import be.sgl.backend.entity.registrable.RegistrableStatus.Companion.getStatus
import be.sgl.backend.entity.registrable.activity.Activity
import be.sgl.backend.entity.registrable.activity.ActivityRegistration
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.*
import java.io.Serializable
import java.time.LocalDateTime

// DTO for a list overview of activities
@Schema(description = "Basic information about an activity.")
open class ActivityBaseDTO(
    val id: Int?,
    @field:NotBlank(message = "{NotBlank.activity.name}")
    @field:Size(max = 50, message = "{Size.activity.name}")
    var name: String,
    @field:NotNull(message = "{NotNull.activity.start}")
    var start: LocalDateTime,
    @field:NotNull(message = "{NotNull.activity.end}")
    var end: LocalDateTime
) : Serializable

// DTO for registration page and CRUD
@Schema(description = "The complete activity configuration.")
class ActivityDTO(
    id: Int?,
    name: String,
    start: LocalDateTime,
    end: LocalDateTime,
    @field:NotBlank(message = "{NotBlank.activity.description}")
    var description: String,
    @field:NotNull(message = "{NotNull.activity.open}")
    var open: LocalDateTime,
    @field:NotNull(message = "{NotNull.activity.closed}")
    var closed: LocalDateTime,
    @field:NotNull(message = "{NotNull.activity.price}")
    @field:PositiveOrZero(message = "{PositiveOrZero.activity.price}")
    var price: Double,
    @field:Positive(message = "{Positive.activity.registrationLimit}")
    var registrationLimit: Int?,
    @field:NotNull(message = "{NotNull.activity.address}")
    var address: AddressDTO,
    var additionalForm: String?,
    @field:Size(max = 255, message = "{Size.activity.additionalFormRule}")
    var additionalFormRule: String?,
    @field:NotNull(message = "{NotNull.activity.cancellable}")
    var cancellable: Boolean,
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
    var restrictions: List<ActivityRestrictionDTO>
) : ActivityBaseDTO(id, name, start, end)

@Schema(description = "A limitation on the activity registration ability for a branch.")
data class ActivityRestrictionDTO(
    val id: Int?,
    val branch: BranchBaseDTO,
    val name: String?,
    val alternativeStart: LocalDateTime?,
    val alternativeEnd: LocalDateTime?,
    val alternativePrice: Double?,
    val alternativeLimit: Int?
)

// DTO for statistics list overview
class ActivityResultDTO(
    id: Int?,
    name: String,
    start: LocalDateTime,
    end: LocalDateTime,
    var registrationCount: Int,
    var totalPrice: Double,
    var status: RegistrableStatus
) : ActivityBaseDTO(id, name, start, end) {
    constructor(activity: Activity, registrations: List<ActivityRegistration>) :
            this(activity.id, activity.name, activity.start, activity.end, registrations.count(), registrations.sumOf { it.price }, activity.getStatus())
}

/**
 * DTO for user feedback about the current activity:
 *  - First of all, the activity should be open for registrations. This check should be happened on activity retrieval.
 *  - The user can only register if he hasn't already a paid registration.
 *  - If the user has a pending registration, it should be finished before creating another one.
 *  - The user can only register when he has an active branch membership.
 *  - When he has an active branch, it should have a matching restriction, meaning not both of the options are empty.
 *  - When an option is present for this member, it should be part of the open options (otherwise the limit is reached).
 *  - When it has one or more valid restrictions, its medical info should be present.
 *  - When its medical info is existing, it should still be up to date (according to the up-to-date flag).
 */
data class ActivityRegistrationStatus(
    val currentRegistration: ActivityRegistrationDTO? = null,
    val activeMembership: Boolean = true,
    val openOptions: List<ActivityRestrictionDTO> = emptyList(),
    val closedOptions: List<ActivityRestrictionDTO> = emptyList(),
    val medicsDate: LocalDateTime? = null,
    val medicalsUpToDate: Boolean = false
)