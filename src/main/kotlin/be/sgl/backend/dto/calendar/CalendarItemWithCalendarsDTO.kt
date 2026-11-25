package be.sgl.backend.dto.calendar

import be.sgl.backend.shared.address.AddressDTO
import be.sgl.backend.entity.calendar.CalendarItem
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
) {
    companion object {
        fun of(item: CalendarItem): CalendarItemWithCalendarsDTO {
            return CalendarItemWithCalendarsDTO(item.id!!, item.start, item.end, item.title, item.content, item.image,
                item.closed, item.address?.let(AddressDTO::of), item.calendars.map(CalendarBaseDTO::of).toMutableList())
        }
    }
}
