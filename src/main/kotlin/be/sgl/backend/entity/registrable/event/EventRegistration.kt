package be.sgl.backend.entity.registrable.event

import be.sgl.backend.entity.registrable.Registration
import be.sgl.backend.entity.user.User
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
    /**
     * Unguessable secret that allows the (possibly anonymous) registrant to cancel this registration
     * via a link in an e-mail. Nullable because registrations created before this column existed have none.
     */
    @Column(unique = true, updatable = false, length = 36)
    var cancelToken: String? = UUID.randomUUID().toString()
}