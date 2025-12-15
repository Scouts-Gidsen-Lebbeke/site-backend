package be.sgl.backend.service.registrable.activity

import be.sgl.backend.dto.registrable.activity.ActivityRegistrationDTO
import be.sgl.backend.dto.registrable.activity.ActivityRegistrationStatusDTO
import be.sgl.backend.dto.Customer
import be.sgl.backend.entity.registrable.RegistrableStatus
import be.sgl.backend.entity.registrable.RegistrableStatus.Companion.getStatus
import be.sgl.backend.entity.registrable.activity.Activity
import be.sgl.backend.entity.registrable.activity.ActivityRegistration
import be.sgl.backend.entity.registrable.activity.ActivityRestriction
import be.sgl.backend.mapper.registrable.activity.ActivityMapper
import be.sgl.backend.repository.activity.ActivityRegistrationRepository
import be.sgl.backend.repository.activity.ActivityRepository
import be.sgl.backend.repository.activity.ActivityRestrictionRepository
import be.sgl.backend.repository.user.UserRepository
import be.sgl.backend.service.payment.PaymentService
import be.sgl.backend.exception.ActivityNotFoundException
import be.sgl.backend.exception.ActivityRegistrationNotFoundException
import be.sgl.backend.exception.RestrictionNotFoundException
import be.sgl.backend.exception.UserNotFoundException
import mu.KotlinLogging
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class ActivityRegistrationService(
    override var paymentRepository: ActivityRegistrationRepository,
    private val userRepository: UserRepository,
    private val activityRepository: ActivityRepository,
    private val restrictionRepository: ActivityRestrictionRepository,
    private val mapper: ActivityMapper,
    private val checkRegistrationStatusForUser: CheckRegistrationStatusForUser,
    private val validateAndCreateActivityRegistration: ValidateAndCreateActivityRegistration,
    private val createCertificateForActivityRegistration: CreateCertificateForActivityRegistration
) : PaymentService<ActivityRegistration, ActivityRegistrationRepository>() {

    private val logger = KotlinLogging.logger {}

    fun getAllRegistrationsForActivity(id: Int): List<ActivityRegistrationDTO> {
        logger.info { "Fetching all registrations for activity #$id" }
        val activity = getActivityById(id)
        return paymentRepository.getPaidRegistrationsByActivity(activity).map(mapper::toDto)
    }

    fun getAllRegistrationsForUser(username: String): List<ActivityRegistrationDTO> {
        logger.info { "Fetching all registrations for user $username" }
        val user = userRepository.findByUsername(username) ?: throw UserNotFoundException(username)
        return paymentRepository.getByUser(user).map(mapper::toDto)
    }

    fun getActivityRegistrationDTOById(id: Int) : ActivityRegistrationDTO? {
        logger.info { "Fetching registration #$id" }
        return paymentRepository.findById(id).map(mapper::toDto).orElse(null)
    }

    fun getStatusForActivityAndUser(activityId: Int, username: String): ActivityRegistrationStatusDTO {
        logger.info { "Fetching status for activity $activityId and user $username" }
        val activity = getActivityById(activityId)
        val user = userRepository.findByUsername(username) ?: throw UserNotFoundException(username)
        return mapper.toDto(checkRegistrationStatusForUser.execute(activity, user))
    }

    fun createPaymentForActivity(id: Int, restrictionId: Int, username: String, additionalData: String?): String {
        val user = userRepository.findByUsername(username) ?: throw UserNotFoundException(username)
        val restriction = getActivityRestrictionById(restrictionId)
        var registration = validateAndCreateActivityRegistration.execute(restriction, user, additionalData)
        registration = paymentRepository.save(registration)
        logger.info { "Created registration #${registration.id}" }
        if (registration.price == 0.0) {
            logger.info { "Registration was free, marking it paid and returning redirect url immediately" }
            registration.markPaid()
            registration = paymentRepository.save(registration)
            handlePaymentPaid(registration)
            return checkoutProvider.createRedirectUrl(registration, "activities", restriction.activity.id)
        }
        logger.info { "Registration is not free, linking payment via payment provider" }
        val checkoutUrl = checkoutProvider.createCheckoutUrl(Customer(user), registration, "activities", restriction.activity.id)
        logger.info { "Registration linked to payment ${registration.paymentId}, saving reference" }
        paymentRepository.save(registration)
        logger.info { "Redirecting user to payment url $checkoutUrl" }
        return checkoutUrl
    }

    override fun handlePaymentPaid(payment: ActivityRegistration) {
        if (!payment.subscribable.sendConfirmation) return
        val params = mapOf(
            "member" to payment.user.firstName,
            "price" to payment.price,
            "activityName" to payment.subscribable.name,
            "branchName" to payment.restriction.branch.name,
            "restrictionName" to payment.restriction.name,
            "additionalData" to payment.getAdditionalDataMap()
        )
        val mailBuilder = mailService.builder()
            .to(payment.user.email)
            .subject("Bevestiging inschrijving")
            .template("activity-confirmation.html", params)
        payment.subscribable.communicationCC?.let { mailBuilder.cc(it) }
        mailBuilder.send()
    }

    override fun handlePaymentRefunded(payment: ActivityRegistration) {
        val params = mapOf(
            "member" to payment.user.firstName,
            "price" to payment.price - checkoutProvider.getRefundCost(payment),
            "activityName" to payment.subscribable.name,
        )
        val mailBuilder = mailService.builder()
            .to(payment.user.email)
            .subject("Annulatie inschrijving")
            .template("cancel-activity-confirmation.html", params)
        payment.subscribable.communicationCC?.let { mailBuilder.cc(it) }
        mailBuilder.send()
    }

    fun markRegistrationAsCompleted(id: Int) {
        logger.info { "Marking registration #$id as completed..." }
        val registration = getRegistrationById(id)
        check(registration.paid) { "Only a paid activity can be marked as completed!" }
        if (registration.completed) {
            logger.warn { "Registration is already marked as completed!" }
            return
        }
        check(registration.start.minusHours(1).isBefore(LocalDateTime.now())) { "Registrations can only be completed starting one hour before the activity!" }
        registration.completed = true
        paymentRepository.save(registration)
        if (registration.subscribable.sendCompleteConfirmation) {
            logger.info { "Linked activity requires completion confirmation, sending mail..." }
            val params = mapOf(
                "member" to registration.user.firstName,
                "activityName" to registration.subscribable.name
            )
            val mailBuilder = mailService.builder()
                .to(registration.user.email)
                .subject("Afwerking inschrijving")
                .template("activity-completion.html", params)
            registration.subscribable.communicationCC?.let { mailBuilder.cc(it) }
            mailBuilder.send()
        }
        logger.info { "Registration #$id successfully marked as completed" }
    }

    fun cancelRegistration(id: Int) {
        logger.info { "Cancelling activity registration #$id..." }
        val registration = getRegistrationById(id)
        check(registration.paid) { "Only a paid activity registration can be cancelled!" }
        check(registration.subscribable.getStatus() == RegistrableStatus.REGISTRATIONS_OPENED) { "Cancellation is only possible when registrations are still open!" }
        if (registration.price > 0) {
            checkoutProvider.refundPayment(registration)
            registration.markRefunded()
            paymentRepository.save(registration)
        } else {
            paymentRepository.delete(registration)
            handlePaymentRefunded(registration)
        }
        logger.info { "Activity registration #$id successfully cancelled" }
    }

    fun getCertificateForRegistration(id: Int): ByteArray {
        val registration = getRegistrationById(id)
        return createCertificateForActivityRegistration.execute(registration)
    }

    fun getPaymentForRegistration(id: Int): String {
        val registration = getRegistrationById(id)
        check(!registration.paid) { "This activity is already paid!" }
        return checkoutProvider.getCheckoutUrl(registration)
    }

    private fun getRegistrationById(id: Int): ActivityRegistration {
        return paymentRepository.findById(id).orElseThrow { ActivityRegistrationNotFoundException() }
    }

    private fun getActivityById(id: Int): Activity {
        return activityRepository.findById(id).orElseThrow { ActivityNotFoundException() }
    }

    private fun getActivityRestrictionById(id: Int): ActivityRestriction {
        return restrictionRepository.findById(id).orElseThrow { RestrictionNotFoundException() }
    }
}