package be.sgl.backend.entity.membership

import be.sgl.backend.shared.persistence.Auditable
import be.sgl.backend.entity.branch.Branch
import be.sgl.backend.entity.branch.isSameAs
import be.sgl.backend.shared.util.belgian
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
    @OneToMany(mappedBy = "period", fetch = FetchType.EAGER, cascade = [CascadeType.ALL], orphanRemoval = true)
    var restrictions = mutableListOf<MembershipRestriction>()

    /**
     * The non-time restriction of the [branch], of which there is at most one.
     * Time-branch restrictions never carry a limit, so they must not hide this one.
     */
    fun getBranchRestriction(branch: Branch): MembershipRestriction? {
        return restrictions.find { it.branch.isSameAs(branch) && !it.isTimeRestriction }
    }

    fun getLimitForBranch(branch: Branch): Int? {
        return getBranchRestriction(branch)?.registrationLimit
    }

    override fun toString() = "${start.belgian()} - ${end.belgian()}"
}