package be.sgl.backend.repository.event

import be.sgl.backend.entity.registrable.event.Event
import be.sgl.backend.entity.registrable.event.EventRegistration
import be.sgl.backend.entity.user.User
import be.sgl.backend.repository.PaymentRepository
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface EventRegistrationRepository : JpaRepository<EventRegistration, Int>, PaymentRepository<EventRegistration> {
    @Query("from EventRegistration where subscribable = :event and paid")
    fun getPaidRegistrationsByEvent(event: Event): List<EventRegistration>
    @Query("from EventRegistration where subscribable = :event")
    fun getRegistrationsByEvent(event: Event): List<EventRegistration>
}