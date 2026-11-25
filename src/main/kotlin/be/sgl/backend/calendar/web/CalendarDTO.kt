package be.sgl.backend.calendar.web

import be.sgl.backend.calendar.domain.Calendar
import be.sgl.backend.organization.BranchSummary
import io.swagger.v3.oas.annotations.media.Schema

@Schema(name = "Calendar")
class CalendarDTO(
    val id: Int?,
    val branch: BranchSummary,
    val period: CalendarPeriodDTO,
    val intro: String?,
    val outro: String?,
    val items: MutableList<CalendarItemDTO>
) {
    companion object {
        fun of(calendar: Calendar, branch: BranchSummary): CalendarDTO {
            return CalendarDTO(calendar.id, branch, CalendarPeriodDTO.of(calendar.period),
                calendar.intro, calendar.outro, calendar.items.map(CalendarItemDTO::of).toMutableList())
        }
    }
}
