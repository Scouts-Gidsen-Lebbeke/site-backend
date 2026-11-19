package be.sgl.backend.dto.registrable.activity

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

// read-only
@Schema(name = "ActivityRegistrationStatus")
data class ActivityRegistrationStatusDTO(
    var currentRegistration: ActivityRegistrationDTO? = null,
    var activeMembership: Boolean = true,
    var openOptions: MutableList<ActivityRestrictionDTO> = mutableListOf(),
    var closedOptions: MutableList<ActivityRestrictionDTO> = mutableListOf(),
    var medicsDate: LocalDateTime? = null,
    var medicalsUpToDate: Boolean = false
) {
    companion object {
        fun of(status: ActivityRegistrationStatus): ActivityRegistrationStatusDTO {
            return ActivityRegistrationStatusDTO(status.currentRegistration?.let(ActivityRegistrationDTO::of),
                status.activeMembership, status.openOptions.map(ActivityRestrictionDTO::of).toMutableList(),
                status.closedOptions.map(ActivityRestrictionDTO::of).toMutableList(), status.medicsDate,
                status.medicalsUpToDate)
        }
    }
}
