package be.sgl.backend.service.event

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

    fun getEventRegistrationDTOById(id: Int) : EventRegistrationDTO {
        return mapper.toDto(getRegistrationById(id))
    }

    fun createPaymentForEvent(id: Int, attempt: EventRegistrationAttemptData, username: String?): String {
        val event = getEventById(id)
        check(!event.needsMobile || attempt.mobile != null) { "No valid mobile number provided!" }
        val user = username?.let { userDataProvider.findUser(it) }
        check(!isGlobalLimitReached(event)) { "The limit for this event is reached!" }
        val finalPrice = calculatePriceForEvent(event, attempt.additionalData)
        var registration = EventRegistration(event, attempt, finalPrice, user)
        registration = paymentRepository.save(registration)
        val checkoutUrl = checkoutProvider.createCheckoutUrl(user?.customerId, registration, "events")
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
        val additionalDataMap = payment.additionalData
            ?.let { ObjectMapper().readValue(it, Map::class.java) }
            ?: emptyMap<String, Any>()
        val params = mapOf(
            "customer.full.name" to "${payment.firstName} ${payment.name}",
            "event.price" to payment.price,
            "event.name" to payment.subscribable.name,
            "event.additional.data" to additionalDataMap
        )
        mailService.builder()
            .to(payment.email)
            .subject("Bevestiging inschrijving")
            .template("event-confirmation.html", params)
            .send()
    }

    override fun handlePaymentRefunded(payment: EventRegistration) {
        // TODO("send payment refunded confirmation email")
    }

    private fun getRegistrationById(id: Int): EventRegistration {
        return paymentRepository.findById(id).orElseThrow { EventRegistrationNotFoundException() }
    }

    private fun getEventById(id: Int): Event {
        return eventRepository.findById(id).orElseThrow { EventNotFoundException() }
    }
}