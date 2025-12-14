package be.sgl.backend.service.calendar

import be.sgl.backend.dto.calendar.*
import be.sgl.backend.entity.calendar.Calendar
import be.sgl.backend.entity.calendar.CalendarItem
import be.sgl.backend.entity.calendar.CalendarPeriod
import be.sgl.backend.mapper.AddressMapper
import be.sgl.backend.mapper.calendar.CalendarMapper
import be.sgl.backend.repository.calendar.CalendarItemRepository
import be.sgl.backend.repository.calendar.CalendarPeriodRepository
import be.sgl.backend.repository.calendar.CalendarRepository
import be.sgl.backend.service.ImageService
import be.sgl.backend.service.ImageService.ImageDirectory.*
import be.sgl.backend.service.exception.CalendarItemNotFoundException
import be.sgl.backend.service.exception.CalendarNotFoundException
import be.sgl.backend.service.exception.CalendarPeriodNotFoundException
import jakarta.persistence.EntityManager
import jakarta.transaction.Transactional
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import java.time.DayOfWeek
import java.time.LocalDate

@Service
@Transactional
class CalendarService(
    private val imageService: ImageService,
    private val periodRepository: CalendarPeriodRepository,
    private val calendarRepository: CalendarRepository,
    private val itemRepository: CalendarItemRepository,
    private val mapper: CalendarMapper,
    private val addressMapper: AddressMapper,
    private val entityManager: EntityManager
) {

    fun getCurrentCalendars(): List<CalendarDTO> {
        return calendarRepository.getCurrentCalendars().map(mapper::toDto)
    }

    fun getCalendarsByPeriod(periodId: Int): List<CalendarDTO> {
        val period = getPeriodById(periodId)
        return calendarRepository.getCalendarsByPeriod(period).map(mapper::toDto)
    }

    fun getCalendarDTOById(id: Int, withDefaults: Boolean): CalendarDTO {
        val calendar = getCalendarById(id)
        if (withDefaults) {
            for ((i, sunday) in getSundaysBetween(calendar.period.start, calendar.period.end).withIndex()) {
                if (calendar.items.count { it.end < sunday.atTime(23, 59) } <= i) {
                    val newItem = CalendarItem.defaultItem(sunday, calendar)
                    calendar.items.add(newItem)
                }
            }
        }
        calendar.items.sortBy { it.start }
        entityManager.detach(calendar) // new items get persisted otherwise
        return mapper.toDto(calendar)
    }

    private fun getSundaysBetween(startDate: LocalDate, endDate: LocalDate): List<LocalDate> {
        val sundays = mutableListOf<LocalDate>()
        var current = startDate.with(DayOfWeek.SUNDAY)
        if (current.isBefore(startDate)) {
            current = current.plusWeeks(1)
        }
        while (!current.isAfter(endDate)) {
            sundays.add(current)
            current = current.plusWeeks(1)
        }
        return sundays
    }

    fun updateCalendar(id: Int, request: UpdateCalendarRequest): CalendarDTO {
        val calendar = getCalendarById(id)
        calendar.intro = request.intro
        calendar.outro = request.outro
        return mapper.toDto(calendarRepository.save(calendar))
    }

    fun getCalendarItemDTOById(id: Int): CalendarItemWithCalendarsDTO {
        return mapper.toDtoWithCalendars(getItemById(id))
    }

    fun createCalendarItem(request: CreateOrUpdateCalendarItemRequest): CalendarItemWithCalendarsDTO {
        val item = mapper.toEntity(request)
        item.calendars.addAll(request.calendars.mapNotNull { it.id?.let(calendarRepository::findByIdOrNull) })
        item.image?.let { imageService.move(it, TEMPORARY, CALENDAR_ITEMS) }
        return mapper.toDtoWithCalendars(itemRepository.save(item))
    }

    fun updateCalendarItem(id: Int, request: CreateOrUpdateCalendarItemRequest): CalendarItemWithCalendarsDTO {
        val item = getItemById(id)
        request.start?.let { item.start = it }
        request.end?.let { item.end = it }
        request.title?.let { item.title = it }
        request.content?.let { item.content = it }
        request.closed?.let { item.closed = it }
        item.calendars = request.calendars.map(mapper::toEntity).toMutableList()
        item.address = request.address?.let { addressMapper.toEntity(it) }
        if (item.image != request.image) {
            item.image?.let { imageService.delete(CALENDAR_ITEMS, it) }
            request.image?.let { imageService.move(it, TEMPORARY, CALENDAR_ITEMS) }
        }
        item.image = request.image
        return mapper.toDtoWithCalendars(itemRepository.save(item))
    }

    fun deleteCalendarItem(id: Int) {
        deleteCalendarItem(getItemById(id))
    }

    private fun deleteCalendarItem(item: CalendarItem) {
        item.image?.let { imageService.delete(CALENDAR_ITEMS, it) }
        itemRepository.delete(item)
    }

    private fun getPeriodById(id: Int): CalendarPeriod {
        return periodRepository.findById(id).orElseThrow { CalendarPeriodNotFoundException() }
    }

    private fun getCalendarById(id: Int): Calendar {
        return calendarRepository.findById(id).orElseThrow { CalendarNotFoundException() }
    }

    private fun getItemById(id: Int): CalendarItem {
        return itemRepository.findById(id).orElseThrow { CalendarItemNotFoundException() }
    }
}