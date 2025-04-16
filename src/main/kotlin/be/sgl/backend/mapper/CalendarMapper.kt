package be.sgl.backend.mapper

import be.sgl.backend.dto.*
import be.sgl.backend.entity.calendar.Calendar
import be.sgl.backend.entity.calendar.CalendarItem
import be.sgl.backend.entity.calendar.CalendarPeriod
import org.mapstruct.Mapper

@Mapper(componentModel = "spring")
interface CalendarMapper {
    fun toDto(calendar: Calendar): CalendarDTO
    fun toBaseDto(calendar: Calendar): CalendarBaseDTO
    fun toEntity(dto: CalendarBaseDTO): Calendar
    fun toDto(calendarItem: CalendarItem): CalendarItemDTO
    fun toDtoWithCalendars(calendarItem: CalendarItem): CalendarItemWithCalendarsDTO
    fun toEntity(dto: CalendarItemDTO): CalendarItem
    fun toDto(calendarPeriod: CalendarPeriod): CalendarPeriodDTO
    fun toEntity(dto: CalendarPeriodDTO): CalendarPeriod
}