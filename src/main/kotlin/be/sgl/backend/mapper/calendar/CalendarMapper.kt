package be.sgl.backend.mapper.calendar

import be.sgl.backend.dto.calendar.*
import be.sgl.backend.entity.calendar.Calendar
import be.sgl.backend.entity.calendar.CalendarItem
import org.mapstruct.Mapper
import org.mapstruct.Mapping

@Mapper(componentModel = "spring")
interface CalendarMapper {
    fun toDto(calendar: Calendar): CalendarDTO
    fun toDtoWithCalendars(calendarItem: CalendarItem): CalendarItemWithCalendarsDTO
    @Mapping(target = "calendars", ignore = true)
    fun toEntity(dto: CreateOrUpdateCalendarItemRequest): CalendarItem
}