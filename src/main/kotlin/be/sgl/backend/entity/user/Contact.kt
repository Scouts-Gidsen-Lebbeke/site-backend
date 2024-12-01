package be.sgl.backend.entity.user

import be.sgl.backend.entity.Address
import be.sgl.backend.entity.Auditable
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.ManyToOne

@Entity
class Contact : Auditable() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int? = null
    lateinit var name: String
    lateinit var firstName: String
    var role = ContactRole.RESPONSIBLE
    var mobile: String? = null
    var email: String? = null
    var nis: String? = null
    @ManyToOne
    var address: Address? = null
}