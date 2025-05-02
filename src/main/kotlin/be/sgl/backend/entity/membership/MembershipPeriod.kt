package be.sgl.backend.entity.membership

import be.sgl.backend.entity.Auditable
import be.sgl.backend.entity.branch.Branch
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

    fun getLimitForBranch(branch: Branch): Int? {
        return restrictions.find { it.branch == branch  }?.registrationLimit
    }

    fun validateRestrictions() {
        restrictions.onEach { it.validate() }.filter { it.branch != null }.groupBy { it.branch }.forEach { (_, restrictions) ->
            check(restrictions.filterNot { it.isTimeRestriction }.size <= 1) { "A branch should at most have one single non-time related restriction!" }
        }
    }

    override fun toString() = "${start.belgian()} - ${end.belgian()}"
}