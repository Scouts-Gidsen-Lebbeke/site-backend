package be.sgl.backend.service.calendar

import be.sgl.backend.dto.calendar.*
import be.sgl.backend.entity.calendar.Calendar
import be.sgl.backend.entity.calendar.CalendarPeriod
import be.sgl.backend.mapper.calendar.CalendarPeriodMapper
import be.sgl.backend.repository.branch.BranchRepository
import be.sgl.backend.repository.calendar.CalendarItemRepository
import be.sgl.backend.repository.calendar.CalendarPeriodRepository
import be.sgl.backend.repository.calendar.CalendarRepository
import be.sgl.backend.exception.CalendarPeriodNotFoundException
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
@Transactional
class CalendarPeriodService(
    private val periodRepository: CalendarPeriodRepository,
    private val calendarRepository: CalendarRepository,
    private val itemRepository: CalendarItemRepository,
    private val mapper: CalendarPeriodMapper,
    private val branchRepository: BranchRepository,
) {

    fun getAllCalendarPeriods(): List<CalendarPeriodDTO> {
        return periodRepository.findAll().map(mapper::toDto)
    }

    fun getCalendarPeriodDTOById(id: Int): CalendarPeriodDTO {
        return mapper.toDto(getPeriodById(id))
    }

    fun createCalendarPeriod(request: CreateOrUpdateCalendarPeriodRequest): CalendarPeriodDTO {
        var newPeriod = mapper.toEntity(request)
        verifyNoOverlaps(newPeriod.start, newPeriod.end)
        newPeriod = periodRepository.save(newPeriod)
        branchRepository.getBranchesWithCalendar().forEach {
            calendarRepository.save(Calendar(newPeriod, it))
        }
        return mapper.toDto(newPeriod)
    }

    fun updateCalendarPeriod(id: Int, request: CreateOrUpdateCalendarPeriodRequest): CalendarPeriodDTO {
        val periodToUpdate = getPeriodById(id)
        request.name?.let { periodToUpdate.name = it }
        request.start?.let { periodToUpdate.start = it }
        request.end?.let { periodToUpdate.end = it }
        verifyNoOverlaps(periodToUpdate.start, periodToUpdate.end, id)
        return mapper.toDto(periodRepository.save(periodToUpdate))
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

    private fun getPeriodById(id: Int): CalendarPeriod {
        return periodRepository.findById(id).orElseThrow { CalendarPeriodNotFoundException() }
    }

    private fun verifyNoOverlaps(start: LocalDate, end: LocalDate, id: Int? = null) {
        val overlaps = periodRepository.getOverlappingPeriods(start, end).filter { it.id != id }
        check(overlaps.isEmpty()) { "Calendar period overlaps with existing periods: ${overlaps.joinToString { it.name }}" }
    }
}