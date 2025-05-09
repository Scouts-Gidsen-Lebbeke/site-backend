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
import be.sgl.backend.service.payment.CheckoutProvider
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class ActivityService {

    private val logger = KotlinLogging.logger {}

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
    @Autowired
    private lateinit var checkoutProvider: CheckoutProvider

    fun getAllActivities(): List<ActivityResultDTO> {
        logger.debug { "Fetching all activities" }
        return activityRepository.findAllRecentFirst()
            .map { ActivityResultDTO(it, registrationRepository.getPaidRegistrationPricesByActivity(it)) }
    }

    fun getVisibleActivities(): List<ActivityBaseDTO> {
        logger.debug { "Fetching all visible activities" }
        return activityRepository.findAllVisibleActivities().map(mapper::toBaseDto)
    }

    fun getActivityDTOById(id: Int): ActivityDTO {
        logger.debug { "Fetching activity #$id" }
        return mapper.toDto(getActivityById(id))
    }

    fun saveActivityDTO(dto: ActivityDTO): ActivityDTO {
        logger.info { "Saving new activity ${dto.name} (${dto.start} - ${dto.end})" }
        validateActivityDTO(dto)
        val newActivity = mapper.toEntity(dto)
        for (restriction in newActivity.restrictions) {
            restriction.activity = newActivity
        }
        newActivity.validateRestrictions()
        return mapper.toDto(activityRepository.save(newActivity))
    }

    fun mergeActivityDTOChanges(id: Int, dto: ActivityDTO): ActivityDTO {
        logger.info { "Updating activity #$id" }
        validateActivityDTO(dto)
        val activity = getActivityById(id)
        // If it was closed, it can be reopened again. The closing date is always before the start date anyway,
        // and the check hereafter enforces that the closing date is still in the future.
        activity.closed = dto.closed
        check(activity.getStatus() != CANCELLED) { "A cancelled activity cannot be edited anymore!" }
        check(activity.getStatus() != REGISTRATIONS_COMPLETED) { "An activity with closed registrations cannot be edited anymore!" }
        check(activity.getStatus() != STARTED) { "A started activity cannot be edited anymore!" }
        check(activity.getStatus() != COMPLETED) { "A completed activity cannot be edited anymore!" }
        if (activity.getStatus() == NOT_YET_OPEN) {
            logger.info { "Activity registrations are not yet open, activity can be fully edited" }
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
            logger.info { "Activity registrations are already open, only certain restriction modifications are allowed" }
            val updatedRestrictions = dto.restrictions.map(mapper::toEntity).toMutableList()
            for (existing in activity.restrictions) {
                val updated = updatedRestrictions.find { it.id == existing.id }
                check(updated != null) { "Existing activity restrictions cannot be removed once the activity has started!" }
                check(updated.alternativePrice == existing.alternativePrice) { "The price cannot be altered once the activity has started!" }
            }
            activity.restrictions.addAll(restrictionRepository.saveAll(updatedRestrictions))
            val registrationCount = registrationRepository.countPaidRegistrationsByActivity(activity)
            check(dto.registrationLimit == null || registrationCount < dto.registrationLimit!!) { "The registration limit cannot be lowered below the current registration count!" }
        }
        activity.registrationLimit = dto.registrationLimit
        activity.address = dto.address?.let { addressMapper.toEntity(it) }
        activity.sendConfirmation = dto.sendConfirmation
        activity.sendCompleteConfirmation = dto.sendCompleteConfirmation
        activity.communicationCC = dto.communicationCC
        activity.description = dto.description
        return mapper.toDto(activityRepository.save(activity))
    }

    fun cancelActivity(id: Int) {
        logger.info { "Cancel activity #$id..." }
        val activity = getActivityById(id)
        check(activity.getStatus() != CANCELLED) { "This activity is already cancelled!" }
        check(activity.getStatus() != STARTED) { "A started activity cannot be cancelled anymore!" }
        check(activity.getStatus() != COMPLETED) { "A completed activity cannot be cancelled anymore!" }
        val registrations = registrationRepository.getRegistrationsByActivity(activity)
        if (registrations.isNotEmpty()) {
            logger.info { "Activity has ${registrations.size} linked registrations needing a refund..." }
            registrations.forEach {
                checkoutProvider.refundPayment(it)
                logger.info { "Refund request sent for registration #${it.id}" }
            }
        }
        logger.info { "Registrations fully checked, marking activity as cancelled..." }
        activity.cancelled = true
        activityRepository.save(activity)
        logger.info { "Activity successfully cancelled" }
    }

    private fun validateActivityDTO(dto: ActivityDTO) {
        logger.debug { "Validating a correct open-closed-start-end sequence" }
        check(dto.open < dto.closed) { "The closure of registrations should be after the opening of registrations!" }
        check(dto.closed < dto.start) { "The start date of an activity should be after the closure of registrations!" }
        check(dto.start < dto.end) { "The start date of an activity should be before its end date!" }
    }

    private fun getActivityById(id: Int): Activity {
        return activityRepository.findById(id).orElseThrow { ActivityNotFoundException() }
    }
}