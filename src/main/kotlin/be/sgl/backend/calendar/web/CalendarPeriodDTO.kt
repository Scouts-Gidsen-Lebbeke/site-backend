package be.sgl.backend.calendar.web

import be.sgl.backend.calendar.domain.CalendarPeriod
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

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
