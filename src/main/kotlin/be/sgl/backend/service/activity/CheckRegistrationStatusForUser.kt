package be.sgl.backend.service.activity

import be.sgl.backend.dto.ActivityRegistrationStatus
import be.sgl.backend.entity.branch.Branch
import be.sgl.backend.entity.registrable.activity.Activity
import be.sgl.backend.entity.registrable.activity.ActivityRestriction
import be.sgl.backend.entity.user.User
import be.sgl.backend.repository.activity.ActivityRegistrationRepository
import be.sgl.backend.service.user.UserDataProvider
import be.sgl.backend.util.reducePrice
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class CheckRegistrationStatusForUser {

    private val logger = KotlinLogging.logger {}

    @Autowired
    lateinit var registrationRepository: ActivityRegistrationRepository
    @Autowired
    private lateinit var userDataProvider: UserDataProvider
    @Autowired
    private lateinit var getCurrentValidBranchesForUser: GetCurrentValidBranchesForUser

    fun execute(activity: Activity, user: User): ActivityRegistrationStatus {
        logger.info { "Calculating registration status for activity #${activity.id} and user ${user.username}" }
        registrationRepository.getByUserAndSubscribable(user, activity)?.let {
            logger.info { "User already registered (#${it.id})" }
            return ActivityRegistrationStatus(currentRegistration = it)
        }
        val relevantBranches = getCurrentValidBranchesForUser.execute(user)
        if (relevantBranches.isEmpty()) {
            logger.info { "No active branch found for ${user.username}" }
            return ActivityRegistrationStatus(activeMembership = false)
        }
        val relevantRestrictions = activity.getRestrictionsForBranches(relevantBranches)
        if (relevantRestrictions.isEmpty()) {
            logger.info { "No applicable restrictions found for ${user.username}" }
            return ActivityRegistrationStatus()
        }
        if (isGlobalLimitReached(activity)) {
            logger.info { "Global activity limit (${activity.registrationLimit}) is reached for ${activity.name}" }
            return ActivityRegistrationStatus(closedOptions = relevantRestrictions)
        }
        val restrictionsWithoutBranchLimit = relevantRestrictions.filter { !isBranchLimitReached(activity, it.branch) }
        if (restrictionsWithoutBranchLimit.isEmpty()) {
            logger.info { "Branch limit is reached for all active branches of ${user.username}" }
            return ActivityRegistrationStatus(closedOptions = relevantRestrictions)
        }
        val (closed, open) = restrictionsWithoutBranchLimit.partition(::isRestrictionLimitReached)
        if (open.isEmpty()) {
            logger.info { "Limit is reached for each applicable restriction for ${user.username}" }
            return ActivityRegistrationStatus(closedOptions = closed)
        }
        if (user.hasReduction) {
            logger.info { "User is eligible for reduced tariff, altering open option prices..." }
            open.onEach(::reducePrice)
        }
        logger.info { "User has ${open.size} open activity options" }
        val medicalRecord = userDataProvider.getMedicalRecord(user)
        if (medicalRecord == null) {
            logger.info { "Medical record not found for ${user.username}" }
        } else if (!medicalRecord.isUpToDate) {
            logger.info { "Medical record for ${user.username} is older than one year" }
        }
        return ActivityRegistrationStatus(
            openOptions = open,
            closedOptions = closed,
            medicsDate = medicalRecord?.lastModifiedDate,
            medicalsUpToDate = medicalRecord?.isUpToDate ?: false
        )
    }

    private fun isGlobalLimitReached(activity: Activity): Boolean {
        val globalLimit = activity.registrationLimit ?: return false
        return registrationRepository.countPaidRegistrationsByActivity(activity) >= globalLimit
    }

    private fun isRestrictionLimitReached(restriction: ActivityRestriction): Boolean {
        val restrictionLimit = restriction.alternativeLimit ?: return false
        return registrationRepository.countByRestriction_Id(restriction.id!!) >= restrictionLimit
    }

    private fun isBranchLimitReached(activity: Activity, branch: Branch): Boolean {
        val branchLimit = activity.getBranchLimit(branch) ?: return false
        return registrationRepository.countByActivityAndBranch(activity, branch) >= branchLimit
    }

    private fun reducePrice(restriction: ActivityRestriction) = ActivityRestriction().apply {
        activity = restriction.activity
        branch = restriction.branch
        name = restriction.name
        alternativeStart = restriction.alternativeStart
        alternativeEnd = restriction.alternativeEnd
        alternativePrice = (restriction.alternativePrice ?: activity.price).reducePrice(activity.reductionFactor)
        alternativeLimit = restriction.alternativeLimit
    }
}