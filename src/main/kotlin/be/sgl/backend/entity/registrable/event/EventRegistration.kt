package be.sgl.backend.entity.registrable.event

import be.sgl.backend.entity.registrable.Registration
import be.sgl.backend.people.domain.User
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.ManyToOne
import java.util.UUID

@Entity
class EventRegistration : Registration<Event>() {
    @ManyToOne
    var user: User? = null
    lateinit var name: String
    lateinit var firstName: String
    lateinit var email: String
    var mobile: String? = null
    @Column(unique = true, updatable = false, length = 36)
    var cancelToken: String? = UUID.randomUUID().toString()
}