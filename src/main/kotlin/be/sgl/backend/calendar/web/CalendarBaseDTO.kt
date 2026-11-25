package be.sgl.backend.calendar.web

import be.sgl.backend.calendar.domain.Calendar
import be.sgl.backend.organization.BranchSummary
import io.swagger.v3.oas.annotations.media.Schema

@Schema(name = "CalendarBase")
open class CalendarBaseDTO(
    val id: Int?,
    val branch: BranchSummary
) {
    companion object {
        fun of(calendar: Calendar, branch: BranchSummary): CalendarBaseDTO {
            return CalendarBaseDTO(calendar.id, branch)
        }
    }
}
