package be.sgl.backend.service.membership

import be.sgl.backend.entity.branch.Branch
import be.sgl.backend.entity.membership.Membership
import be.sgl.backend.entity.membership.MembershipPeriod
import be.sgl.backend.entity.membership.MembershipRestriction
import be.sgl.backend.entity.user.User
import be.sgl.backend.repository.BranchRepository
import be.sgl.backend.repository.membership.MembershipRepository
import be.sgl.backend.repository.user.SiblingRepository
import be.sgl.backend.util.Usecase
import be.sgl.backend.util.reducePrice
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters.lastDayOfYear

@Usecase
class ValidateAndCreateMembership {

    private val logger = KotlinLogging.logger {}

    @Autowired
    private lateinit var membershipRepository: MembershipRepository
    @Autowired
    private lateinit var branchRepository: BranchRepository
    @Autowired
    private lateinit var siblingRepository: SiblingRepository

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

    private fun calculateBasePriceForBranch(period: MembershipPeriod, branch: Branch, at: LocalDate): Double {
        logger.info { "Calculating price for a membership in $period for $branch at $at..." }
        val branchRestrictions = period.restrictions.filter { it.branch == branch }
        if (branchRestrictions.isEmpty()) {
            logger.info { "No restrictions in period applicable to $branch specifically, looking at time restrictions..." }
            findApplicableTimeRestrictionPrice(period.restrictions, at)?.let {
                logger.info { "Applicable time restriction found with price €${it}" }
                return it
            }
            logger.info { "No applicable time restriction found, returning base price €${period.price} for period" }
            return period.price
        }
        logger.info { "Found ${branchRestrictions.size} restrictions in period applicable to $branch" }
        findApplicableTimeRestrictionPrice(branchRestrictions, at)?.let {
            logger.info { "Applicable branch time restriction found with price €${it}" }
            return it
        }
        logger.info { "No applicable branch time restriction found, looking at branch restriction price" }
        branchRestrictions.filterNot { it.isTimeRestriction }.first().alternativePrice?.let {
            logger.info { "Applicable branch restriction has price €${it} configured" }
            return it
        }
        logger.info { "Applicable branch restriction has no price configured, returning base price €${period.price} for period" }
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