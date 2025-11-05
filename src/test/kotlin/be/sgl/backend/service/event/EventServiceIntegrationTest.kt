package be.sgl.backend.service.event

import be.sgl.backend.config.TestConfigurations
import be.sgl.backend.dto.EventDTO
import be.sgl.backend.entity.registrable.event.Event
import be.sgl.backend.entity.registrable.event.EventRegistration
import be.sgl.backend.repository.event.EventRegistrationRepository
import be.sgl.backend.repository.event.EventRepository
import be.sgl.backend.service.exception.EventNotFoundException
import be.sgl.backend.util.IntegrationTest
import jakarta.transaction.Transactional
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import java.time.LocalDateTime

@IntegrationTest
@Import(TestConfigurations::class)
@Transactional
class EventServiceIntegrationTest {

    @Autowired
    private lateinit var eventService: EventService

    @Autowired
    private lateinit var eventRepository: EventRepository

    @Autowired
    private lateinit var registrationRepository: EventRegistrationRepository

    private lateinit var testEvent: Event

    @BeforeEach
    fun setup() {
        registrationRepository.deleteAll()
        eventRepository.deleteAll()

        testEvent = Event().apply {
            name = "Test Event"
            description = "Test Description"
            open = LocalDateTime.now().plusDays(1)
            closed = LocalDateTime.now().plusDays(7)
            start = LocalDateTime.now().plusDays(10)
            end = LocalDateTime.now().plusDays(12)
            price = 50.0
            needsMobile = true
            cancellable = true
            sendConfirmation = true
            sendCompleteConfirmation = false
        }
    }

    @Test
    fun `getAllEvents should return all events with registration data`() {
        val saved = eventRepository.save(testEvent)
        eventRepository.flush()

        val events = eventService.getAllEvents()

        assertNotNull(events)
        assertTrue(events.any { it.event.id == saved.id })
    }

    @Test
    fun `getVisibleEvents should only return non-cancelled events`() {
        val visibleEvent = testEvent
        val cancelledEvent = Event().apply {
            name = "Cancelled Event"
            description = "Cancelled"
            open = LocalDateTime.now().plusDays(1)
            closed = LocalDateTime.now().plusDays(7)
            start = LocalDateTime.now().plusDays(10)
            end = LocalDateTime.now().plusDays(12)
            price = 50.0
            cancelled = true
        }

        eventRepository.save(visibleEvent)
        eventRepository.save(cancelledEvent)
        eventRepository.flush()

        val events = eventService.getVisibleEvents()

        assertTrue(events.any { it.name == "Test Event" })
        assertFalse(events.any { it.name == "Cancelled Event" })
    }

    @Test
    fun `saveEventDTO should validate date sequence`() {
        val dto = EventDTO(
            id = null,
            name = "Invalid Event",
            description = "Test",
            open = LocalDateTime.now().plusDays(10),
            closed = LocalDateTime.now().plusDays(5), // Closed before open!
            start = LocalDateTime.now().plusDays(15),
            end = LocalDateTime.now().plusDays(20),
            price = 50.0,
            needsMobile = true,
            registrationLimit = null,
            address = null,
            additionalForm = null,
            additionalFormRule = null,
            cancellable = true,
            sendConfirmation = true,
            sendCompleteConfirmation = false,
            communicationCC = null
        )

        val exception = assertThrows(IllegalStateException::class.java) {
            eventService.saveEventDTO(dto)
        }

        assertEquals("The closure of registrations should be after the opening of registrations!", exception.message)
    }

    @Test
    fun `saveEventDTO should validate closed before start`() {
        val dto = EventDTO(
            id = null,
            name = "Invalid Event",
            description = "Test",
            open = LocalDateTime.now().plusDays(5),
            closed = LocalDateTime.now().plusDays(10),
            start = LocalDateTime.now().plusDays(8), // Start before closed!
            end = LocalDateTime.now().plusDays(20),
            price = 50.0,
            needsMobile = true,
            registrationLimit = null,
            address = null,
            additionalForm = null,
            additionalFormRule = null,
            cancellable = true,
            sendConfirmation = true,
            sendCompleteConfirmation = false,
            communicationCC = null
        )

        val exception = assertThrows(IllegalStateException::class.java) {
            eventService.saveEventDTO(dto)
        }

        assertEquals("The start date of an event should be after the closure of registrations!", exception.message)
    }

    @Test
    fun `saveEventDTO should validate start before end`() {
        val dto = EventDTO(
            id = null,
            name = "Invalid Event",
            description = "Test",
            open = LocalDateTime.now().plusDays(5),
            closed = LocalDateTime.now().plusDays(10),
            start = LocalDateTime.now().plusDays(20),
            end = LocalDateTime.now().plusDays(15), // End before start!
            price = 50.0,
            needsMobile = true,
            registrationLimit = null,
            address = null,
            additionalForm = null,
            additionalFormRule = null,
            cancellable = true,
            sendConfirmation = true,
            sendCompleteConfirmation = false,
            communicationCC = null
        )

        val exception = assertThrows(IllegalStateException::class.java) {
            eventService.saveEventDTO(dto)
        }

        assertEquals("The start date of an event should be before its end date!", exception.message)
    }

    @Test
    fun `mergeEventDTOChanges should not allow editing cancelled event`() {
        testEvent.cancelled = true
        val saved = eventRepository.save(testEvent)
        eventRepository.flush()

        val dto = EventDTO(
            id = saved.id,
            name = "Updated Event",
            description = "Updated",
            open = testEvent.open,
            closed = testEvent.closed,
            start = testEvent.start,
            end = testEvent.end,
            price = 60.0,
            needsMobile = true,
            registrationLimit = null,
            address = null,
            additionalForm = null,
            additionalFormRule = null,
            cancellable = true,
            sendConfirmation = true,
            sendCompleteConfirmation = false,
            communicationCC = null
        )

        val exception = assertThrows(IllegalStateException::class.java) {
            eventService.mergeEventDTOChanges(saved.id!!, dto)
        }

        assertEquals("A cancelled event cannot be edited anymore!", exception.message)
    }

