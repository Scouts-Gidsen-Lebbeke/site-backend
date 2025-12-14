package be.sgl.backend.dto.calendar

import be.sgl.backend.util.StartEndDate
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.LocalDate

data class CreateOrUpdateCalendarPeriodRequest(
    @field:NotBlank(message = "{NotBlank.calendarPeriod.name}")
    @field:Size(max = 50, message = "{Size.calendarPeriod.name}")
    var name: String?,
    @field:NotNull(message = "{NotNull.calendarPeriod.start}")
    override var start: LocalDate?,
    @field:NotNull(message = "{NotNull.calendarPeriod.end}")
    override var end: LocalDate?
) : StartEndDate