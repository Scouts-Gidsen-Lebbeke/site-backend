package be.sgl.backend.service

import be.sgl.backend.repository.CalendarRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class CalendarService {

    @Autowired
    private lateinit var calendarRepository: CalendarRepository
}