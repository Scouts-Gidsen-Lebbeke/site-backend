package be.sgl.backend.repository

import be.sgl.backend.entity.calendar.CalendarPeriod
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
interface CalendarPeriodRepository : JpaRepository<CalendarPeriod, Int> {
    @Query("from CalendarPeriod where start between :start and :end or end between :start and :end")
    fun getOverlappingPeriods(start: LocalDate, end: LocalDate): List<CalendarPeriod>
}