package be.sgl.backend.entity.membership

import be.sgl.backend.entity.Auditable
import be.sgl.backend.util.belgian
import jakarta.persistence.*
import java.time.LocalDate

@Entity
class MembershipPeriod : Auditable() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int? = null
    lateinit var start: LocalDate
    lateinit var end: LocalDate
    var price: Double = 0.0
    var registrationLimit: Int? = null
    var reductionFactor: Double = 3.0
    var siblingReduction: Double = 0.0
    @OneToMany(mappedBy = "period", fetch = FetchType.EAGER, cascade = [CascadeType.ALL])
    var restrictions = mutableListOf<MembershipRestriction>()

    override fun toString() = "${start.belgian()} - ${end.belgian()}"
}