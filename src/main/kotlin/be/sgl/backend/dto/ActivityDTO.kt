package be.sgl.backend.dto

import be.sgl.backend.entity.registrable.RegistrableStatus
import java.io.Serializable
import java.time.LocalDateTime

// DTO for a list overview of activities
open class ActivityBaseDTO(
    val name: String,
    var start: LocalDateTime,
    var end: LocalDateTime
) : Serializable

// DTO for registration page and CRUD
class ActivityDTO(
    name: String,
    start: LocalDateTime,
    end: LocalDateTime,
    var open: LocalDateTime,
    var closed: LocalDateTime,
    var info: String,
    var price: Double,
    var limit: Int?,
    var address: AddressDTO,
    var additionalForm: String?,
    var additionalFormRule: String?,
    var cancellable: Boolean,
    var sendConfirmation: Boolean,
    var sendCompleteConfirmation: Boolean,
    var communicationCC: String?,
    var reductionFactor: Double,
    var siblingReduction: Double,
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