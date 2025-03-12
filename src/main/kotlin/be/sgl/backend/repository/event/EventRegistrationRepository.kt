package be.sgl.backend.repository.event

import be.sgl.backend.entity.registrable.event.Event
import be.sgl.backend.entity.registrable.event.EventRegistration
import be.sgl.backend.entity.user.User
import be.sgl.backend.repository.PaymentRepository
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface EventRegistrationRepository : JpaRepository<EventRegistration, Int>, PaymentRepository<EventRegistration> {
    fun getBySubscribable(subscribable: Event): List<EventRegistration>
    fun existsBySubscribable(subscribable: Event): Boolean
    fun getByUser(user: User): List<EventRegistration>
}