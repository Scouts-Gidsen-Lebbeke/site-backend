package be.sgl.backend.entity.registrable.event

import be.sgl.backend.dto.EventRegistrationAttemptData
import be.sgl.backend.entity.registrable.Registration
import be.sgl.backend.entity.user.User
import jakarta.persistence.Entity
import jakarta.persistence.ManyToOne

@Entity
class EventRegistration() : Registration<Event>() {
    @ManyToOne
    var user: User? = null
    lateinit var name: String
    lateinit var firstName: String
    lateinit var email: String
    var mobile: String? = null

    constructor(attempt: EventRegistrationAttemptData, price: Double, user: User?) : this() {
        this.user = user
        this.name = attempt.name
        this.firstName = attempt.firstName
        this.email = attempt.email
        this.mobile = attempt.email
        this.price = price
        this.additionalData = attempt.additionalData
    }
}