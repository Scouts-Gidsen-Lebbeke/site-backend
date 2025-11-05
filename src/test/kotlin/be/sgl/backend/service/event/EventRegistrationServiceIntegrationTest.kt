package be.sgl.backend.service.event

import be.sgl.backend.config.TestConfigurations
import be.sgl.backend.dto.EventRegistrationAttemptData
import be.sgl.backend.entity.registrable.event.Event
import be.sgl.backend.entity.registrable.event.EventRegistration
import be.sgl.backend.repository.event.EventRegistrationRepository
import be.sgl.backend.repository.event.EventRepository
import be.sgl.backend.service.exception.EventRegistrationNotFoundException
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
class EventRegistrationServiceIntegrationTest {

    @Autowired
    private lateinit var registrationService: EventRegistrationService

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
            open = LocalDateTime.now().minusDays(1) // Already open
            closed = LocalDateTime.now().plusDays(7)
            start = LocalDateTime.now().plusDays(10)
            end = LocalDateTime.now().plusDays(12)
            price = 50.0
            needsMobile = true
            cancellable = true
            sendConfirmation = true
            sendCompleteConfirmation = false
        }
        testEvent = eventRepository.save(testEvent)
    }

    @Test
    fun `createPaymentForEvent should fail without mobile when needsMobile is true`() {
        val attempt = EventRegistrationAttemptData(
            name = "Doe",
            firstName = "John",
            email = "john@example.com",
            mobile = null, // Missing mobile
            additionalData = null
        )

        val exception = assertThrows(IllegalStateException::class.java) {
            registrationService.createPaymentForEvent(testEvent.id!!, attempt, null)
        }

        assertEquals("No valid mobile number provided!", exception.message)
    }

    @Test
    fun `createPaymentForEvent should succeed with mobile when needsMobile is true`() {
        val attempt = EventRegistrationAttemptData(
            name = "Doe",
            firstName = "John",
            email = "john@example.com",
            mobile = "0123456789",
            additionalData = null
        )

        val checkoutUrl = registrationService.createPaymentForEvent(testEvent.id!!, attempt, null)

        assertNotNull(checkoutUrl)
        assertTrue(checkoutUrl.isNotBlank())

        // Verify registration was created
        val registrations = registrationRepository.findAll()
        assertEquals(1, registrations.size)
        assertEquals("Doe", registrations[0].name)
        assertEquals("john@example.com", registrations[0].email)
    }

    @Test
    fun `createPaymentForEvent should calculate price with additional data`() {
        testEvent.additionalFormRule = "nights * 10"
        eventRepository.save(testEvent)

        val attempt = EventRegistrationAttemptData(
            name = "Doe",
            firstName = "John",
            email = "john@example.com",
            mobile = "0123456789",
            additionalData = "{\"nights\":\"3\"}"
        )

        registrationService.createPaymentForEvent(testEvent.id!!, attempt, null)

        val registration = registrationRepository.findAll()[0]
        assertEquals(80.0, registration.price) // 50 base + 30 (3 nights * 10)
    }

    @Test
    fun `getAllRegistrationsForEvent should return only paid registrations`() {
        val paidReg = EventRegistration().apply {
            subscribable = testEvent
            name = "Paid"
            firstName = "User"
            email = "paid@example.com"
            mobile = "0123456789"
            price = testEvent.price
            paid = true
        }

        val unpaidReg = EventRegistration().apply {
            subscribable = testEvent
            name = "Unpaid"
            firstName = "User"
            email = "unpaid@example.com"
            mobile = "0987654321"
            price = testEvent.price
            paid = false
        }

        registrationRepository.save(paidReg)
        registrationRepository.save(unpaidReg)
        registrationRepository.flush()

        val registrations = registrationService.getAllRegistrationsForEvent(testEvent.id!!)

        assertEquals(1, registrations.size)
        assertEquals("paid@example.com", registrations[0].email)
    }

    @Test
    fun `markRegistrationAsCompleted should fail for unpaid registration`() {
        val unpaidReg = EventRegistration().apply {
            subscribable = testEvent
            name = "User"
            firstName = "Test"
            email = "test@example.com"
            mobile = "0123456789"
            price = testEvent.price
            paid = false
        }
        val saved = registrationRepository.save(unpaidReg)
        registrationRepository.flush()

        val exception = assertThrows(IllegalStateException::class.java) {
            registrationService.markRegistrationAsCompleted(saved.id!!)
        }

        assertEquals("Only a paid event can be marked as completed!", exception.message)
    }

    @Test
    fun `markRegistrationAsCompleted should fail if event hasn't started yet`() {
        testEvent.start = LocalDateTime.now().plusDays(5) // Event in future
        eventRepository.save(testEvent)

        val paidReg = EventRegistration().apply {
            subscribable = testEvent
            name = "User"
            firstName = "Test"
            email = "test@example.com"
            mobile = "0123456789"
            price = testEvent.price
            paid = true
        }
        val saved = registrationRepository.save(paidReg)
        registrationRepository.flush()

        val exception = assertThrows(IllegalStateException::class.java) {
            registrationService.markRegistrationAsCompleted(saved.id!!)
        }

        assertEquals("Registrations can only be completed starting one hour before the event!", exception.message)
    }

    @Test
    fun `markRegistrationAsCompleted should succeed for paid registration near event start`() {
        testEvent.start = LocalDateTime.now().minusMinutes(30) // Started 30 min ago
        testEvent.end = LocalDateTime.now().plusHours(2)
        eventRepository.save(testEvent)

        val paidReg = EventRegistration().apply {
            subscribable = testEvent
            name = "User"
            firstName = "Test"
            email = "test@example.com"
            mobile = "0123456789"
            price = testEvent.price
            paid = true
            completed = false
        }
        val saved = registrationRepository.save(paidReg)
        registrationRepository.flush()

        registrationService.markRegistrationAsCompleted(saved.id!!)

        val updated = registrationRepository.findById(saved.id!!).get()
        assertTrue(updated.completed)
    }

    @Test
    fun `markRegistrationAsCompleted should not fail if already completed`() {
        testEvent.start = LocalDateTime.now().minusMinutes(30)
        testEvent.end = LocalDateTime.now().plusHours(2)
        eventRepository.save(testEvent)

        val paidReg = EventRegistration().apply {
            subscribable = testEvent
            name = "User"
            firstName = "Test"
            email = "test@example.com"
            mobile = "0123456789"
            price = testEvent.price
            paid = true
            completed = true // Already completed
        }
        val saved = registrationRepository.save(paidReg)
        registrationRepository.flush()

        // Should not throw exception, just log warning
        assertDoesNotThrow {
            registrationService.markRegistrationAsCompleted(saved.id!!)
        }

        val registration = registrationRepository.findById(saved.id!!).get()
        assertTrue(registration.completed)
    }

    @Test
    fun `cancelRegistration should fail for unpaid registration`() {
        val unpaidReg = EventRegistration().apply {
            subscribable = testEvent
            name = "User"
            firstName = "Test"
            email = "test@example.com"
            mobile = "0123456789"
            price = testEvent.price
            paid = false
        }
        val saved = registrationRepository.save(unpaidReg)
        registrationRepository.flush()

        val exception = assertThrows(IllegalStateException::class.java) {
            registrationService.cancelRegistration(saved.id!!)
        }

        assertEquals("Only a paid event registration can be cancelled!", exception.message)
    }

    @Test
    fun `cancelRegistration should fail when registrations are closed`() {
        testEvent.closed = LocalDateTime.now().minusDays(1) // Already closed
        eventRepository.save(testEvent)

        val paidReg = EventRegistration().apply {
            subscribable = testEvent
            name = "User"
            firstName = "Test"
            email = "test@example.com"
            mobile = "0123456789"
            price = testEvent.price
            paid = true
        }
        val saved = registrationRepository.save(paidReg)
        registrationRepository.flush()

        val exception = assertThrows(IllegalStateException::class.java) {
            registrationService.cancelRegistration(saved.id!!)
        }

        assertEquals("Cancellation is only possible when registrations are still open!", exception.message)
    }

    @Test
    fun `getEventRegistrationDTOById should return registration when exists`() {
        val registration = EventRegistration().apply {
            subscribable = testEvent
            name = "User"
            firstName = "Test"
            email = "test@example.com"
            mobile = "0123456789"
            price = testEvent.price
            paid = true
        }
        val saved = registrationRepository.save(registration)
        registrationRepository.flush()

        val result = registrationService.getEventRegistrationDTOById(saved.id!!)

        assertNotNull(result)
        assertEquals("test@example.com", result?.email)
    }

    @Test
    fun `getEventRegistrationDTOById should return null when not found`() {
        val result = registrationService.getEventRegistrationDTOById(999)

        assertNull(result)
    }

    @Test
    fun `createPaymentForEvent should allow registration without mobile when not needed`() {
        testEvent.needsMobile = false
        eventRepository.save(testEvent)

        val attempt = EventRegistrationAttemptData(
            name = "Doe",
            firstName = "John",
            email = "john@example.com",
            mobile = null, // No mobile, but that's OK now
            additionalData = null
        )

        val checkoutUrl = registrationService.createPaymentForEvent(testEvent.id!!, attempt, null)

        assertNotNull(checkoutUrl)
    }

    @Test
    fun `createPaymentForEvent should handle additional data with complex expressions`() {
        testEvent.additionalFormRule = "adults * 20 + children * 10 + (lunch ? 15 : 0)"
        eventRepository.save(testEvent)

        val attempt = EventRegistrationAttemptData(
            name = "Doe",
            firstName = "John",
            email = "john@example.com",
            mobile = "0123456789",
            additionalData = "{\"adults\":\"2\",\"children\":\"3\",\"lunch\":\"true\"}"
        )

        registrationService.createPaymentForEvent(testEvent.id!!, attempt, null)

        val registration = registrationRepository.findAll()[0]
        // Base: 50 + (2*20 + 3*10 + 15) = 50 + (40 + 30 + 15) = 135
        assertEquals(135.0, registration.price)
    }
}
