package be.sgl.backend.repository.calendar

import be.sgl.backend.entity.calendar.Calendar
import be.sgl.backend.entity.calendar.CalendarPeriod
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface CalendarRepository : JpaRepository<Calendar, Int> {
    @Query("from Calendar where now() between period.start and period.end")
    fun getCurrentCalendars(): List<Calendar>
    @Query("from Calendar where period = :period")
    fun getCalendarsByPeriod(period: CalendarPeriod): List<Calendar>
    fun deleteCalendarsByPeriod(period: CalendarPeriod)
}