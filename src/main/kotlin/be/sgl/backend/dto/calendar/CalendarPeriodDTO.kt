package be.sgl.backend.dto.calendar

import be.sgl.backend.entity.calendar.CalendarPeriod
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

// read-only
@Schema(name = "CalendarPeriod")
data class CalendarPeriodDTO(
    val id: Int?,
    val name: String,
    val start: LocalDate,
    val end: LocalDate
) {
    companion object {
        fun of(period: CalendarPeriod): CalendarPeriodDTO {
            return CalendarPeriodDTO(period.id, period.name, period.start, period.end)
        }
    }
}
