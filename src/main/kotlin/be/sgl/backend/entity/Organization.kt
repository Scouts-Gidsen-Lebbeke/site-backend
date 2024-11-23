package be.sgl.backend.entity

import be.sgl.backend.entity.enum.OrganizationType
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id

@Entity
class Organization : Auditable() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int? = null
    lateinit var name: String
    var type = OrganizationType.OWNER
    var kbo: String? = null
    var description: String? = null
}