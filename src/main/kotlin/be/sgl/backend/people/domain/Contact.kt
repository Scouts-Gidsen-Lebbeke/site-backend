package be.sgl.backend.people.domain

import be.sgl.backend.shared.address.Address
import be.sgl.backend.shared.persistence.Auditable
import jakarta.persistence.*

@Entity
class Contact : Auditable() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Int? = null
    @ManyToOne
    lateinit var user: User
    var externalId: String? = null
    @Column(nullable = false)
    lateinit var name: String
    @Column(nullable = false)
    lateinit var firstName: String
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    lateinit var role: ContactRole
    var mobile: String? = null
    var email: String? = null
    var nis: String? = null
    @ManyToOne
    var address: Address? = null
}