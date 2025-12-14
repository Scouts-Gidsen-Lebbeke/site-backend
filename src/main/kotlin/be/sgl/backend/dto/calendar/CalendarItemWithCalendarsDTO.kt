package be.sgl.backend.dto.calendar

import be.sgl.backend.dto.AddressDTO
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(name = "CalendarItemWithCalendars")
class CalendarItemWithCalendarsDTO(
    val id: Int,
    val start: LocalDateTime,
    val end: LocalDateTime,
    val title: String,
    val content: String,
    val image: String?,
    val closed: Boolean,
    val address: AddressDTO?,
    val calendars: MutableList<CalendarBaseDTO>
)