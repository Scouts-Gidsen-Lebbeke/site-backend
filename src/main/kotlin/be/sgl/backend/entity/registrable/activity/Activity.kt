package be.sgl.backend.entity.registrable.activity

import be.sgl.backend.organization.domain.Branch
import be.sgl.backend.organization.domain.isSameAs
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
    @OneToMany(mappedBy = "activity", fetch = FetchType.EAGER, cascade = [CascadeType.ALL])
    var restrictions = mutableListOf<ActivityRestriction>()

    fun getRestrictionsForBranches(branches: List<Branch>): List<ActivityRestriction> {
        return restrictions.filter { restriction -> branches.any { restriction.branch.isSameAs(it) } && !isBranchLimit(restriction) }
    }

    fun getBranchLimit(branch: Branch): Int? {
        return restrictions.find { it.branch.isSameAs(branch) && isBranchLimit(it) }?.alternativeLimit
    }

    /**
     * Only when a branch has multiple options, its single unnamed restriction serves as limit for the whole branch.
     * A single unnamed restriction is a regular registration option.
     */
    fun isBranchLimit(restriction: ActivityRestriction): Boolean {
        return restriction.name == null && restrictions.count { it.branch.isSameAs(restriction.branch) } > 1
    }

    fun validateRestrictions() {
        // branches from a request are distinct instances, so group them on id
        for (branchRestrictions in restrictions.groupBy { it.branch.id ?: it.branch }.values) {
            val branch = branchRestrictions.first().branch
            if (branchRestrictions.size == 1) continue
            if (branchRestrictions.size == 2) {
                check(branchRestrictions.all { it.name != null }) { "All restrictions should be named for branch $branch" }
                continue
            }
            val unnamed = branchRestrictions.filter { it.name == null }
            check(unnamed.size <= 1) { "At most one restriction (serving as branch limit) should be unnamed for branch $branch" }
            val branchLimit = unnamed.singleOrNull() ?: continue
            check(branchLimit.alternativeLimit != null) { "The single unnamed restriction (serving as branch limit) for branch $branch should have a limit" }
            check(branchLimit.alternativePrice == null) { "The single unnamed restriction (serving as branch limit) for branch $branch should not have a price" }
            check(branchLimit.alternativeStart == null) { "The single unnamed restriction (serving as branch limit) for branch $branch should not have a start date" }
            check(branchLimit.alternativeEnd == null) { "The single unnamed restriction (serving as branch limit) for branch $branch should not have an end date" }
        }
    }
}