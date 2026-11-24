package be.sgl.backend.service.calendar

import be.sgl.backend.dto.calendar.*
import be.sgl.backend.entity.calendar.Calendar
import be.sgl.backend.entity.calendar.CalendarItem
import be.sgl.backend.entity.calendar.CalendarPeriod
import be.sgl.backend.repository.calendar.CalendarItemRepository
import be.sgl.backend.repository.calendar.CalendarPeriodRepository
import be.sgl.backend.repository.calendar.CalendarRepository
import be.sgl.backend.service.ImageService
import be.sgl.backend.service.ImageService.ImageDirectory.*
import be.sgl.backend.exception.CalendarItemNotFoundException
import be.sgl.backend.exception.CalendarNotFoundException
import be.sgl.backend.exception.CalendarPeriodNotFoundException
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
    private val entityManager: EntityManager
) {

    fun getCurrentCalendars(): List<CalendarDTO> {
        return calendarRepository.getCurrentCalendars().map(CalendarDTO::of)
    }

    fun getCalendarsByPeriod(periodId: Int): List<CalendarDTO> {
        val period = getPeriodById(periodId)
        return calendarRepository.getCalendarsByPeriod(period).map(CalendarDTO::of)
    }

    fun getCalendarDTOById(id: Int, withDefaults: Boolean): CalendarDTO {
        val calendar = getCalendarById(id)
        if (withDefaults) {
            val existingItems = calendar.items.toList()
            for (sunday in getSundaysBetween(calendar.period.start, calendar.period.end)) {
                // Each week (monday until sunday) without any item gets a default item on its sunday
                if (existingItems.none { it.start.toLocalDate() <= sunday && it.end.toLocalDate() >= sunday.minusDays(6) }) {
                    val newItem = CalendarItem.defaultItem(sunday, calendar)
                    calendar.items.add(newItem)
                }
            }
        }
        calendar.items.sortBy { it.start }
        entityManager.detach(calendar) // new items get persisted otherwise
        return CalendarDTO.of(calendar)
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
        return CalendarDTO.of(calendarRepository.save(calendar))
    }

    fun getCalendarItemDTOById(id: Int): CalendarItemWithCalendarsDTO {
        return CalendarItemWithCalendarsDTO.of(getItemById(id))
    }

    fun createCalendarItem(request: CreateOrUpdateCalendarItemRequest): CalendarItemWithCalendarsDTO {
        val newItem = request.toEntity()
        newItem.calendars.addAll(request.calendars.mapNotNull(calendarRepository::findByIdOrNull))
        newItem.image?.let { imageService.move(it, TEMPORARY, CALENDAR_ITEMS) }
        return CalendarItemWithCalendarsDTO.of(itemRepository.save(newItem))
    }

    fun updateCalendarItem(id: Int, request: CreateOrUpdateCalendarItemRequest): CalendarItemWithCalendarsDTO {
        val itemToUpdate = getItemById(id)
        val itemFromDto = request.toEntity()
        itemToUpdate.start = itemFromDto.start
        itemToUpdate.end = itemFromDto.end
        itemToUpdate.title = itemFromDto.title
        itemToUpdate.content = itemFromDto.content
        itemToUpdate.closed = itemFromDto.closed
        itemToUpdate.calendars = request.calendars.mapNotNull(calendarRepository::findByIdOrNull).toMutableList()
        itemToUpdate.address = itemFromDto.address
        if (itemToUpdate.image != itemFromDto.image) {
            itemToUpdate.image?.let { imageService.delete(CALENDAR_ITEMS, it) }
            itemFromDto.image?.let { imageService.move(it, TEMPORARY, CALENDAR_ITEMS) }
        }
        itemToUpdate.image = itemFromDto.image
        return CalendarItemWithCalendarsDTO.of(itemRepository.save(itemToUpdate))
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