package be.sgl.backend.service

import be.sgl.backend.dto.EventBaseDTO
import be.sgl.backend.dto.EventDTO
import be.sgl.backend.repository.EventRepository
import be.sgl.backend.mapper.EventMapper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class EventService {

    @Autowired
    private lateinit var eventRepository: EventRepository
    @Autowired
    private lateinit var eventMapper: EventMapper

    fun getAllEvents(): List<EventBaseDTO> {
        return eventRepository.findAll().map(eventMapper::toBaseDto)
    }

    fun getVisibleEvents(): List<EventBaseDTO> {
        return eventRepository.findAllByEndAfterOrderByStart(LocalDateTime.now()).map(eventMapper::toBaseDto)
    }
}