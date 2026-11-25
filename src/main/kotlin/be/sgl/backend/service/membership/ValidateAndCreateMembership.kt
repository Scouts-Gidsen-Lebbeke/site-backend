package be.sgl.backend.service.membership

import be.sgl.backend.organization.domain.Branch
import be.sgl.backend.organization.domain.isSameAs
import be.sgl.backend.entity.membership.Membership
import be.sgl.backend.entity.membership.MembershipPeriod
import be.sgl.backend.entity.membership.MembershipRestriction
import be.sgl.backend.entity.user.User
import be.sgl.backend.organization.domain.BranchRepository
import be.sgl.backend.repository.membership.MembershipRepository
import be.sgl.backend.repository.user.SiblingRepository
import be.sgl.backend.shared.util.Usecase
import be.sgl.backend.shared.util.reducePrice
import mu.KotlinLogging
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters.lastDayOfYear

@Usecase
class ValidateAndCreateMembership(
    private val membershipRepository: MembershipRepository,
    private val branchRepository: BranchRepository,
    private val siblingRepository: SiblingRepository
) {

    private val logger = KotlinLogging.logger {}

    fun execute(period: MembershipPeriod, user: User, at: LocalDate = LocalDate.now()): Membership {
        logger.info { "Creating membership entity for $period and user ${user.username} at $at..." }
        period.registrationLimit?.let {
            val periodCount = membershipRepository.countByPeriod(period)
            check(periodCount < it) { "This period already has its maximum number of members!" }
        }
        val branch = determineCurrentBranchForUser(user, period)
        logger.info { "Applicable branch is $branch, checking membership limits for period..." }
        period.getLimitForBranch(branch)?.let {
            val branchCount = membershipRepository.countByPeriodAndBranch(period, branch)
            check(branchCount < it) { "No more free membership spots for this branch!" }
        }
        val price = calculatePrice(period, user, branch, at)
        return Membership(user, period, branch, price)
    }

    private fun calculatePrice(period: MembershipPeriod, user: User, branch: Branch, at: LocalDate): Double {
        logger.info { "Calculating price for a membership in $period for user ${user.username} at $at..." }
        val basePrice = calculateBasePriceForBranch(period, branch, at)
        if (user.hasReduction) {
            logger.info { "User is eligible for reduced tariff, dividing base price with reduction factor (${period.reductionFactor})" }
            return basePrice.reducePrice(period.reductionFactor)
        }
        getSiblingsWithoutReductionAndWithMembership(user, period)?.let { sibling ->
            logger.info { "User has already subscribed sibling ${sibling.username}, applying sibling reduction" }
            return (basePrice - period.siblingReduction).coerceAtLeast(0.0)
        }
        logger.info { "User has no additional reduction, returning base price." }
        return basePrice
    }

    private fun determineCurrentBranchForUser(user: User, period: MembershipPeriod): Branch {
        logger.info { "Determining applicable branch user ${user.username}..." }
        val age = user.getAge(period.end.with(lastDayOfYear())) + user.ageDeviation
        return branchRepository.getPossibleBranchesForSexAndAge(user.sex, age).firstOrNull()
            ?: throw IllegalStateException("No active branch can be linked to a user of this age and sex!")
    }

    /**
     * Applies the restrictions in their order of importance (see [MembershipRestriction]):
     * a started time-branch restriction, the branch restriction, a started general time restriction and finally the period price.
     */
    private fun calculateBasePriceForBranch(period: MembershipPeriod, branch: Branch, at: LocalDate): Double {
        logger.info { "Calculating price for a membership in $period for $branch at $at..." }
        val branchTimeRestrictions = period.restrictions.filter { it.branch.isSameAs(branch) && it.isTimeRestriction }
        val generalTimeRestrictions = period.restrictions.filter { it.branch == null && it.isTimeRestriction }
        findApplicableTimeRestrictionPrice(branchTimeRestrictions, at)?.let {
            logger.info { "Applicable branch time restriction found with price €$it" }
            return it
        }
        period.getBranchRestriction(branch)?.alternativePrice?.let {
            logger.info { "Applicable branch restriction has price €$it configured" }
            return it
        }
        findApplicableTimeRestrictionPrice(generalTimeRestrictions, at)?.let {
            logger.info { "Applicable time restriction found with price €$it" }
            return it
        }
        logger.info { "No applicable restriction with a price found, returning base price €${period.price} for period" }
        return period.price
    }

    private fun findApplicableTimeRestrictionPrice(restrictions: List<MembershipRestriction>, at: LocalDate): Double? {
        return restrictions.filter { it.alternativeStart?.isBefore(at) ?: false }
            .maxByOrNull { it.alternativeStart!! }?.alternativePrice
    }

    private fun getSiblingsWithoutReductionAndWithMembership(user: User, period: MembershipPeriod): User? {
        return siblingRepository.getByUser(user).map { it.sibling }.firstOrNull {
            !it.hasReduction && membershipRepository.existsByPeriodAndUser(period, it)
        }
    }
}