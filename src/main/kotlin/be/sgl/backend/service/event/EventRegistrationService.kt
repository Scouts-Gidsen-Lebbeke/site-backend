package be.sgl.backend.service.event

import be.sgl.backend.dto.Customer
import be.sgl.backend.dto.EventRegistrationAttemptData
import be.sgl.backend.dto.EventRegistrationDTO
import be.sgl.backend.entity.registrable.event.Event
import be.sgl.backend.entity.registrable.event.EventRegistration
import be.sgl.backend.mapper.EventMapper
import be.sgl.backend.repository.event.EventRegistrationRepository
import be.sgl.backend.repository.event.EventRepository
import be.sgl.backend.service.PaymentService
import be.sgl.backend.service.exception.EventNotFoundException
import be.sgl.backend.service.exception.EventRegistrationNotFoundException
import be.sgl.backend.service.user.UserDataProvider
import com.fasterxml.jackson.databind.ObjectMapper
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class EventRegistrationService : PaymentService<EventRegistration, EventRegistrationRepository>() {

    private val logger = KotlinLogging.logger {}

    @Autowired
    override lateinit var paymentRepository: EventRegistrationRepository
    @Autowired
    private lateinit var eventRepository: EventRepository
    @Autowired
    private lateinit var mapper: EventMapper
    @Autowired
    private lateinit var userDataProvider: UserDataProvider

    fun getAllRegistrationsForEvent(id: Int): List<EventRegistrationDTO> {
        val event = getEventById(id)
        return paymentRepository.getPaidRegistrationsByEvent(event).map(mapper::toDto)
    }

    fun getEventRegistrationDTOById(id: Int) : EventRegistrationDTO? {
        return paymentRepository.findById(id).map(mapper::toDto).orElse(null)
    }

    fun createPaymentForEvent(id: Int, attempt: EventRegistrationAttemptData, username: String?): String {
        val event = getEventById(id)
        check(!event.needsMobile || attempt.mobile != null) { "No valid mobile number provided!" }
        val user = username?.let { userDataProvider.findUser(it) }
        check(!isGlobalLimitReached(event)) { "The limit for this event is reached!" }
        val finalPrice = calculatePriceForEvent(event, attempt.additionalData)
        var registration = EventRegistration(event, attempt, finalPrice, user)
        registration = paymentRepository.save(registration)
        val customer = user?.let { Customer(it) } ?: Customer("${registration.firstName} ${registration.name}", registration.email)
        val checkoutUrl = checkoutProvider.createCheckoutUrl(customer, registration, "events", event.id)
        paymentRepository.save(registration)
        return checkoutUrl
    }

    private fun isGlobalLimitReached(event: Event): Boolean {
        val globalLimit = event.registrationLimit ?: return false
        return paymentRepository.getPaidRegistrationsByEvent(event).count() < globalLimit
    }

    private fun calculatePriceForEvent(event: Event, additionalData: String?): Double {
        return event.price + event.readAdditionalData(additionalData)
    }

    override fun handlePaymentPaid(payment: EventRegistration) {
        if (!payment.subscribable.sendConfirmation) return
        val params = mapOf(
            "name" to "${payment.firstName} ${payment.name}",
            "price" to payment.price,
            "eventName" to payment.subscribable.name,
            "additionalData" to payment.getAdditionalDataMap()
        )
        val mailBuilder = mailService.builder()
            .to(payment.email)
            .subject("Bevestiging registratie")
            .template("event-confirmation.html", params)
        payment.subscribable.communicationCC?.let { mailBuilder.cc(it) }
        mailBuilder.send()
    }

    override fun handlePaymentRefunded(payment: EventRegistration) {
        // TODO("send payment refunded confirmation email")
    }

    fun markRegistrationAsCompleted(id: Int) {
        logger.info { "Marking registration #$id as completed..." }
        val registration = getRegistrationById(id)
        check(registration.paid) { "Only a paid event can be marked as completed!" }
        if (registration.completed) {
            logger.warn { "Registration is already marked as completed!" }
            return
        }
        registration.completed = true
        paymentRepository.save(registration)
        if (registration.subscribable.sendCompleteConfirmation) {
            logger.info { "Linked event requires completion confirmation, sending mail..." }
            val params = mapOf(
                "customer" to "${registration.firstName} ${registration.name}",
                "eventName" to registration.subscribable.name
            )
            val mailBuilder = mailService.builder()
                .to(registration.email)
                .subject("Afwerking registratie")
                .template("event-completion.html", params)
            registration.subscribable.communicationCC?.let { mailBuilder.cc(it) }
            mailBuilder.send()
        }
        logger.info { "Registration #$id successfully marked as completed" }
    }

    private fun getRegistrationById(id: Int): EventRegistration {
        return paymentRepository.findById(id).orElseThrow { EventRegistrationNotFoundException() }
    }

    private fun getEventById(id: Int): Event {
        return eventRepository.findById(id).orElseThrow { EventNotFoundException() }
    }
}