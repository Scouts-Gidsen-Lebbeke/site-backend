package be.sgl.backend.dto

import be.sgl.backend.entity.registrable.RegistrableStatus
import be.sgl.backend.entity.registrable.activity.ActivityRegistration
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
    val currentRegistration: ActivityRegistrationDTO?,
    val activeMembership: Boolean,
    val openOptions: List<ActivityRestrictionDTO>,
    val closedOptions: List<ActivityRestrictionDTO>,
    val medicsDate: LocalDateTime?,
    val medicalsUpToDate: Boolean
)