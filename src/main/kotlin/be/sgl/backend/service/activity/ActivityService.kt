package be.sgl.backend.service.activity

import be.sgl.backend.dto.ActivityBaseDTO
import be.sgl.backend.dto.ActivityDTO
import be.sgl.backend.dto.ActivityResultDTO
import be.sgl.backend.entity.registrable.RegistrableStatus.*
import be.sgl.backend.entity.registrable.RegistrableStatus.Companion.getStatus
import be.sgl.backend.entity.registrable.activity.Activity
import be.sgl.backend.repository.activity.ActivityRegistrationRepository
import be.sgl.backend.repository.activity.ActivityRepository
import be.sgl.backend.repository.activity.ActivityRestrictionRepository
import be.sgl.backend.service.exception.ActivityNotFoundException
import be.sgl.backend.mapper.ActivityMapper
import be.sgl.backend.mapper.AddressMapper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class ActivityService {

    @Autowired
    private lateinit var activityRepository: ActivityRepository
    @Autowired
    private lateinit var registrationRepository: ActivityRegistrationRepository
    @Autowired
    private lateinit var restrictionRepository: ActivityRestrictionRepository
    @Autowired
    private lateinit var mapper: ActivityMapper
    @Autowired
    private lateinit var addressMapper: AddressMapper

    fun getAllActivities(): List<ActivityResultDTO> {
        return activityRepository.findAll().map { ActivityResultDTO(it, registrationRepository.getPaidRegistrationsByActivity(it)) }
    }

    fun getVisibleActivities(): List<ActivityBaseDTO> {
        return activityRepository.findAllByEndAfterOrderByStart(LocalDateTime.now()).map(mapper::toBaseDto)
    }

    fun getActivityDTOById(id: Int): ActivityDTO {
        return mapper.toDto(getActivityById(id))
    }

    fun saveActivityDTO(dto: ActivityDTO): ActivityDTO {
        validateActivityDTO(dto)
        check(LocalDateTime.now() < dto.closed) { "New activities should still have the possibility to register!" }
        val newActivity = mapper.toEntity(dto)
        for (restriction in newActivity.restrictions) {
            restriction.activity = newActivity
        }
        return mapper.toDto(activityRepository.save(newActivity))
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
            // price and user data collection can only be altered if no registration was possible yet
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
            activity.restrictions = dto.restrictions.map(mapper::toEntity).toMutableList()
            activity.validateRestrictions()
        } else {
            val updatedRestrictions = dto.restrictions.map(mapper::toEntity).toMutableList()
            for (existing in activity.restrictions) {
                val updated = updatedRestrictions.find { it.id == existing.id }
                check(updated != null) { "Existing activity restrictions cannot be removed once the activity has started!" }
                check(updated.alternativePrice == existing.alternativePrice) { "The price cannot be altered once the activity has started!" }
            }
            activity.restrictions.addAll(restrictionRepository.saveAll(updatedRestrictions))
            val registrationCount = registrationRepository.getPaidRegistrationsByActivity(activity).count()
            check(dto.registrationLimit == null || registrationCount < dto.registrationLimit!!) { "The registration limit cannot be lowered below the current registration count!" }
        }
        activity.registrationLimit = dto.registrationLimit
        activity.address = addressMapper.toEntity(dto.address)
        activity.sendConfirmation = dto.sendConfirmation
        activity.sendCompleteConfirmation = dto.sendCompleteConfirmation
        activity.communicationCC = dto.communicationCC
        activity.description = dto.description
        return mapper.toDto(activityRepository.save(activity))
    }

    fun deleteActivity(id: Int) {
        val activity = getActivityById(id)
        check(hasRegistrations(activity)) { "This activity has registrations and cannot be deleted anymore!" }
        activityRepository.deleteById(id)
    }

    private fun validateActivityDTO(dto: ActivityDTO) {
        check(dto.open < dto.closed) { "The closure of registrations should be after the opening of registrations!" }
        check(dto.closed < dto.start) { "The start date of an activity should be after the closure of registrations!" }
        check(dto.start < dto.end) { "The start date of an activity should be before its end date!" }
    }

    private fun hasRegistrations(activity: Activity): Boolean {
        return registrationRepository.existsBySubscribable(activity)
    }

    private fun getActivityById(id: Int): Activity {
        return activityRepository.findById(id).orElseThrow { ActivityNotFoundException() }
    }
}