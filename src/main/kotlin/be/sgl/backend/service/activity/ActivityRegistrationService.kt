package be.sgl.backend.service.activity

import be.sgl.backend.dto.ActivityRegistrationDTO
import be.sgl.backend.entity.registrable.activity.Activity
import be.sgl.backend.entity.registrable.activity.ActivityRegistration
import be.sgl.backend.entity.registrable.activity.ActivityRestriction
import be.sgl.backend.entity.user.User
import be.sgl.backend.repository.ActivityRegistrationRepository
import be.sgl.backend.repository.ActivityRepository
import be.sgl.backend.repository.ActivityRestrictionRepository
import be.sgl.backend.service.exception.ActivityNotFoundException
import be.sgl.backend.service.exception.RestrictionNotFoundException
import be.sgl.backend.service.mapper.ActivityMapper
import be.sgl.backend.service.payment.CheckoutProvider
import be.sgl.backend.service.user.UserDataProvider
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class ActivityRegistrationService {

    @Autowired
    private lateinit var userDataProvider: UserDataProvider
    @Autowired
    private lateinit var activityRepository: ActivityRepository
    @Autowired
    private lateinit var registrationRepository: ActivityRegistrationRepository
    @Autowired
    private lateinit var restrictionRepository: ActivityRestrictionRepository
    @Autowired
    private lateinit var activityMapper: ActivityMapper
    @Autowired
    private lateinit var checkoutProvider: CheckoutProvider


    fun getAllRegistrationsForActivity(id: Int): List<ActivityRegistrationDTO> {
        val activity = getActivityById(id)
        return registrationRepository.getBySubscribable(activity).map(activityMapper::toDto)
    }

    fun getAllRegistrationsForUser(username: String): List<ActivityRegistrationDTO> {
        val user = userDataProvider.getUser(username)
        return registrationRepository.getByUser(user).map(activityMapper::toDto)
    }

    fun createPaymentForActivity(id: Int, restrictionId: Int, username: String, additionalData: String?): String {
        val user = userDataProvider.getUser(username)
        val activity = getActivityById(id)
        val restriction = getActivityRestrictionById(restrictionId)
        validateActivityLimits(restriction)
        val finalPrice = calculatePriceForActivity(user, activity, restriction, additionalData)
        var registration = ActivityRegistration(user, restriction, finalPrice, additionalData)
        registration = registrationRepository.save(registration)
        val checkoutUrl = checkoutProvider.createCheckoutUrl(user, registration, "activity")
        registrationRepository.save(registration)
        return checkoutUrl
    }

    /**
     * Check the presence of a global activity limit, a restriction limit and a branch limit.
     */
    private fun validateActivityLimits(restriction: ActivityRestriction) {
        restriction.activity.registrationLimit?.let {
            val registrationCount = registrationRepository.getBySubscribable(restriction.activity).count()
            check(registrationCount < it) { "The limit for this activity is reached!" }
        }
        restriction.alternativeLimit?.let {
            val restrictionCount = registrationRepository.getByRestriction(restriction).count()
            check(restrictionCount < it) { "The limit for this restriction is reached!" }
        }
        restrictionRepository.findAllByBranch(restriction.branch).find { it.isBranchLimit() }?.let {
            val branchCount = registrationRepository.getByBranch(restriction.branch).count()
            check(branchCount < it.alternativeLimit!!) { "The limit for this branch is reached!" }
        }
    }

    private fun calculatePriceForActivity(user: User, activity: Activity, restriction: ActivityRestriction, additionalData: String?): Double {
        var finalPrice = restriction.alternativePrice ?: activity.price
        val additionalPrice = activity.readAdditionalData(additionalData)
        if (user.userData.hasReduction) {
            return finalPrice / activity.reductionFactor + additionalPrice
        }
        finalPrice += additionalPrice
        if (user.siblings.any { !it.userData.hasReduction && registrationRepository.existsBySubscribableAndUser(activity, it) }) {
            return (finalPrice - activity.siblingReduction).coerceAtLeast(0.0)
        }
        return finalPrice
    }

    private fun getActivityById(id: Int): Activity {
        return activityRepository.findById(id).orElseThrow { ActivityNotFoundException() }
    }

    private fun getActivityRestrictionById(id: Int): ActivityRestriction {
        return restrictionRepository.findById(id).orElseThrow { RestrictionNotFoundException() }
    }

    fun updatePayment(paymentId: String) {
        TODO("Not yet implemented")
    }
}