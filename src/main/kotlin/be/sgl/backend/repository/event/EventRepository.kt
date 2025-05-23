package be.sgl.backend.repository.event

import be.sgl.backend.entity.registrable.event.Event
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface EventRepository : JpaRepository<Event, Int> {
    @Query("from Event order by start desc")
    fun findAllRecentFirst(): List<Event>
    @Query("from Event where now() between open and end and not cancelled order by start")
    fun findAllVisibleEvents(): List<Event>
}