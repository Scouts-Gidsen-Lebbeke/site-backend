package be.sgl.backend.service.registrable.event

import be.sgl.backend.dto.Customer
import be.sgl.backend.dto.registrable.event.CreateEventRegistrationRequestTestMother.eventRegistrationRequest
import be.sgl.backend.entity.PayableTestMother.event
import be.sgl.backend.entity.PaymentTestMother.eventRegistration
import be.sgl.backend.entity.registrable.event.EventRegistration
import be.sgl.backend.entity.user.UserTestMother.user
import be.sgl.backend.exception.EventNotFoundException
import be.sgl.backend.exception.EventRegistrationNotFoundException
import be.sgl.backend.repository.event.EventRegistrationRepository
import be.sgl.backend.repository.event.EventRepository
import be.sgl.backend.repository.user.UserRepository
import be.sgl.backend.service.MailService
import be.sgl.backend.service.payment.CheckoutProvider
import be.sgl.backend.service.registrable.CalculatePriceFromAdditionalData
import be.sgl.backend.util.anyValue
import be.sgl.backend.util.argumentCaptor
import be.sgl.backend.util.captureValue
import be.sgl.backend.util.eqValue
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Answers
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.then
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.never
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.test.util.ReflectionTestUtils
import java.time.LocalDateTime
import java.util.Optional
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class EventRegistrationServiceTest {

    @Mock
    lateinit var paymentRepository: EventRegistrationRepository
    @Mock
    lateinit var eventRepository: EventRepository
    @Mock
    lateinit var userRepository: UserRepository
    @Mock
    lateinit var calculatePriceFromAdditionalData: CalculatePriceFromAdditionalData
    @Mock
    lateinit var checkoutProvider: CheckoutProvider
    @Mock
    lateinit var mailService: MailService
    @Mock(answer = Answers.RETURNS_SELF)
    lateinit var mailBuilder: MailService.MailBuilder
    @InjectMocks
    lateinit var testee: EventRegistrationService

    private val now = LocalDateTime.now()

    @BeforeEach
    fun injectInheritedCollaborators() {
        ReflectionTestUtils.setField(testee, "checkoutProvider", checkoutProvider)
        ReflectionTestUtils.setField(testee, "mailService", mailService)
    }

    private fun openedEvent() = event().id(4).open(now.minusDays(1)).closed(now.plusDays(5))
        .start(now.plusDays(10)).end(now.plusDays(11))

    private fun givenSaveReturnsArgument() {
        given(paymentRepository.save(anyValue<EventRegistration>())).willAnswer { it.getArgument<EventRegistration>(0) }
    }

    @Nested
    inner class CreatePaymentForEvent {

        @Test
        fun `should fail when the event is not found`() {
            // given
            given(eventRepository.findById(4)).willReturn(Optional.empty())

            // when / then
            assertThatThrownBy { testee.createPaymentForEvent(4, eventRegistrationRequest().build(), null) }
                .isInstanceOf(EventNotFoundException::class.java)
        }

        @Test
        fun `should fail when the event needs a mobile number and none is given`() {
            // given
            given(eventRepository.findById(4)).willReturn(Optional.of(openedEvent().needsMobile().build()))

            // when / then
            assertThatThrownBy { testee.createPaymentForEvent(4, eventRegistrationRequest().build(), null) }
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessage("No valid mobile number provided!")
        }

        @Test
        fun `should fail when the event limit is reached`() {
            // given
            val event = openedEvent().registrationLimit(20).build()
            given(eventRepository.findById(4)).willReturn(Optional.of(event))
            given(paymentRepository.countPaidRegistrationsByEvent(event)).willReturn(event.registrationLimit)

            // when / then
            assertThatThrownBy { testee.createPaymentForEvent(4, eventRegistrationRequest().build(), null) }
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessage("The limit for this event is reached!")
        }

        @Test
        fun `should accept a registration when the event limit is not reached yet`() {
            // given
            val event = openedEvent().registrationLimit(20).price(0.0).build()
            given(eventRepository.findById(4)).willReturn(Optional.of(event))
            given(paymentRepository.countPaidRegistrationsByEvent(event)).willReturn(event.registrationLimit!! - 1)
            givenSaveReturnsArgument()
            given(checkoutProvider.createRedirectUrl(anyValue(), eqValue("events"), eqValue(event.id))).willReturn("redirect")

            // when
            val url = testee.createPaymentForEvent(4, eventRegistrationRequest().build(), null)

            // then
            assertThat(url).isEqualTo("redirect")
        }

        @Test
        fun `should mark a free registration paid and return the confirmation url`() {
            // given
            val event = openedEvent().price(0.0).needsMobile().build()
            val request = eventRegistrationRequest().mobile("+32470123456").build()
            given(eventRepository.findById(4)).willReturn(Optional.of(event))
            givenSaveReturnsArgument()
            given(checkoutProvider.createRedirectUrl(anyValue(), eqValue("events"), eqValue(event.id))).willReturn("redirect")
            val captor = argumentCaptor<EventRegistration>()

            // when
            val url = testee.createPaymentForEvent(4, request, null)

            // then
            assertThat(url).isEqualTo("redirect")
            then(checkoutProvider).should().createRedirectUrl(captureValue(captor), eqValue("events"), eqValue(event.id))
            assertThat(captor.value.paid).isTrue()
            assertThat(captor.value.mobile).isEqualTo(request.mobile)
            then(checkoutProvider).should(never()).createCheckoutUrl(anyValue(), anyValue(), anyValue(), anyValue())
        }

        @Test
        fun `should charge the event price plus additional costs for an anonymous registration`() {
            // given
            val event = openedEvent().price(10.0).build()
            val request = eventRegistrationRequest().firstName("Jan").name("Peeters").email("jan@corp.com")
                .additionalData("""{"count":"2"}""").build()
            val additionalPrice = 5.0
            given(eventRepository.findById(4)).willReturn(Optional.of(event))
            given(calculatePriceFromAdditionalData.execute(event, request.additionalData)).willReturn(additionalPrice)
            givenSaveReturnsArgument()
            val captor = argumentCaptor<EventRegistration>()
            given(checkoutProvider.createCheckoutUrl(eqValue(Customer("Jan Peeters", "jan@corp.com")), captureValue(captor), eqValue("events"), eqValue(event.id)))
                .willReturn("checkout")

            // when
            val url = testee.createPaymentForEvent(4, request, null)

            // then
            assertThat(url).isEqualTo("checkout")
            val registration = captor.value
            assertThat(registration.price).isEqualTo(event.price + additionalPrice)
            assertThat(registration.subscribable).isSameAs(event)
            assertThat(registration.user).isNull()
            assertThat(registration.paid).isFalse()
        }

        @Test
        fun `should link the registration to the logged in user and use it as customer`() {
            // given
            val event = openedEvent().price(10.0).build()
            val user = user().username("member").customerId("cst_1").build()
            given(eventRepository.findById(4)).willReturn(Optional.of(event))
            given(userRepository.findByUsername("member")).willReturn(user)
            givenSaveReturnsArgument()
            val captor = argumentCaptor<EventRegistration>()
            given(checkoutProvider.createCheckoutUrl(eqValue(Customer(user)), captureValue(captor), eqValue("events"), eqValue(event.id)))
                .willReturn("checkout")

            // when
            testee.createPaymentForEvent(4, eventRegistrationRequest().build(), "member")

            // then
            assertThat(captor.value.user).isSameAs(user)
        }

        @Test
        fun `should fail when registrations for the event are closed`() {
            // given
            val closedEvent = event().open(now.minusDays(10)).closed(now.minusDays(1)).start(now.plusDays(5)).end(now.plusDays(6)).build()
            given(eventRepository.findById(4)).willReturn(Optional.of(closedEvent))

            // when / then
            assertThatThrownBy { testee.createPaymentForEvent(4, eventRegistrationRequest().build(), null) }
                .isInstanceOf(IllegalStateException::class.java)
        }
    }

    @Nested
    inner class PaymentHooks {

        @Test
        fun `should not send a confirmation when the event does not require it`() {
            // given
            val registration = eventRegistration().event(event().sendConfirmation(false).build()).build()

            // when
            testee.handlePaymentPaid(registration)

            // then
            then(mailService).shouldHaveNoInteractions()
        }

        @Test
        fun `should send a confirmation to the registration email when the event requires it`() {
            // given
            val event = event().name("Quiz").sendConfirmation(true).communicationCC("cc@corp.com").build()
            val registration = eventRegistration().event(event).firstName("Jan").name("Peeters").email("jan@corp.com").price(10.0).build()
            given(mailService.builder()).willReturn(mailBuilder)

            // when
            testee.handlePaymentPaid(registration)

            // then
            then(mailBuilder).should().to("jan@corp.com")
            then(mailBuilder).should().template("event-confirmation.html", mapOf(
                "customer" to "Jan Peeters",
                "price" to registration.price,
                "eventName" to event.name,
                "additionalData" to null
            ))
            then(mailBuilder).should().cc("cc@corp.com")
            then(mailBuilder).should().send()
        }

        @Test
        fun `should send a cancellation mail with the refunded amount`() {
            // given
            val event = event().name("Quiz").build()
            val registration = eventRegistration().event(event).firstName("Jan").name("Peeters").price(10.0).build()
            given(mailService.builder()).willReturn(mailBuilder)
            given(checkoutProvider.getRefundCost(registration)).willReturn(1.0)

            // when
            testee.handlePaymentRefunded(registration)

            // then
            then(mailBuilder).should().template("cancel-event-confirmation.html", mapOf(
                "customer" to "Jan Peeters",
                "price" to registration.price - 1.0,
                "eventName" to event.name
            ))
            then(mailBuilder).should().send()
        }

        @Test
        fun `should not mention a negative refund when cancelling a free registration`() {
            // given
            val event = event().name("Quiz").build()
            val registration = eventRegistration().event(event).firstName("Jan").name("Peeters").price(0.0).build()
            given(mailService.builder()).willReturn(mailBuilder)
            given(checkoutProvider.getRefundCost(registration)).willReturn(1.0)

            // when
            testee.handlePaymentRefunded(registration)

            // then
            then(mailBuilder).should().template("cancel-event-confirmation.html", mapOf(
                "customer" to "Jan Peeters",
                "price" to 0.0,
                "eventName" to event.name
            ))
        }
    }

    @Nested
    inner class MarkRegistrationAsCompleted {

        @Test
        fun `should fail when the registration is not found`() {
            // given
            given(paymentRepository.findById(1)).willReturn(Optional.empty())

            // when / then
            assertThatThrownBy { testee.markRegistrationAsCompleted(1) }
                .isInstanceOf(EventRegistrationNotFoundException::class.java)
                .hasMessage("Event registration not found")
        }

        @Test
        fun `should fail when the registration is not paid`() {
            // given
            given(paymentRepository.findById(1)).willReturn(Optional.of(eventRegistration().paid(false).build()))

            // when / then
            assertThatThrownBy { testee.markRegistrationAsCompleted(1) }
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessage("Only a paid event can be marked as completed!")
        }

        @Test
        fun `should do nothing when the registration is already completed`() {
            // given
            given(paymentRepository.findById(1)).willReturn(Optional.of(eventRegistration().paid().completed(true).build()))

            // when
            testee.markRegistrationAsCompleted(1)

            // then
            then(paymentRepository).should(never()).save(anyValue())
        }

        @Test
        fun `should fail when the event starts more than one hour from now`() {
            // given
            val event = event().start(now.plusHours(2)).build()
            given(paymentRepository.findById(1)).willReturn(Optional.of(eventRegistration().paid().completed(false).event(event).build()))

            // when / then
            assertThatThrownBy { testee.markRegistrationAsCompleted(1) }
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessage("Registrations can only be completed starting one hour before the event!")
        }

        @Test
        fun `should mark the registration completed without mail when no completion confirmation is required`() {
            // given
            val registration = eventRegistration().paid().completed(false).event(event().start(now.plusMinutes(30)).build()).build()
            given(paymentRepository.findById(1)).willReturn(Optional.of(registration))

            // when
            testee.markRegistrationAsCompleted(1)

            // then
            assertThat(registration.completed).isTrue()
            then(paymentRepository).should().save(registration)
            then(mailService).shouldHaveNoInteractions()
        }

        @Test
        fun `should send a completion mail when the event requires completion confirmation`() {
            // given
            val event = event().name("Quiz").start(now.minusHours(1)).sendCompleteConfirmation(true).build()
            val registration = eventRegistration().paid().completed(false).event(event).firstName("Jan").name("Peeters").build()
            given(paymentRepository.findById(1)).willReturn(Optional.of(registration))
            given(mailService.builder()).willReturn(mailBuilder)

            // when
            testee.markRegistrationAsCompleted(1)

            // then
            then(mailBuilder).should().template("event-completion.html", mapOf("customer" to "Jan Peeters", "eventName" to event.name))
            then(mailBuilder).should().send()
        }
    }

    @Nested
    inner class CancelRegistration {

        @Test
        fun `should fail when the registration is not paid`() {
            // given
            given(paymentRepository.findById(1)).willReturn(Optional.of(eventRegistration().paid(false).build()))

            // when / then
            assertThatThrownBy { testee.cancelRegistration(1) }
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessage("Only a paid event registration can be cancelled!")
        }

        @Test
        fun `should fail when registrations are no longer open`() {
            // given
            val closedEvent = event().open(now.minusDays(10)).closed(now.minusDays(1)).start(now.plusDays(5)).end(now.plusDays(6)).build()
            given(paymentRepository.findById(1)).willReturn(Optional.of(eventRegistration().paid().event(closedEvent).build()))

            // when / then
            assertThatThrownBy { testee.cancelRegistration(1) }
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessage("Cancellation is only possible when registrations are still open!")
        }

        @Test
        fun `should refund a paid registration and mark it refunded`() {
            // given
            val registration = eventRegistration().paid().price(10.0).event(openedEvent().build()).build()
            given(paymentRepository.findById(1)).willReturn(Optional.of(registration))
            given(paymentRepository.save(registration)).willReturn(registration)

            // when
            testee.cancelRegistration(1)

            // then
            then(checkoutProvider).should().refundPayment(registration)
            then(paymentRepository).should().save(registration)
            assertThat(registration.paid).isFalse()
        }

        @Test
        fun `should delete a free registration and send the cancellation mail`() {
            // given
            val registration = eventRegistration().paid().price(0.0).event(openedEvent().build()).build()
            given(paymentRepository.findById(1)).willReturn(Optional.of(registration))
            given(mailService.builder()).willReturn(mailBuilder)

            // when
            testee.cancelRegistration(1)

            // then
            then(paymentRepository).should().delete(registration)
            then(checkoutProvider).should(never()).refundPayment(anyValue())
            then(mailBuilder).should().send()
        }

        @Test
        fun `should cancel the registration identified by its cancel token`() {
            // given
            val registration = eventRegistration().paid().price(10.0).event(openedEvent().build()).build()
            val token = registration.cancelToken!!
            given(paymentRepository.findByCancelToken(token)).willReturn(registration)
            given(paymentRepository.save(registration)).willReturn(registration)

            // when
            testee.cancelRegistrationByToken(token)

            // then
            then(checkoutProvider).should().refundPayment(registration)
            assertThat(registration.paid).isFalse()
        }

        @Test
        fun `should fail when the cancel token is unknown`() {
            // given
            given(paymentRepository.findByCancelToken("unknown")).willReturn(null)

            // when / then
            assertThatThrownBy { testee.cancelRegistrationByToken("unknown") }
                .isInstanceOf(EventRegistrationNotFoundException::class.java)
            then(checkoutProvider).should(never()).refundPayment(anyValue())
        }

        @Test
        fun `should give every registration its own random cancel token`() {
            // when
            val first = eventRegistration().build()
            val second = eventRegistration().build()

            // then
            assertThat(first.cancelToken).isNotNull().isNotEqualTo(second.cancelToken)
            assertThat(UUID.fromString(first.cancelToken)).isNotNull()
        }

        @Test
        fun `should fail when the registrant cancels a registration of a non cancellable event by token`() {
            // given
            val registration = eventRegistration().paid().price(10.0).event(openedEvent().cancellable(false).build()).build()
            val token = registration.cancelToken!!
            given(paymentRepository.findByCancelToken(token)).willReturn(registration)

            // when / then
            assertThatThrownBy { testee.cancelRegistrationByToken(token) }
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessage("Registrations for this event cannot be cancelled!")
            then(checkoutProvider).should(never()).refundPayment(anyValue())
        }

        @Test
        fun `should let staff refund a registration of a non cancellable event`() {
            // given
            val registration = eventRegistration().paid().price(10.0).event(openedEvent().cancellable(false).build()).build()
            given(paymentRepository.findById(1)).willReturn(Optional.of(registration))
            given(paymentRepository.save(registration)).willReturn(registration)

            // when
            testee.cancelRegistration(1)

            // then
            then(checkoutProvider).should().refundPayment(registration)
        }
    }
}
