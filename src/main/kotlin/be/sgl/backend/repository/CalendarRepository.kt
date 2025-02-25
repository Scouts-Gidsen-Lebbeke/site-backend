package be.sgl.backend.repository

import be.sgl.backend.entity.calendar.Calendar
import be.sgl.backend.entity.calendar.CalendarPeriod
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface CalendarRepository : JpaRepository<Calendar, Int> {
    @Query("from Calendar where now() between period.start and period.end")
    fun getCurrentCalendars(): List<Calendar>
    @Query("from Calendar where branch = :branch and now() between period.start and period.end")
    fun getCalendarsByPeriod(period: CalendarPeriod): List<Calendar>
    fun deleteCalendarsByPeriod(period: CalendarPeriod)
}