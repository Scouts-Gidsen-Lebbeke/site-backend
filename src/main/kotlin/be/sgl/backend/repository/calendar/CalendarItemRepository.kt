package be.sgl.backend.repository.calendar

import be.sgl.backend.entity.calendar.CalendarItem
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface CalendarItemRepository : JpaRepository<CalendarItem, Int> {
    fun findByClosedFalseAndStartAfterAndEndBefore(from: LocalDateTime, to: LocalDateTime): List<CalendarItem>
}