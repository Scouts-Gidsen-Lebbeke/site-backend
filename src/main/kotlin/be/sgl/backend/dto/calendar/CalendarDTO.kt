package be.sgl.backend.dto.calendar

import be.sgl.backend.organization.web.BranchBaseDTO
import be.sgl.backend.entity.calendar.Calendar
import io.swagger.v3.oas.annotations.media.Schema

// read-only
@Schema(name = "Calendar")
class CalendarDTO(
    val id: Int?,
    val branch: BranchBaseDTO,
    val period: CalendarPeriodDTO,
    val intro: String?,
    val outro: String?,
    val items: MutableList<CalendarItemDTO>
) {
    companion object {
        fun of(calendar: Calendar): CalendarDTO {
            return CalendarDTO(calendar.id, BranchBaseDTO.of(calendar.branch), CalendarPeriodDTO.of(calendar.period),
                calendar.intro, calendar.outro, calendar.items.map(CalendarItemDTO::of).toMutableList())
        }
    }
}
