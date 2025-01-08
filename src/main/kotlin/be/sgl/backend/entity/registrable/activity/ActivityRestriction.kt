package be.sgl.backend.entity.registrable.activity

import be.sgl.backend.entity.Auditable
import be.sgl.backend.entity.branch.Branch
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.ManyToOne
import java.time.LocalDateTime

/**
 * An [ActivityRestriction] is a link between a [Branch] and an [Activity],
 * stating that users having an active membership in the given [branch] can apply for the given [activity].
 * This access to the activity can be accompanied by a deviant activity base price, start date, end date or limit.
 * Multiple restrictions can apply to a single branch, representing different registration options,
 * but then a [name] should be present to distinguish between the options (except for branch limits).
 */
@Entity
class ActivityRestriction : Auditable() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int? = null
    @ManyToOne
    lateinit var activity: Activity
    @ManyToOne
    lateinit var branch: Branch
    var name: String? = null

    /**
     * Custom start date for the activity for this branch.
     * Should always be after the global closed date and before the end date (or optionally [alternativeEnd]).
     */
    var alternativeStart: LocalDateTime? = null

    /**
     * Custom end date for the activity for this branch.
     * Should always be after the activity's begin date (or optionally [alternativeStart]).
     */
    var alternativeEnd: LocalDateTime? = null

    /**
     * Custom, greater than zero price for this activity. Can be more or less than the original price.
     */
    var alternativePrice: Double? = null

    /**
     * Limit for this specific branch registration option.
     * If the main limit is defined, this specific limit should be less, else it is ignored.
     *
     * If multiple branch options exist, an unnamed branch restriction safeguards the global branch limit.
     * For the other named options, the alternative limit is then discarded if their total exceeds the branch limit.
     */
    var alternativeLimit: Int? = null

    fun isBranchLimit(): Boolean {
        return alternativePrice == null && alternativeStart == null && alternativeEnd == null && name == null
    }
}