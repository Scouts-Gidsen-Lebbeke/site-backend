package be.sgl.backend.entity.membership

import be.sgl.backend.entity.Auditable
import be.sgl.backend.entity.branch.Branch
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.ManyToOne

@Entity
class MembershipRestriction : Auditable() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int? = null
    @ManyToOne
    lateinit var period: MembershipPeriod
    @ManyToOne
    lateinit var branch: Branch
    var alternativePrice: Double? = null
    var registrationLimit: Double? = null
}