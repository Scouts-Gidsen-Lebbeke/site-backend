package be.sgl.backend.service.activity

import be.sgl.backend.dto.ActivityBaseDTO
import be.sgl.backend.dto.ActivityDTO
import be.sgl.backend.dto.ActivityRegistrationDTO
import be.sgl.backend.entity.registrable.RegistrableStatus.*
import be.sgl.backend.entity.registrable.RegistrableStatus.Companion.getStatus
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
import java.time.LocalDateTime

@Service
class ActivityService {

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

    fun getAllActivities(): List<ActivityBaseDTO> {
        return activityRepository.findAll().map(activityMapper::toBaseDto)
    }

    fun getVisibleActivities(): List<ActivityBaseDTO> {
        return activityRepository.findAllByEndAfterOrderByStart(LocalDateTime.now()).map(activityMapper::toBaseDto)
    }

    fun getActivityDTOById(id: Int): ActivityDTO? {
        return activityMapper.toDto(getActivityById(id))
    }

    fun saveActivityDTO(dto: ActivityDTO): ActivityDTO {
        validateActivityDTO(dto)
        check(LocalDateTime.now() < dto.closed) { "New activities should still have the possibility to register!" }
        val newActivity = activityMapper.toEntity(dto)
        for (restriction in newActivity.restrictions) {
            restriction.activity = newActivity
        }
        return activityMapper.toDto(activityRepository.save(newActivity))
    }

    private fun validateActivityDTO(dto: ActivityDTO) {
        check(dto.open < dto.closed) { "The closure of registrations should be after the opening of registrations!" }
        check(dto.closed < dto.start) { "The start date of an activity should be after the closure of registrations!" }
        check(dto.start < dto.end) { "The start date of an activity should be before its end date!" }
    }

    fun mergeActivityDTOChanges(id: Int, dto: ActivityDTO): ActivityDTO {
        validateActivityDTO(dto)
        val activity = getActivityById(id)
        // update this first, maybe the status alters
        activity.closed = dto.closed
        check(activity.getStatus() != CANCELLED) { "A cancelled activity cannot be edited anymore!" }
        check(activity.getStatus() != STARTED) { "A started activity cannot be edited anymore!" }
        check(activity.getStatus() != COMPLETED) { "A completed activity cannot be edited anymore!" }
        check(activity.getStatus() != REGISTRATIONS_COMPLETED) { "An activity with closed registrations cannot be edited anymore!" }
        if (activity.getStatus() == NOT_YET_OPEN) {
            // price and user data collection can only be altered is no registration was possible yet
            activity.reductionFactor = dto.reductionFactor
            activity.siblingReduction = dto.siblingReduction
            activity.price = dto.price
            activity.additionalForm = dto.additionalForm
            activity.additionalFormRule = dto.additionalFormRule
            check(dto.cancellable || !activity.cancellable) { "A previously cancellable activity cannot be made uncancellable!" }
            activity.cancellable = dto.cancellable
            // Core activity data that is used in certificates, should never be changed when registrations opened
            activity.name = dto.name
            activity.start = dto.start
            activity.end = dto.end
            // One can only delay or advance the registration period when it wasn't open yet
            activity.open = dto.open
            restrictionRepository.deleteAll(activity.restrictions)
            activity.restrictions = dto.restrictions.map(activityMapper::toEntity).toMutableList()
            activity.validateRestrictions()
        } else {
            val updatedRestrictions = dto.restrictions.map(activityMapper::toEntity).toMutableList()
            for (existing in activity.restrictions) {
                val updated = updatedRestrictions.find { it.id == existing.id }
                check(updated != null) { "Existing activity restrictions cannot be removed once the activity has started!" }
                check(updated.alternativePrice == existing.alternativePrice) { "The price cannot be altered once the activity has started!" }
            }
            activity.restrictions.addAll(restrictionRepository.saveAll(updatedRestrictions))
            val registrationCount = registrationRepository.getBySubscribable(activity).count()
            check(dto.registrationLimit == null || registrationCount < dto.registrationLimit!!) { "The registration limit cannot be lowered below the current registration count!" }
        }
        activity.registrationLimit = dto.registrationLimit
        //activity.address = activityMapper.toEntity(dto.address)
        activity.sendConfirmation = dto.sendConfirmation
        activity.sendCompleteConfirmation = dto.sendCompleteConfirmation
        activity.communicationCC = dto.communicationCC
        activity.description = dto.description
        return activityMapper.toDto(activityRepository.save(activity))
    }

    private fun hasRegistrations(activity: Activity): Boolean {
        return registrationRepository.existsBySubscribable(activity)
    }

    fun deleteActivity(id: Int) {
        val activity = getActivityById(id)
        check(hasRegistrations(activity)) { "This activity has registrations and cannot be deleted anymore!" }
        registrationRepository.deleteById(id)
    }

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
        //val checkoutUrl = checkoutProvider.createCheckoutUrl(user, registration)
        registrationRepository.save(registration)
        //return checkoutUrl
        return ""
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
}