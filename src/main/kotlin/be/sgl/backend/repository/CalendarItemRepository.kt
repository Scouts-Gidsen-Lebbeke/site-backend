package be.sgl.backend.repository

import be.sgl.backend.entity.calendar.CalendarItem
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface CalendarItemRepository : JpaRepository<CalendarItem, Int>