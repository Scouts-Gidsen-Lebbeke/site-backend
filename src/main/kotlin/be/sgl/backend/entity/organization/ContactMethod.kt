package be.sgl.backend.entity.organization

import be.sgl.backend.entity.Auditable
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.OneToOne
import jakarta.persistence.PrimaryKeyJoinColumn

@Entity
class ContactMethod : Auditable() {
    @Id
    val id: Int? = null
    @OneToOne
    @PrimaryKeyJoinColumn
    lateinit var organization: Organization
    var type = ContactMethodType.LINK
    lateinit var value: String
}