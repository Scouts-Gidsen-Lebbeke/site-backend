package be.sgl.backend.dto

import be.sgl.backend.util.StartEndDate
import be.sgl.backend.util.StartEndTime
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
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
    var id: Int?,
    @field:NotNull(message = "{NotNull.calendarItem.start}")
    override var start: LocalDateTime,
    @field:NotNull(message = "{NotNull.calendarItem.end}")
    override var end: LocalDateTime,
    @field:NotBlank(message = "{NotBlank.calendarItem.title}")
    @field:Size(max = 50, message = "{Size.calendarItem.title}")
    var title: String,
    @field:NotBlank(message = "{NotBlank.calendarItem.content}")
    var content: String,
    var image: String?,
    var closed: Boolean,
    var address: AddressDTO?
) : StartEndTime

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
    @field:NotEmpty(message = "{NotEmpty.calendarItem.calendars}")
    var calendars: MutableList<CalendarBaseDTO>
) : CalendarItemDTO(id, start, end, title, content, image, closed, address)

data class CalendarPeriodDTO(
    var id: Int?,
    @field:NotBlank(message = "{NotBlank.calendarPeriod.name}")
    @field:Size(max = 50, message = "{Size.calendarPeriod.name}")
    var name: String,
    @field:NotNull(message = "{NotNull.calendarPeriod.start}")
    override var start: LocalDate,
    @field:NotNull(message = "{NotNull.calendarPeriod.end}")
    override var end: LocalDate
) : StartEndDate