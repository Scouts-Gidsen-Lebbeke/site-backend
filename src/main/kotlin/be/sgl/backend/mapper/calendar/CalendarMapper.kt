package be.sgl.backend.mapper.calendar

import be.sgl.backend.dto.calendar.*
import be.sgl.backend.entity.calendar.Calendar
import be.sgl.backend.entity.calendar.CalendarItem
import org.mapstruct.Mapper

@Mapper(componentModel = "spring")
interface CalendarMapper {
    fun toDto(calendar: Calendar): CalendarDTO
    fun toBaseDto(calendar: Calendar): CalendarBaseDTO
    fun toEntity(dto: CalendarBaseDTO): Calendar
    fun toDto(calendarItem: CalendarItem): CalendarItemDTO
    fun toDtoWithCalendars(calendarItem: CalendarItem): CalendarItemWithCalendarsDTO
    fun toEntity(dto: CreateOrUpdateCalendarItemRequest): CalendarItem
}