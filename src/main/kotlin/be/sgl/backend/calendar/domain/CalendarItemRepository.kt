package be.sgl.backend.calendar.domain

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface CalendarItemRepository : JpaRepository<CalendarItem, Int>
