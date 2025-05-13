package be.sgl.backend.service.event

import be.sgl.backend.dto.EventBaseDTO
import be.sgl.backend.dto.EventDTO
import be.sgl.backend.dto.EventResultDTO
import be.sgl.backend.entity.registrable.RegistrableStatus.*
import be.sgl.backend.entity.registrable.RegistrableStatus.Companion.getStatus
import be.sgl.backend.entity.registrable.event.Event
import be.sgl.backend.mapper.AddressMapper
import be.sgl.backend.repository.event.EventRepository
import be.sgl.backend.mapper.EventMapper
import be.sgl.backend.repository.event.EventRegistrationRepository
import be.sgl.backend.service.exception.EventNotFoundException
import be.sgl.backend.service.payment.CheckoutProvider
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalDateTime

@Service
class EventService {

    private val logger = KotlinLogging.logger {}

    @Autowired
    private lateinit var eventRepository: EventRepository
    @Autowired
    private lateinit var registrationRepository: EventRegistrationRepository
    @Autowired
    private lateinit var mapper: EventMapper
    @Autowired
    private lateinit var addressMapper: AddressMapper
    @Autowired
    private lateinit var checkoutProvider: CheckoutProvider

    fun getAllEvents(): List<EventResultDTO> {
        logger.debug { "Fetching all events" }
        return eventRepository.findAllRecentFirst().map { EventResultDTO(it, registrationRepository.getPaidRegistrationPricesByEvent(it)) }
    }

    fun getVisibleEvents(): List<EventBaseDTO> {
        logger.debug { "Fetching all visible events" }
        return eventRepository.findAllVisibleEvents().map(mapper::toBaseDto)
    }

    fun getEventDTOById(id: Int): EventDTO {
        logger.debug { "Fetching event #$id" }
        return mapper.toDto(getEventById(id))
    }

    fun saveEventDTO(dto: EventDTO): EventDTO {
        logger.info { "Saving new event ${dto.name} (${dto.start} - ${dto.end})" }
        check(LocalDateTime.now() < dto.closed) { "New events cannot be closed for registrations yet!" }
        validateEventDTO(dto)
        val newEvent = mapper.toEntity(dto)
        return mapper.toDto(eventRepository.save(newEvent))
    }

    fun mergeEventDTOChanges(id: Int, dto: EventDTO): EventDTO {
        logger.info { "Updating event #$id" }
        validateEventDTO(dto)
        val event = getEventById(id)
        // update this first, maybe the status alters
        event.closed = dto.closed
        check(event.getStatus() != CANCELLED) { "A cancelled event cannot be edited anymore!" }
        check(event.getStatus() != REGISTRATIONS_COMPLETED) { "An event with closed registrations cannot be edited anymore!" }
        check(event.getStatus() != STARTED) { "A started event cannot be edited anymore!" }
        check(event.getStatus() != COMPLETED) { "A completed event cannot be edited anymore!" }
        if (event.getStatus() == NOT_YET_OPEN) {
            logger.info { "Event registrations are not yet open, activity can be fully edited" }
            // price and user data collection can only be altered if no registration was possible yet
            event.price = dto.price
            event.additionalForm = dto.additionalForm
            event.additionalFormRule = dto.additionalFormRule
            event.needsMobile = dto.needsMobile
            check(dto.cancellable || !event.cancellable) { "A previously cancellable event cannot be made uncancellable!" }
            event.cancellable = dto.cancellable
            // Core activity data that is used in certificates, should never be changed when registrations opened
            event.name = dto.name
            event.start = dto.start
            event.end = dto.end
            // One can only delay or advance the registration period when it wasn't open yet
            event.open = dto.open
        } else {
            logger.info { "Event registrations are already open, registration limit should respect current registration count" }
            val registrationCount = registrationRepository.countPaidRegistrationsByEvent(event)
            check(dto.registrationLimit == null || registrationCount < dto.registrationLimit!!) { "The registration limit cannot be lowered below the current registration count!" }
        }
        event.registrationLimit = dto.registrationLimit
        event.address = dto.address?.let { addressMapper.toEntity(it) }
        event.sendConfirmation = dto.sendConfirmation
        event.sendCompleteConfirmation = dto.sendCompleteConfirmation
        event.communicationCC = dto.communicationCC
        event.description = dto.description
        return mapper.toDto(eventRepository.save(event))
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

    private fun validateEventDTO(dto: EventDTO) {
        logger.debug { "Validating a correct open-closed-start-end sequence" }
        check(dto.open < dto.closed) { "The closure of registrations should be after the opening of registrations!" }
        check(dto.closed < dto.start) { "The start date of an event should be after the closure of registrations!" }
        check(dto.start < dto.end) { "The start date of an event should be before its end date!" }
    }

    private fun getEventById(id: Int): Event {
        return eventRepository.findById(id).orElseThrow { EventNotFoundException() }
    }
}