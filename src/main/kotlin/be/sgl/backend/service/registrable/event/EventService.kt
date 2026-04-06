package be.sgl.backend.service.registrable.event

import be.sgl.backend.dto.registrable.event.CreateOrUpdateEventRequest
import be.sgl.backend.dto.registrable.event.EventBaseDTO
import be.sgl.backend.dto.registrable.event.EventDTO
import be.sgl.backend.dto.registrable.event.EventResult
import be.sgl.backend.entity.registrable.RegistrableStatus.*
import be.sgl.backend.entity.registrable.RegistrableStatus.Companion.getStatus
import be.sgl.backend.entity.registrable.event.Event
import be.sgl.backend.mapper.registrable.event.EventMapper
import be.sgl.backend.repository.event.EventRegistrationRepository
import be.sgl.backend.repository.event.EventRepository
import be.sgl.backend.exception.EventNotFoundException
import be.sgl.backend.service.payment.CheckoutProvider
import mu.KotlinLogging
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class EventService(
    private val eventRepository: EventRepository,
    private val registrationRepository: EventRegistrationRepository,
    private val mapper: EventMapper,
    private val checkoutProvider: CheckoutProvider
) {

    private val logger = KotlinLogging.logger {}

    fun getAllEvents(): List<EventResult> {
        logger.debug { "Fetching all events" }
        return eventRepository.findAllRecentFirst()
            .map { EventResult.of(it, registrationRepository.getPaidRegistrationPricesByEvent(it)) }
    }

    fun getVisibleEvents(): List<EventBaseDTO> {
        logger.debug { "Fetching all visible events" }
        return eventRepository.findAllVisibleEvents().map(mapper::toBaseDto)
    }

    fun getEventDTOById(id: Int): EventDTO {
        logger.debug { "Fetching event #$id" }
        return mapper.toDto(getEventById(id))
    }

    fun createEvent(request: CreateOrUpdateEventRequest): EventDTO {
        logger.info { "Saving new event ${request.name} (${request.start} - ${request.end})" }
        check(LocalDateTime.now() < request.closed) { "New events cannot be closed for registrations yet!" }
        val newEvent = mapper.toEntity(request)
        validateEvent(newEvent)
        return mapper.toDto(eventRepository.save(newEvent))
    }

    fun updateEvent(id: Int, request: CreateOrUpdateEventRequest): EventDTO {
        logger.info { "Updating event #$id" }
        val eventFromDto = mapper.toEntity(request)
        validateEvent(eventFromDto)
        val eventToUpdate = getEventById(id)
        // update this first, maybe the status alters
        eventToUpdate.closed = eventFromDto.closed
        check(eventToUpdate.getStatus() != CANCELLED) { "A cancelled event cannot be edited anymore!" }
        check(eventToUpdate.getStatus() != REGISTRATIONS_COMPLETED) { "An event with closed registrations cannot be edited anymore!" }
        check(eventToUpdate.getStatus() != STARTED) { "A started event cannot be edited anymore!" }
        check(eventToUpdate.getStatus() != COMPLETED) { "A completed event cannot be edited anymore!" }
        if (eventToUpdate.getStatus() == NOT_YET_OPEN) {
            logger.info { "Event registrations are not yet open, activity can be fully edited" }
            // price and user data collection can only be altered if no registration was possible yet
            eventToUpdate.price = eventFromDto.price
            eventToUpdate.additionalForm = eventFromDto.additionalForm
            eventToUpdate.additionalFormRule = eventFromDto.additionalFormRule
            eventToUpdate.needsMobile = eventFromDto.needsMobile
            check(eventFromDto.cancellable || !eventToUpdate.cancellable) { "A previously cancellable event cannot be made uncancellable!" }
            eventToUpdate.cancellable = eventFromDto.cancellable
            // Core activity data that is used in certificates, should never be changed when registrations opened
            eventToUpdate.name = eventFromDto.name
            eventToUpdate.start = eventFromDto.start
            eventToUpdate.end = eventFromDto.end
            // One can only delay or advance the registration period when it wasn't open yet
            eventToUpdate.open = eventFromDto.open
        } else {
            logger.info { "Event registrations are already open, registration limit should respect current registration count" }
            val registrationCount = registrationRepository.countPaidRegistrationsByEvent(eventToUpdate)
            check(eventFromDto.registrationLimit == null || registrationCount < eventFromDto.registrationLimit!!) { "The registration limit cannot be lowered below the current registration count!" }
        }
        eventToUpdate.registrationLimit = eventFromDto.registrationLimit
        eventToUpdate.address = eventFromDto.address
        eventToUpdate.sendConfirmation = eventFromDto.sendConfirmation
        eventToUpdate.sendCompleteConfirmation = eventFromDto.sendCompleteConfirmation
        eventToUpdate.communicationCC = eventFromDto.communicationCC
        eventToUpdate.description = eventFromDto.description
        return mapper.toDto(eventRepository.save(eventToUpdate))
    }

    fun cancelEvent(id: Int) {
        logger.info { "Cancel event #$id..." }
        val event = getEventById(id)
        check(event.getStatus() != CANCELLED) { "This event is already cancelled!" }
        check(event.getStatus() != STARTED) { "A started event cannot be cancelled anymore!" }
        check(event.getStatus() != COMPLETED) { "A completed event cannot be cancelled anymore!" }
        val registrations = registrationRepository.getRegistrationsByEvent(event)
        if (registrations.isNotEmpty()) {
            logger.info { "Event has ${registrations.size} linked registrations needing a refund..." }
            registrations.forEach {
                checkoutProvider.refundPayment(it)
                logger.info { "Refund request sent for registration #${it.id}" }
            }
        }
        logger.info { "Registrations fully checked, marking event as cancelled..." }
        event.cancelled = true
        eventRepository.save(event)
        logger.info { "Event successfully cancelled" }
    }

    private fun validateEvent(event: Event) {
        logger.debug { "Validating a correct open-closed-start-end sequence" }
        check(event.open < event.closed) { "The closure of registrations should be after the opening of registrations!" }
        check(event.closed < event.start) { "The start date of an event should be after the closure of registrations!" }
        check(event.start < event.end) { "The start date of an event should be before its end date!" }
    }

    private fun getEventById(id: Int): Event {
        return eventRepository.findById(id).orElseThrow { EventNotFoundException() }
    }
}