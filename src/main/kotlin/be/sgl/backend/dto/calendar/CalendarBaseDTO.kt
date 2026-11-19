package be.sgl.backend.dto.calendar

import be.sgl.backend.dto.branch.BranchBaseDTO
import be.sgl.backend.entity.calendar.Calendar
import io.swagger.v3.oas.annotations.media.Schema

// read-only
@Schema(name = "CalendarBase")
open class CalendarBaseDTO(
    val id: Int?,
    val branch: BranchBaseDTO
) {
    companion object {
        fun of(calendar: Calendar): CalendarBaseDTO {
            return CalendarBaseDTO(calendar.id, BranchBaseDTO.of(calendar.branch))
        }
    }
}
