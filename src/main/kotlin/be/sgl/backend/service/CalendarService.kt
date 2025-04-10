package be.sgl.backend.service

import be.sgl.backend.dto.CalendarDTO
import be.sgl.backend.dto.CalendarItemWithCalendarsDTO
import be.sgl.backend.dto.CalendarPeriodDTO
import be.sgl.backend.dto.CalendarUpdateDTO
import be.sgl.backend.entity.calendar.Calendar
import be.sgl.backend.entity.calendar.CalendarItem
import be.sgl.backend.entity.calendar.CalendarPeriod
import be.sgl.backend.mapper.AddressMapper
import be.sgl.backend.mapper.CalendarMapper
import be.sgl.backend.repository.BranchRepository
import be.sgl.backend.repository.calendar.CalendarItemRepository
import be.sgl.backend.repository.calendar.CalendarPeriodRepository
import be.sgl.backend.repository.calendar.CalendarRepository
import be.sgl.backend.service.ImageService.ImageDirectory.*
import be.sgl.backend.service.exception.CalendarItemNotFoundException
import be.sgl.backend.service.exception.CalendarNotFoundException
import be.sgl.backend.service.exception.CalendarPeriodNotFoundException
import jakarta.persistence.EntityManager
import jakarta.transaction.Transactional
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import java.time.DayOfWeek
import java.time.LocalDate

@Service
@Transactional
class CalendarService {

    @Autowired
    private lateinit var imageService: ImageService
    @Autowired
    private lateinit var periodRepository: CalendarPeriodRepository
    @Autowired
    private lateinit var calendarRepository: CalendarRepository
    @Autowired
    private lateinit var itemRepository: CalendarItemRepository
    @Autowired
    private lateinit var mapper: CalendarMapper
    @Autowired
    private lateinit var addressMapper: AddressMapper
    @Autowired
    private lateinit var branchRepository: BranchRepository
    @Autowired
    private lateinit var entityManager: EntityManager

    fun getAllCalendarPeriods(): List<CalendarPeriodDTO> {
        return periodRepository.findAll().map(mapper::toDto)
    }

    fun saveCalendarPeriodDTO(dto: CalendarPeriodDTO): CalendarPeriodDTO {
        verifyNoOverlaps(periodRepository.getOverlappingPeriods(dto.start, dto.end))
        val period = periodRepository.save(mapper.toEntity(dto))
        branchRepository.getBranchesWithCalendar().forEach {
            calendarRepository.save(Calendar(period, it))
        }
        return mapper.toDto(period)
    }

    fun mergeCalendarPeriodDTOChanges(id: Int, dto: CalendarPeriodDTO): CalendarPeriodDTO {
        val period = getPeriodById(id)
        verifyNoOverlaps(periodRepository.getOverlappingPeriods(dto.start, dto.end).filter { it.id == id })
        period.name = dto.name
        period.start = dto.start
        period.end = dto.end
        return mapper.toDto(periodRepository.save(mapper.toEntity(dto)))
    }

    fun deleteCalendarPeriod(id: Int) {
        val period = getPeriodById(id)
        calendarRepository.getCalendarsByPeriod(period).forEach(::deleteCalendar)
        periodRepository.delete(period)
    }

    private fun deleteCalendar(calendar: Calendar) {
        calendar.items.forEach {
            it.calendars.remove(calendar)
            if (it.calendars.isEmpty()) {
                itemRepository.delete(it)
            } else {
                itemRepository.save(it)
            }
        }
        calendarRepository.delete(calendar)
    }

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

    fun mergeCalendarDTOChanges(id: Int, calendarDTO: CalendarUpdateDTO): CalendarDTO {
        val calendar = getCalendarById(id)
        calendar.intro = calendarDTO.intro
        calendar.outro = calendarDTO.outro
        return mapper.toDto(calendarRepository.save(calendar))
    }

    private fun deleteCalendar(id: Int) {
        deleteCalendar(getCalendarById(id))
    }

    fun getCalendarItemDTOById(id: Int): CalendarItemWithCalendarsDTO {
        return mapper.toDtoWithCalendars(getItemById(id))
    }

    fun saveCalendarItemDTO(dto: CalendarItemWithCalendarsDTO): CalendarItemWithCalendarsDTO {
        val item = mapper.toEntity(dto)
        item.calendars.addAll(dto.calendars.mapNotNull { calendarRepository.findByIdOrNull(it.id) })
        item.image?.let { imageService.move(it, TEMPORARY, CALENDAR_ITEMS) }
        return mapper.toDtoWithCalendars(itemRepository.save(item))
    }

    fun mergeCalendarItemDTOChanges(id: Int, dto: CalendarItemWithCalendarsDTO): CalendarItemWithCalendarsDTO {
        val item = getItemById(id)
        item.start = dto.start
        item.end = dto.end
        item.title = dto.title
        item.content = dto.content
        item.closed = dto.closed
        item.calendars = dto.calendars.map(mapper::toEntity).toMutableList()
        item.address = dto.address?.let { addressMapper.toEntity(it) }
        if (item.image != dto.image) {
            item.image?.let { imageService.delete(CALENDAR_ITEMS, it) }
            dto.image?.let { imageService.move(it, TEMPORARY, CALENDAR_ITEMS) }
        }
        item.image = dto.image
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

    private fun verifyNoOverlaps(overlaps: List<CalendarPeriod>) {
        check(overlaps.isEmpty()) { "Calendar period overlaps with existing periods: ${overlaps.joinToString { it.name }}" }
    }
}