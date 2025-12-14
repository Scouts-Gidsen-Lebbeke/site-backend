package be.sgl.backend.dto.calendar

import be.sgl.backend.dto.branch.BranchBaseDTO
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
)