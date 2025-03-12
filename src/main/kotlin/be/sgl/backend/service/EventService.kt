package be.sgl.backend.service

import be.sgl.backend.dto.EventBaseDTO
import be.sgl.backend.dto.EventDTO
import be.sgl.backend.entity.registrable.event.Event
import be.sgl.backend.repository.event.EventRepository
import be.sgl.backend.mapper.EventMapper
import be.sgl.backend.repository.event.EventRegistrationRepository
import be.sgl.backend.service.exception.EventNotFoundException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class EventService {

    @Autowired
    private lateinit var eventRepository: EventRepository
    @Autowired
    private lateinit var registrationRepository: EventRegistrationRepository
    @Autowired
    private lateinit var mapper: EventMapper

    fun getAllEvents(): List<EventBaseDTO> {
        return eventRepository.findAll().map(mapper::toBaseDto)
    }

    fun getVisibleEvents(): List<EventBaseDTO> {
        return eventRepository.findAllByEndAfterOrderByStart(LocalDateTime.now()).map(mapper::toBaseDto)
    }

    fun getEventDTOById(id: Int): EventDTO {
        return mapper.toDto(getEventById(id))
    }

    fun saveEventDTO(eventDTO: EventDTO): EventDTO {
        validateEventDTO(eventDTO)
        // TODO: additional validations for new events
        val newEvent = mapper.toEntity(eventDTO)
        return mapper.toDto(eventRepository.save(newEvent))
    }

    fun mergeEventDTOChanges(id: Int, eventDTO: EventDTO): EventDTO {
        TODO()
    }

    fun deleteEvent(id: Int) {
        val event = getEventById(id)
        check(hasRegistrations(event)) { "This event has registrations and cannot be deleted anymore!" }
        eventRepository.deleteById(id)
    }

    private fun validateEventDTO(eventDTO: EventDTO): Boolean {
        TODO()
    }

    private fun hasRegistrations(event: Event): Boolean {
        return registrationRepository.existsBySubscribable(event)
    }

    private fun getEventById(id: Int): Event {
        return eventRepository.findById(id).orElseThrow { EventNotFoundException() }
    }
}