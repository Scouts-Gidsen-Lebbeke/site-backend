package be.sgl.backend.repository

import be.sgl.backend.entity.registrable.event.Event
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface EventRepository : JpaRepository<Event, Int> {
    fun findAllByEndAfterOrderByStart(end: LocalDateTime): List<Event>
}