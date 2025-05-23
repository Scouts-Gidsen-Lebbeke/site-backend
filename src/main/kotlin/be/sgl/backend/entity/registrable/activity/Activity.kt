package be.sgl.backend.entity.registrable.activity

import be.sgl.backend.entity.branch.Branch
import be.sgl.backend.entity.registrable.Registrable
import jakarta.persistence.*

/**
 * An [Activity] is a physical event for which only members linked to a configured [ActivityRestriction] can apply.
 */
@Entity
class Activity : Registrable() {

    /**
     * Reduction factor applied on the final price (excluding additional costs) when a member is entitled to reduction.
     */
    var reductionFactor: Double = 3.0

    /**
     * Reduction amount applied on the final price when a member has siblings that are already registered.
     * This reduction has no effect on members that are entitled to reduction.
     */
    var siblingReduction: Double = 0.0
    @OneToMany(mappedBy = "activity", fetch = FetchType.EAGER)
    var restrictions = mutableListOf<ActivityRestriction>()

    fun getRestrictionsForBranches(branches: List<Branch>): List<ActivityRestriction> {
        return restrictions.filter { it.branch in branches && !it.isBranchLimit() }
    }

    fun getBranchLimit(branch: Branch): Int? {
        return restrictions.find { it.branch == branch && it.isBranchLimit() }?.alternativeLimit
    }

    fun validateRestrictions() {
        for ((branch, branchRestrictions) in restrictions.groupBy { it.branch }) {
            if (branchRestrictions.size == 1) continue
            if (branchRestrictions.size == 2) {
                check(branchRestrictions.all { it.name != null }) { "All restrictions should be named for branch $branch" }
            }
            val unnamed = branchRestrictions.filter { it.name == null }
            check(unnamed.size == 1) { "At most one restriction (serving as branch limit) should be unnamed for branch $branch" }
            check(unnamed.first().alternativeLimit != null) { "The single unnamed restriction (serving as branch limit) for branch $branch should have a limit" }
            check(unnamed.first().alternativePrice == null) { "The single unnamed restriction (serving as branch limit) for branch $branch should not have a price" }
            check(unnamed.first().alternativeStart == null) { "The single unnamed restriction (serving as branch limit) for branch $branch should not have a start date" }
            check(unnamed.first().alternativeEnd == null) { "The single unnamed restriction (serving as branch limit) for branch $branch should not have an end date" }
        }
    }
}