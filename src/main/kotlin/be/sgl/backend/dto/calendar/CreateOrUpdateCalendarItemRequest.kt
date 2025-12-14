package be.sgl.backend.dto.calendar

import be.sgl.backend.dto.AddressDTO
import be.sgl.backend.util.StartEndTime
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.LocalDateTime

class CreateOrUpdateCalendarItemRequest(
    @field:NotNull(message = "{NotNull.calendarItem.start}")
    override var start: LocalDateTime?,
    @field:NotNull(message = "{NotNull.calendarItem.end}")
    override var end: LocalDateTime?,
    @field:NotBlank(message = "{NotBlank.calendarItem.title}")
    @field:Size(max = 50, message = "{Size.calendarItem.title}")
    var title: String?,
    @field:NotBlank(message = "{NotBlank.calendarItem.content}")
    var content: String?,
    var image: String?,
    var closed: Boolean?,
    var address: AddressDTO?,
    @field:NotEmpty(message = "{NotEmpty.calendarItem.calendars}")
    var calendars: MutableList<CalendarBaseDTO>
) : StartEndTime