package be.sgl.backend.calendar.application

import be.sgl.backend.calendar.web.*
import be.sgl.backend.calendar.domain.Calendar
import be.sgl.backend.calendar.domain.CalendarItem
import be.sgl.backend.calendar.domain.CalendarPeriod
import be.sgl.backend.calendar.domain.CalendarItemRepository
import be.sgl.backend.calendar.domain.CalendarPeriodRepository
import be.sgl.backend.calendar.domain.CalendarRepository
import be.sgl.backend.organization.BranchApi
import be.sgl.backend.shared.image.ImageService
import be.sgl.backend.shared.image.ImageService.ImageDirectory.*
import be.sgl.backend.shared.exception.CalendarItemNotFoundException
import be.sgl.backend.shared.exception.CalendarNotFoundException
import be.sgl.backend.shared.exception.CalendarPeriodNotFoundException
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
    private val branchApi: BranchApi,
    private val periodRepository: CalendarPeriodRepository,
    private val calendarRepository: CalendarRepository,
    private val itemRepository: CalendarItemRepository,
    private val entityManager: EntityManager
) {

    fun getCurrentCalendars(): List<CalendarDTO> {
        return toDTOs(calendarRepository.getCurrentCalendars())
    }

    fun getCalendarsByPeriod(periodId: Int): List<CalendarDTO> {
        val period = getPeriodById(periodId)
        return toDTOs(calendarRepository.getCalendarsByPeriod(period))
    }

    fun getCalendarDTOById(id: Int, withDefaults: Boolean): CalendarDTO {
        val calendar = getCalendarById(id)
        if (withDefaults) {
            val existingItems = calendar.items.toList()
            for (sunday in getSundaysBetween(calendar.period.start, calendar.period.end)) {
                if (existingItems.none { it.start.toLocalDate() <= sunday && it.end.toLocalDate() >= sunday.minusDays(6) }) {
                    val newItem = CalendarItem.defaultItem(sunday, calendar)
                    calendar.items.add(newItem)
                }
            }
        }
        calendar.items.sortBy { it.start }
        entityManager.detach(calendar)
        return toDTO(calendar)
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
        return toDTO(calendarRepository.save(calendar))
    }

    fun getCalendarItemDTOById(id: Int): CalendarItemWithCalendarsDTO {
        return toDTO(getItemById(id))
    }

    fun createCalendarItem(request: CreateOrUpdateCalendarItemRequest): CalendarItemWithCalendarsDTO {
        val newItem = request.toEntity()
        newItem.calendars.addAll(request.calendars.mapNotNull(calendarRepository::findByIdOrNull))
        newItem.image?.let { imageService.move(it, TEMPORARY, CALENDAR_ITEMS) }
        return toDTO(itemRepository.save(newItem))
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
        return toDTO(itemRepository.save(itemToUpdate))
    }

    fun deleteCalendarItem(id: Int) {
        deleteCalendarItem(getItemById(id))
    }

    private fun deleteCalendarItem(item: CalendarItem) {
        item.image?.let { imageService.delete(CALENDAR_ITEMS, it) }
        itemRepository.delete(item)
    }

    private fun toDTOs(calendars: List<Calendar>): List<CalendarDTO> {
        val branches = branchApi.findSummaries(calendars.map(Calendar::branchId).toSet())
        return calendars.map { CalendarDTO.of(it, branches.getValue(it.branchId)) }
    }

    private fun toDTO(calendar: Calendar): CalendarDTO {
        return toDTOs(listOf(calendar)).single()
    }

    private fun toDTO(item: CalendarItem): CalendarItemWithCalendarsDTO {
        val branches = branchApi.findSummaries(item.calendars.map(Calendar::branchId).toSet())
        return CalendarItemWithCalendarsDTO.of(item, branches)
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