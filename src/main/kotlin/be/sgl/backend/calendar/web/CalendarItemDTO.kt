package be.sgl.backend.calendar.web

import be.sgl.backend.shared.address.AddressDTO
import be.sgl.backend.calendar.domain.CalendarItem
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(name = "CalendarItem")
open class CalendarItemDTO(
    val id: Int?,
    val start: LocalDateTime,
    val end: LocalDateTime,
    val title: String,
    val content: String,
    val image: String?,
    val closed: Boolean,
    val address: AddressDTO?
) {
    companion object {
        fun of(item: CalendarItem): CalendarItemDTO {
            return CalendarItemDTO(item.id, item.start, item.end, item.title, item.content, item.image, item.closed,
                item.address?.let(AddressDTO::of))
        }
    }
}
