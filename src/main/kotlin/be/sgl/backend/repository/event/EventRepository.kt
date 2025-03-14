package be.sgl.backend.repository.event

import be.sgl.backend.entity.registrable.event.Event
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface EventRepository : JpaRepository<Event, Int> {
    @Query("from Event where now() between open and end")
    fun findAllVisibleEvents(): List<Event>
}