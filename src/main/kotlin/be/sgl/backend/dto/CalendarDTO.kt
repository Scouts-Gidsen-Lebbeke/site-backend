package be.sgl.backend.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.LocalDate
import java.time.LocalDateTime

// base for item linking
open class CalendarBaseDTO(
    val id: Int?,
    val branch: BranchBaseDTO // readonly
)

// full entity, for crud operations
class CalendarDTO(
    id: Int?,
    branch: BranchBaseDTO,
    val period: CalendarPeriodDTO, // readonly
    val intro: String?,
    val outro: String?,
    val items: MutableList<CalendarItemDTO>
) : CalendarBaseDTO(id, branch)

// for item listing
open class CalendarItemDTO(
    val id: Int?,
    val start: LocalDateTime,
    val end: LocalDateTime,
    @NotBlank(message = "{NotBlank.calendarItem.title}")
    @Size(max = 50, message = "{Size.calendarItem.title}")
    val title: String,
    @NotBlank(message = "{NotBlank.calendarItem.content}")
    val content: String,
    val image: String?,
    val closed: Boolean,
    val address: AddressDTO?
)

// for item edits
class CalendarItemWithCalendarsDTO(
    id: Int?,
    start: LocalDateTime,
    end: LocalDateTime,
    title: String,
    content: String,
    image: String?,
    closed: Boolean,
    address: AddressDTO?,
    val calendars: MutableList<CalendarBaseDTO>
) : CalendarItemDTO(id, start, end, title, content, image, closed, address)

data class CalendarPeriodDTO(
    val id: Int?,
    @NotBlank(message = "{NotBlank.calendarPeriod.name}")
    @Size(max = 50, message = "{Size.calendarPeriod.name}")
    val name: String,
    val start: LocalDate,
    val end: LocalDate
)