    @Test
    fun `mergeEventDTOChanges should allow full edit when not yet open`() {
        val saved = eventRepository.save(testEvent)
        eventRepository.flush()

        val dto = EventDTO(
            id = saved.id,
            name = "Updated Event",
            description = "Updated Description",
            open = testEvent.open,
            closed = testEvent.closed,
            start = testEvent.start,
            end = testEvent.end,
            price = 75.0,
            needsMobile = false,
            registrationLimit = 100,
            address = null,
            additionalForm = "{\"field\": \"value\"}",
            additionalFormRule = "price + 10",
            cancellable = false,
            sendConfirmation = false,
            sendCompleteConfirmation = true,
            communicationCC = "cc@example.com"
        )

        val updated = eventService.mergeEventDTOChanges(saved.id!!, dto)

        assertEquals("Updated Event", updated.name)
        assertEquals(75.0, updated.price)
        assertFalse(updated.needsMobile)
        assertEquals(100, updated.registrationLimit)
    }

    @Test
    fun `mergeEventDTOChanges should not allow lowering limit below registrations`() {
        testEvent.registrationLimit = 10
        testEvent.open = LocalDateTime.now().minusDays(5) // Already open
        testEvent.closed = LocalDateTime.now().plusDays(5)
        val saved = eventRepository.save(testEvent)
        eventRepository.flush()

        // Create 3 paid registrations
        repeat(3) { index ->
            val registration = EventRegistration().apply {
                subscribable = saved
                name = "User$index"
                firstName = "Test"
                email = "user$index@example.com"
                price = saved.price
                paid = true
            }
            registrationRepository.save(registration)
        }
        registrationRepository.flush()

        val dto = EventDTO(
            id = saved.id,
            name = saved.name,
            description = saved.description,
            open = saved.open,
            closed = saved.closed,
            start = saved.start,
            end = saved.end,
            price = saved.price,
            needsMobile = saved.needsMobile,
            registrationLimit = 2, // Trying to lower below 3 registrations!
            address = null,
            additionalForm = null,
            additionalFormRule = null,
            cancellable = true,
            sendConfirmation = true,
            sendCompleteConfirmation = false,
            communicationCC = null
        )

        val exception = assertThrows(IllegalStateException::class.java) {
            eventService.mergeEventDTOChanges(saved.id!!, dto)
        }

        assertEquals("The registration limit cannot be lowered below the current registration count!", exception.message)
    }

    @Test
    fun `cancelEvent should mark event as cancelled`() {
        testEvent.open = LocalDateTime.now().minusDays(5)
        testEvent.closed = LocalDateTime.now().plusDays(5)
        val saved = eventRepository.save(testEvent)
        eventRepository.flush()

        eventService.cancelEvent(saved.id!!)

        val cancelled = eventRepository.findById(saved.id!!).get()
        assertTrue(cancelled.cancelled)
    }

    @Test
    fun `cancelEvent should not allow cancelling already cancelled event`() {
        testEvent.cancelled = true
        val saved = eventRepository.save(testEvent)
        eventRepository.flush()

        val exception = assertThrows(IllegalStateException::class.java) {
            eventService.cancelEvent(saved.id!!)
        }

        assertEquals("This event is already cancelled!", exception.message)
    }

    @Test
    fun `cancelEvent should not allow cancelling started event`() {
        testEvent.open = LocalDateTime.now().minusDays(15)
        testEvent.closed = LocalDateTime.now().minusDays(10)
        testEvent.start = LocalDateTime.now().minusDays(5)
        testEvent.end = LocalDateTime.now().plusDays(1)
        val saved = eventRepository.save(testEvent)
        eventRepository.flush()

        val exception = assertThrows(IllegalStateException::class.java) {
            eventService.cancelEvent(saved.id!!)
        }

        assertEquals("A started event cannot be cancelled anymore!", exception.message)
    }

    @Test
    fun `getEventDTOById should return event when exists`() {
        val saved = eventRepository.save(testEvent)
        eventRepository.flush()

        val result = eventService.getEventDTOById(saved.id!!)

        assertNotNull(result)
        assertEquals("Test Event", result.name)
    }

    @Test
    fun `getEventDTOById should throw exception when not found`() {
        assertThrows(EventNotFoundException::class.java) {
            eventService.getEventDTOById(999)
        }
    }

    @Test
    fun `mergeEventDTOChanges should not allow making previously cancellable event uncancellable`() {
        testEvent.cancellable = true
        val saved = eventRepository.save(testEvent)
        eventRepository.flush()

        val dto = EventDTO(
            id = saved.id,
            name = saved.name,
            description = saved.description,
            open = saved.open,
            closed = saved.closed,
            start = saved.start,
            end = saved.end,
            price = saved.price,
            needsMobile = saved.needsMobile,
            registrationLimit = null,
            address = null,
            additionalForm = null,
            additionalFormRule = null,
            cancellable = false, // Trying to make it uncancellable!
            sendConfirmation = true,
            sendCompleteConfirmation = false,
            communicationCC = null
        )

        val exception = assertThrows(IllegalStateException::class.java) {
            eventService.mergeEventDTOChanges(saved.id!!, dto)
        }

        assertEquals("A previously cancellable event cannot be made uncancellable!", exception.message)
    }
}
