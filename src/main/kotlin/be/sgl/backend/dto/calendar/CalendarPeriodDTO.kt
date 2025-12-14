package be.sgl.backend.dto.calendar

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

// read-only
@Schema(name = "CalendarPeriod")
data class CalendarPeriodDTO(
    val id: Int?,
    val name: String,
    val start: LocalDate,
    val end: LocalDate
)