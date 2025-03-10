package be.sgl.backend.entity.organization

import be.sgl.backend.entity.Auditable
import jakarta.persistence.*

@Entity
class ContactMethod() : Auditable() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int? = null
    @ManyToOne
    lateinit var organization: Organization
    @Enumerated(EnumType.STRING)
    var type = ContactMethodType.LINK
    lateinit var value: String

    constructor(organization: Organization, type: ContactMethodType, value: String) : this() {
        this.organization = organization
        this.type = type
        this.value = value
    }
}