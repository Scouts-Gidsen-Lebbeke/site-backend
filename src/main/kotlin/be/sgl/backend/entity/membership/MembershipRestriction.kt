package be.sgl.backend.entity.membership

import be.sgl.backend.entity.Auditable
import be.sgl.backend.entity.branch.Branch
import jakarta.persistence.*
import java.time.LocalDate

/**
 * Membership restrictions can come in three forms:
 *  * A time restriction: no branch nor limit is linked, it is valid only when the current date exceeds its alternative start date
 *  * A branch restriction: no time is linked, a limit can apply for this specific branch
 *  * A time branch restriction: time restriction for a specific branch, without a limit
 * The order of importance is time < branch < time branch.
 */
@Entity
class MembershipRestriction : Auditable() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int? = null
    @ManyToOne
    lateinit var period: MembershipPeriod
    @ManyToOne
    var branch: Branch? = null
    var alternativeStart: LocalDate? = null
    var alternativePrice: Double? = null
    var registrationLimit: Int? = null

    val isTimeRestriction: Boolean
        get() = alternativeStart != null

    fun validate() {
        if (isTimeRestriction) {
            check(alternativeStart!! in period.start..period.end) { "The start date of a time related restriction must be in its period boundaries!" }
            check(registrationLimit == null) { "A time related restriction can't have a registration limit!" }
            checkNotNull(alternativePrice) { "A time related restriction should have an alternative price!" }
        } else {
            checkNotNull(branch) { "If a restriction is not time related, a linked branch is required!" }
            check(alternativePrice != null || registrationLimit != null) { "If a restriction is not time related, it should have either an alternative price or a limit to be useful!" }
        }
    }
}