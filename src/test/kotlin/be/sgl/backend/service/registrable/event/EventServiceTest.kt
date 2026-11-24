package be.sgl.backend.service.registrable.event

import be.sgl.backend.dto.registrable.event.CreateOrUpdateEventRequestTestMother.eventRequest
import be.sgl.backend.entity.PayableTestMother.event
import be.sgl.backend.entity.PaymentTestMother.eventRegistration
import be.sgl.backend.entity.registrable.event.Event
import be.sgl.backend.exception.EventNotFoundException
import be.sgl.backend.repository.event.EventRegistrationRepository
import be.sgl.backend.repository.event.EventRepository
import be.sgl.backend.util.anyValue
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.then
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import java.time.LocalDateTime
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class EventServiceTest {

    @Mock
    lateinit var eventRepository: EventRepository
    @Mock
    lateinit var registrationRepository: EventRegistrationRepository
    @Mock
    lateinit var registrationService: EventRegistrationService
    @InjectMocks
    lateinit var testee: EventService

    private val now = LocalDateTime.now()

    private fun notYetOpenEvent() = event().id(1)
        .open(now.plusDays(1)).closed(now.plusDays(10)).start(now.plusDays(20)).end(now.plusDays(21))

    private fun openedEvent() = event().id(1)
        .open(now.minusDays(1)).closed(now.plusDays(10)).start(now.plusDays(20)).end(now.plusDays(21))

    private fun givenSaveReturnsArgument() {
        given(eventRepository.save(anyValue<Event>())).willAnswer { it.getArgument<Event>(0).also { e -> e.id = e.id ?: 1 } }
    }

    @Test
    fun `should summarize the paid registrations per event`() {
        // given
        val event = event().build()
        val prices = listOf(10.0, 12.5)
        given(eventRepository.findAllRecentFirst()).willReturn(listOf(event))
        given(registrationRepository.getPaidRegistrationPricesByEvent(event)).willReturn(prices)

        // when
        val results = testee.getAllEvents()

        // then
        assertThat(results).singleElement().satisfies({
            assertThat(it.registrationCount).isEqualTo(prices.size)
            assertThat(it.totalPrice).isEqualTo(prices.sum())
        })
    }

    @Test
    fun `should fail when the event is not found`() {
        // given
        given(eventRepository.findById(1)).willReturn(Optional.empty())

        // when / then
        assertThatThrownBy { testee.getEventDTOById(1) }
            .isInstanceOf(EventNotFoundException::class.java)
            .hasMessage("Event not found")
    }

    @Nested
    inner class CreateEvent {

        @Test
        fun `should fail when the closing date is already in the past`() {
            // given
            val request = eventRequest().open(now.minusDays(10)).closed(now.minusDays(1)).build()

            // when / then
            assertThatThrownBy { testee.createEvent(request) }
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessage("New events cannot be closed for registrations yet!")
        }

        @Test
        fun `should fail when registrations close before they open`() {
            // given
            val request = eventRequest().open(now.plusDays(6)).closed(now.plusDays(5)).build()

            // when / then
            assertThatThrownBy { testee.createEvent(request) }
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessage("The closure of registrations should be after the opening of registrations!")
        }

        @Test
        fun `should fail when the event starts before registrations close`() {
            // given
            val request = eventRequest().closed(now.plusDays(10)).start(now.plusDays(9)).build()

            // when / then
            assertThatThrownBy { testee.createEvent(request) }
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessage("The start date of an event should be after the closure of registrations!")
        }

        @Test
        fun `should fail when the event ends before it starts`() {
            // given
            val request = eventRequest().start(now.plusDays(20)).end(now.plusDays(19)).build()

            // when / then
            assertThatThrownBy { testee.createEvent(request) }
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessage("The start date of an event should be before its end date!")
        }

        @Test
        fun `should save a valid event`() {
            // given
            val request = eventRequest().needsMobile().price(15.0).build()
            givenSaveReturnsArgument()

            // when
            val result = testee.createEvent(request)

            // then
            assertThat(result.name).isEqualTo(request.name)
            assertThat(result.price).isEqualTo(request.price)
            assertThat(result.needsMobile).isTrue()
        }
    }

    @Nested
    inner class UpdateEvent {

        @Test
        fun `should validate the request before looking up the event`() {
            // given
            val request = eventRequest().start(now.plusDays(20)).end(now.plusDays(19)).build()

            // when / then
            assertThatThrownBy { testee.updateEvent(1, request) }
                .isInstanceOf(IllegalStateException::class.java)
            then(eventRepository).shouldHaveNoInteractions()
        }

        @Test
        fun `should fail when the event is not found`() {
            // given
            given(eventRepository.findById(1)).willReturn(Optional.empty())

            // when / then
            assertThatThrownBy { testee.updateEvent(1, eventRequest().build()) }
                .isInstanceOf(EventNotFoundException::class.java)
        }

        @Test
        fun `should fail when the event is cancelled`() {
            // given
            given(eventRepository.findById(1)).willReturn(Optional.of(notYetOpenEvent().cancelled(true).build()))

            // when / then
            assertThatThrownBy { testee.updateEvent(1, eventRequest().build()) }
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessage("A cancelled event cannot be edited anymore!")
        }

        @Test
        fun `should fail when the new closing date has already passed`() {
            // given
            given(eventRepository.findById(1)).willReturn(Optional.of(openedEvent().build()))
            val request = eventRequest().open(now.minusDays(5)).closed(now.minusDays(1)).build()

            // when / then
            assertThatThrownBy { testee.updateEvent(1, request) }
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessage("An event with closed registrations cannot be edited anymore!")
        }

        @Test
        fun `should fail when the event has started`() {
            // given
            val started = event().open(now.minusDays(10)).closed(now.minusDays(5)).start(now.minusDays(1)).end(now.plusDays(1)).build()
            given(eventRepository.findById(1)).willReturn(Optional.of(started))
            val request = eventRequest().open(now.minusDays(10)).closed(now.minusDays(5)).build()

            // when / then
            assertThatThrownBy { testee.updateEvent(1, request) }
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessage("A started event cannot be edited anymore!")
        }

        @Test
        fun `should fail when the event is completed`() {
            // given
            val completed = event().open(now.minusDays(10)).closed(now.minusDays(5)).start(now.minusDays(3)).end(now.minusDays(1)).build()
            given(eventRepository.findById(1)).willReturn(Optional.of(completed))
            val request = eventRequest().open(now.minusDays(10)).closed(now.minusDays(5)).build()

            // when / then
            assertThatThrownBy { testee.updateEvent(1, request) }
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessage("A completed event cannot be edited anymore!")
        }

        @Test
        fun `should fully update the event when registrations are not yet open`() {
            // given
            given(eventRepository.findById(1)).willReturn(Optional.of(notYetOpenEvent().price(10.0).build()))
            givenSaveReturnsArgument()
            val request = eventRequest().name("newName").price(20.0).needsMobile().additionalFormRule("count * 2")
                .registrationLimit(50).communicationCC("cc@corp.com").build()

            // when
            val result = testee.updateEvent(1, request)

            // then
            assertThat(result.name).isEqualTo(request.name)
            assertThat(result.price).isEqualTo(request.price)
            assertThat(result.needsMobile).isTrue()
            assertThat(result.additionalFormRule).isEqualTo(request.additionalFormRule)
            assertThat(result.registrationLimit).isEqualTo(request.registrationLimit)
            assertThat(result.communicationCC).isEqualTo(request.communicationCC)
            assertThat(result.start).isEqualTo(request.start)
            then(registrationRepository).shouldHaveNoInteractions()
        }

        @Test
        fun `should fail when a cancellable event is made uncancellable`() {
            // given
            given(eventRepository.findById(1)).willReturn(Optional.of(notYetOpenEvent().cancellable(true).build()))
            val request = eventRequest().cancellable(false).build()

            // when / then
            assertThatThrownBy { testee.updateEvent(1, request) }
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessage("A previously cancellable event cannot be made uncancellable!")
        }

        @Test
        fun `should keep the price when registrations are already open`() {
            // given
            val event = openedEvent().price(10.0).build()
            given(eventRepository.findById(1)).willReturn(Optional.of(event))
            given(registrationRepository.countPaidRegistrationsByEvent(event)).willReturn(0)
            givenSaveReturnsArgument()
            val request = eventRequest().open(event.open).price(99.0).build()

            // when
            val result = testee.updateEvent(1, request)

            // then
            assertThat(result.price).isEqualTo(10.0)
        }

        @Test
        fun `should fail when the registration limit is lowered below the registration count`() {
            // given
            val event = openedEvent().build()
            given(eventRepository.findById(1)).willReturn(Optional.of(event))
            given(registrationRepository.countPaidRegistrationsByEvent(event)).willReturn(10)
            val request = eventRequest().open(event.open).registrationLimit(5).build()

            // when / then
            assertThatThrownBy { testee.updateEvent(1, request) }
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessage("The registration limit cannot be lowered below the current registration count!")
        }

        @Test
        fun `should accept removing the registration limit when registrations are open`() {
            // given
            val event = openedEvent().registrationLimit(10).build()
            given(eventRepository.findById(1)).willReturn(Optional.of(event))
            given(registrationRepository.countPaidRegistrationsByEvent(event)).willReturn(10)
            givenSaveReturnsArgument()
            val request = eventRequest().open(event.open).registrationLimit(null).build()

            // when
            val result = testee.updateEvent(1, request)

            // then
            assertThat(result.registrationLimit).isNull()
        }

        @Test
        fun `should accept a registration limit equal to the registration count`() {
            // given
            val event = openedEvent().build()
            given(eventRepository.findById(1)).willReturn(Optional.of(event))
            given(registrationRepository.countPaidRegistrationsByEvent(event)).willReturn(10)
            givenSaveReturnsArgument()
            val request = eventRequest().open(event.open).registrationLimit(10).build()

            // when / then
            assertThatCode { testee.updateEvent(1, request) }.doesNotThrowAnyException()
        }
    }

    @Nested
    inner class CancelEvent {

        @Test
        fun `should fail when the event is already cancelled`() {
            // given
            given(eventRepository.findById(1)).willReturn(Optional.of(openedEvent().cancelled(true).build()))

            // when / then
            assertThatThrownBy { testee.cancelEvent(1) }
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessage("This event is already cancelled!")
        }

        @Test
        fun `should fail when the event has started`() {
            // given
            val started = event().open(now.minusDays(10)).closed(now.minusDays(5)).start(now.minusDays(1)).end(now.plusDays(1)).build()
            given(eventRepository.findById(1)).willReturn(Optional.of(started))

            // when / then
            assertThatThrownBy { testee.cancelEvent(1) }
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessage("A started event cannot be cancelled anymore!")
        }

        @Test
        fun `should fail when the event is completed`() {
            // given
            val completed = event().open(now.minusDays(10)).closed(now.minusDays(5)).start(now.minusDays(3)).end(now.minusDays(1)).build()
            given(eventRepository.findById(1)).willReturn(Optional.of(completed))

            // when / then
            assertThatThrownBy { testee.cancelEvent(1) }
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessage("A completed event cannot be cancelled anymore!")
        }

        @Test
        fun `should refund or remove every paid registration and mark the event cancelled`() {
            // given
            val event = openedEvent().build()
            val registration = eventRegistration().event(event).paid().price(10.0).build()
            val freeRegistration = eventRegistration().event(event).paid().price(0.0).build()
            given(eventRepository.findById(1)).willReturn(Optional.of(event))
            given(registrationRepository.getRegistrationsByEvent(event)).willReturn(listOf(registration, freeRegistration))

            // when
            testee.cancelEvent(1)

            // then
            then(registrationService).should().refundOrRemove(registration)
            then(registrationService).should().refundOrRemove(freeRegistration)
            then(eventRepository).should().save(event)
            assertThat(event.cancelled).isTrue()
        }

        @Test
        fun `should mark the event cancelled without refunds when there are no registrations`() {
            // given
            val event = notYetOpenEvent().build()
            given(eventRepository.findById(1)).willReturn(Optional.of(event))
            given(registrationRepository.getRegistrationsByEvent(event)).willReturn(emptyList())

            // when
            testee.cancelEvent(1)

            // then
            then(registrationService).shouldHaveNoInteractions()
            assertThat(event.cancelled).isTrue()
        }

        @Test
        fun `should only refund or remove paid registrations`() {
            // given
            val event = openedEvent().build()
            val unpaid = eventRegistration().event(event).paid(false).price(10.0).build()
            given(eventRepository.findById(1)).willReturn(Optional.of(event))
            given(registrationRepository.getRegistrationsByEvent(event)).willReturn(listOf(unpaid))

            // when
            testee.cancelEvent(1)

            // then
            then(registrationService).shouldHaveNoInteractions()
        }
    }
}
