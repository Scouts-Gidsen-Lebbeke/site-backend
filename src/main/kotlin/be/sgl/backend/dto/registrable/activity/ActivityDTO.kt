package be.sgl.backend.dto.registrable.activity

import be.sgl.backend.dto.AddressDTO
import be.sgl.backend.dto.branch.BranchBaseDTO
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

// read-only
@Schema(name = "Activity", description = "The complete activity configuration.")
data class ActivityDTO(
    val id: Int,
    val name: String,
    val start: LocalDateTime,
    val end: LocalDateTime,
    val closed: LocalDateTime,
    val cancellable: Boolean,
    val description: String,
    val open: LocalDateTime,
    val price: Double,
    val registrationLimit: Int?,
    val address: AddressDTO?,
    val additionalForm: String?,
    val additionalFormRule: String?,
    val sendConfirmation: Boolean,
    val sendCompleteConfirmation: Boolean,
    val communicationCC: String?,
    val reductionFactor: Double,
    val siblingReduction: Double,
    val restrictions: List<ActivityRestrictionDTO>,
    val cancelled: Boolean
)