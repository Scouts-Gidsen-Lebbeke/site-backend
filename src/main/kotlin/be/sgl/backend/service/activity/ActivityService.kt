package be.sgl.backend.service.activity

import be.sgl.backend.dto.ActivityDTO
import be.sgl.backend.dto.ActivityRegistrationDTO
import be.sgl.backend.entity.registrable.RegistrableStatus
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
import be.sgl.backend.service.user.UserDataProvider
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service


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

    fun getAllActivities(): List<ActivityDTO> {
        return activityRepository.findAll().map(activityMapper::toDto)
    }

    fun getVisibleActivities(): List<ActivityDTO> {
        TODO("Not yet implemented")
    }

    fun getActivityDTOById(id: Int): ActivityDTO? {
        return activityMapper.toDto(getActivityById(id))
    }

    fun saveActivityDTO(dto: ActivityDTO): ActivityDTO {
        validateActivityDTO(dto)
        val newActivity = activityMapper.toEntity(dto)
        for (restriction in newActivity.restrictions) {
            restriction.activity = newActivity
        }
        return activityMapper.toDto(activityRepository.save(newActivity))
    }

    private fun validateActivityDTO(dto: ActivityDTO) {
        check(dto.start < dto.end) { "The start date of an activity should be before its end date!" }
        check(dto.closed < dto.start) { "The start date of an activity should be after the closure of subscriptions!" }
    }

    fun mergeActivityDTOChanges(id: Int, dto: ActivityDTO): ActivityDTO {
        validateActivityDTO(dto)
        val activity = getActivityById(id)
        if (activity.getStatus() == RegistrableStatus.NOT_YET_OPEN) {
            activity.reductionFactor = dto.reductionFactor
            activity.siblingReduction = dto.siblingReduction
        }
        synchronizeRestrictions(activity, dto.restrictions.map(activityMapper::toEntity))
        activity.start = dto.start
        activity.end = dto.end
        activity.price = dto.price
        activity.limit = dto.limit
        activity.address = activityMapper.toEntity(dto.address)
        activity.additionalForm = dto.additionalForm
        activity.additionalFormRule = dto.additionalFormRule
        activity.cancellable = dto.cancellable
        activity.sendConfirmation = dto.sendConfirmation
        activity.sendCompleteConfirmation = dto.sendCompleteConfirmation
        activity.communicationCC = dto.communicationCC
        activity.name = dto.name
        activity.description = dto.description
        activity.open = dto.open
        activity.closed = dto.closed
        return activityMapper.toDto(activityRepository.save(activity))
    }

    private fun synchronizeRestrictions(existingActivity: Activity, updatedRestrictions: List<ActivityRestriction>) {
        // Remove restrictions that are no longer present
        existingActivity.restrictions.removeIf { existingRestriction ->
            updatedRestrictions.stream().noneMatch { updatedRestriction ->
                existingRestriction.id ==  updatedRestriction.id
            }
        }
        for (updatedRestriction in updatedRestrictions) {
            val existingRestrictionOpt = existingActivity.restrictions.firstOrNull { it.id == updatedRestriction.id }

            if (existingRestrictionOpt != null) {
                // Update existing restriction
                val existingRestriction: ActivityRestriction = existingRestrictionOpt.get()
                existingRestriction.setRestrictionType(updatedRestriction.getRestrictionType())
            } else {
                // Add new restriction
                updatedRestriction.activity = existingActivity
                existingActivity.restrictions.add(updatedRestriction)
            }
        }
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

    fun registerForActivity(id: Int, restrictionId: Int, username: String, additionalData: String?): ActivityRegistrationDTO {
        val user = userDataProvider.getUser(username)
        val activity = getActivityById(id)
        val restriction = getActivityRestrictionById(restrictionId)
        val finalPrice = calculatePriceForActivity(user, activity, restriction, additionalData)
        var registration = ActivityRegistration(user, restriction, finalPrice, additionalData)
        registration = registrationRepository.save(registration)
        return activityMapper.toDto(registration)
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