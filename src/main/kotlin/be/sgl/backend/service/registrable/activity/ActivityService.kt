package be.sgl.backend.service.registrable.activity

import be.sgl.backend.dto.registrable.activity.ActivityBaseDTO
import be.sgl.backend.dto.registrable.activity.ActivityDTO
import be.sgl.backend.dto.registrable.activity.ActivityResult
import be.sgl.backend.dto.registrable.activity.CreateOrUpdateActivityRequest
import be.sgl.backend.entity.registrable.RegistrableStatus.*
import be.sgl.backend.entity.registrable.RegistrableStatus.Companion.getStatus
import be.sgl.backend.entity.registrable.activity.Activity
import be.sgl.backend.repository.activity.ActivityRegistrationRepository
import be.sgl.backend.repository.activity.ActivityRepository
import be.sgl.backend.repository.activity.ActivityRestrictionRepository
import be.sgl.backend.exception.ActivityNotFoundException
import be.sgl.backend.mapper.registrable.activity.ActivityMapper
import be.sgl.backend.service.payment.CheckoutProvider
import mu.KotlinLogging
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class ActivityService(
    private val activityRepository: ActivityRepository,
    private val registrationRepository: ActivityRegistrationRepository,
    private val restrictionRepository: ActivityRestrictionRepository,
    private val mapper: ActivityMapper,
    private val checkoutProvider: CheckoutProvider
) {

    private val logger = KotlinLogging.logger {}

    fun getAllActivities(): List<ActivityResult> {
        logger.debug { "Fetching all activities" }
        return activityRepository.findAllRecentFirst()
            .map { ActivityResult.of(it, registrationRepository.getPaidRegistrationPricesByActivity(it)) }
    }

    fun getVisibleActivities(): List<ActivityBaseDTO> {
        logger.debug { "Fetching all visible activities" }
        return activityRepository.findAllVisibleActivities().map(mapper::toBaseDto)
    }

    fun getActivityDTOById(id: Int): ActivityDTO {
        logger.debug { "Fetching activity #$id" }
        return mapper.toDto(getActivityById(id))
    }

    fun createActivity(request: CreateOrUpdateActivityRequest): ActivityDTO {
        logger.info { "Saving new activity ${request.name} (${request.start} - ${request.end})" }
        check(LocalDateTime.now() < request.closed) { "New activities cannot be closed for registrations yet!" }
        validateActivityRequest(request)
        val newActivity = mapper.toEntity(request)
        for (restriction in newActivity.restrictions) {
            restriction.activity = newActivity
        }
        newActivity.validateRestrictions()
        return mapper.toDto(activityRepository.save(newActivity))
    }

    fun updateActivity(id: Int, request: CreateOrUpdateActivityRequest): ActivityDTO {
        logger.info { "Updating activity #$id" }
        validateActivityRequest(request)
        val activityToUpdate = getActivityById(id)
        val activityFromDto = mapper.toEntity(request)
        // If it was closed, it can be reopened again. The closing date is always before the start date anyway,
        // and the check hereafter enforces that the closing date is still in the future.
        activityToUpdate.closed = activityFromDto.closed
        check(activityToUpdate.getStatus() != CANCELLED) { "A cancelled activity cannot be edited anymore!" }
        check(activityToUpdate.getStatus() != REGISTRATIONS_COMPLETED) { "An activity with closed registrations cannot be edited anymore!" }
        check(activityToUpdate.getStatus() != STARTED) { "A started activity cannot be edited anymore!" }
        check(activityToUpdate.getStatus() != COMPLETED) { "A completed activity cannot be edited anymore!" }
        if (activityToUpdate.getStatus() == NOT_YET_OPEN) {
            logger.info { "Activity registrations are not yet open, activity can be fully edited" }
            // price and user data collection can only be altered if no registration was possible yet
            activityToUpdate.reductionFactor = activityFromDto.reductionFactor
            activityToUpdate.siblingReduction = activityFromDto.siblingReduction
            activityToUpdate.price = activityFromDto.price
            activityToUpdate.additionalForm = activityFromDto.additionalForm
            activityToUpdate.additionalFormRule = activityFromDto.additionalFormRule
            check(activityFromDto.cancellable || !activityToUpdate.cancellable) { "A previously cancellable activity cannot be made uncancellable!" }
            activityToUpdate.cancellable = activityFromDto.cancellable
            // Core activity data that is used in certificates, should never be changed when registrations opened
            activityToUpdate.name = activityFromDto.name
            activityToUpdate.start = activityFromDto.start
            activityToUpdate.end = activityFromDto.end
            // One can only delay or advance the registration period when it wasn't open yet
            activityToUpdate.open = activityFromDto.open
            restrictionRepository.deleteAll(activityToUpdate.restrictions)
            activityToUpdate.restrictions = activityFromDto.restrictions
            activityToUpdate.validateRestrictions()
        } else {
            logger.info { "Activity registrations are already open, only certain restriction modifications are allowed" }
            val updatedRestrictions = activityFromDto.restrictions
            for (existing in activityToUpdate.restrictions) {
                val updated = updatedRestrictions.find { it.id == existing.id }
                check(updated != null) { "Existing activity restrictions cannot be removed once the activity has started!" }
                check(updated.alternativePrice == existing.alternativePrice) { "The price cannot be altered once the activity has started!" }
            }
            activityToUpdate.restrictions.addAll(restrictionRepository.saveAll(updatedRestrictions))
            val registrationCount = registrationRepository.countPaidRegistrationsByActivity(activityToUpdate)
            check(activityFromDto.registrationLimit == null || registrationCount < activityFromDto.registrationLimit!!) { "The registration limit cannot be lowered below the current registration count!" }
        }
        activityToUpdate.registrationLimit = activityFromDto.registrationLimit
        activityToUpdate.address = activityFromDto.address
        activityToUpdate.sendConfirmation = activityFromDto.sendConfirmation
        activityToUpdate.sendCompleteConfirmation = activityFromDto.sendCompleteConfirmation
        activityToUpdate.communicationCC = activityFromDto.communicationCC
        activityToUpdate.description = activityFromDto.description
        return mapper.toDto(activityRepository.save(activityToUpdate))
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

    private fun validateActivityRequest(request: CreateOrUpdateActivityRequest) {
        logger.debug { "Validating a correct open-closed-start-end sequence" }
        check(request.open < request.closed) { "The closure of registrations should be after the opening of registrations!" }
        check(request.closed < request.start) { "The start date of an activity should be after the closure of registrations!" }
        check(request.start < request.end) { "The start date of an activity should be before its end date!" }
    }

    private fun getActivityById(id: Int): Activity {
        return activityRepository.findById(id).orElseThrow { ActivityNotFoundException() }
    }
}