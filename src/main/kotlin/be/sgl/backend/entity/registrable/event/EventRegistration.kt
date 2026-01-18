package be.sgl.backend.entity.registrable.event

import be.sgl.backend.entity.registrable.Registration
import be.sgl.backend.entity.user.User
import jakarta.persistence.Entity
import jakarta.persistence.ManyToOne

@Entity
class EventRegistration : Registration<Event>() {
    @ManyToOne
    var user: User? = null
    lateinit var name: String
    lateinit var firstName: String
    lateinit var email: String
    var mobile: String? = null
}