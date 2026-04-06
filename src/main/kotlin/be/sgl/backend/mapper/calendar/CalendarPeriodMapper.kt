package be.sgl.backend.mapper.calendar

import be.sgl.backend.dto.calendar.CalendarPeriodDTO
import be.sgl.backend.dto.calendar.CreateOrUpdateCalendarPeriodRequest
import be.sgl.backend.entity.calendar.CalendarPeriod
import org.mapstruct.Mapper

@Mapper(componentModel = "spring")
interface CalendarPeriodMapper {
    fun toDto(calendarPeriod: CalendarPeriod): CalendarPeriodDTO
    fun toEntity(dto: CreateOrUpdateCalendarPeriodRequest): CalendarPeriod
}