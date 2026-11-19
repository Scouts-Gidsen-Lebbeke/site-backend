package be.sgl.backend.service.payment

import be.sgl.backend.entity.PaymentTestMother.activityRegistration
import be.sgl.backend.entity.SimplifiedPaymentStatus
import be.sgl.backend.entity.registrable.activity.ActivityRegistration
import be.sgl.backend.repository.activity.ActivityRegistrationRepository
import be.sgl.backend.service.MailService
import be.sgl.backend.util.anyValue
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.then
import org.mockito.Mock
import org.mockito.Mockito.never
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.test.util.ReflectionTestUtils

@ExtendWith(MockitoExtension::class)
class PaymentServiceTest {

    @Mock
    lateinit var paymentRepository: ActivityRegistrationRepository
    @Mock
    lateinit var checkoutProvider: CheckoutProvider
    @Mock
    lateinit var mailService: MailService

    lateinit var testee: RecordingPaymentService

    /**
     * Minimal concrete [PaymentService] recording which hook was triggered.
     */
    class RecordingPaymentService(
        override var paymentRepository: ActivityRegistrationRepository
    ) : PaymentService<ActivityRegistration, ActivityRegistrationRepository>() {
        val paid = mutableListOf<ActivityRegistration>()
        val canceled = mutableListOf<ActivityRegistration>()
        val refunded = mutableListOf<ActivityRegistration>()

        override fun handlePaymentPaid(payment: ActivityRegistration) { paid += payment }
        override fun handlePaymentCanceled(payment: ActivityRegistration) { canceled += payment }
        override fun handlePaymentRefunded(payment: ActivityRegistration) { refunded += payment }
    }

    @BeforeEach
    fun setUp() {
        testee = RecordingPaymentService(paymentRepository)
        ReflectionTestUtils.setField(testee, "checkoutProvider", checkoutProvider)
        ReflectionTestUtils.setField(testee, "mailService", mailService)
    }

    @Test
    fun `should ignore the update when the payment is unknown`() {
        // given
        given(paymentRepository.getByPaymentId("tr_1")).willReturn(null)

        // when
        testee.updatePayment("tr_1")

        // then
        then(checkoutProvider).shouldHaveNoInteractions()
    }

    @Test
    fun `should mark the payment paid and trigger the paid hook when the provider reports it paid`() {
        // given
        val payment = activityRegistration().paid(false).build()
        given(paymentRepository.getByPaymentId("tr_1")).willReturn(payment)
        given(checkoutProvider.getPaymentStatusById("tr_1")).willReturn(SimplifiedPaymentStatus.PAID)

        // when
        testee.updatePayment("tr_1")

        // then
        assertThat(payment.paid).isTrue()
        then(paymentRepository).should().save(payment)
        assertThat(testee.paid).containsExactly(payment)
    }

    @Test
    fun `should skip a paid update when the payment is already marked paid`() {
        // given
        val payment = activityRegistration().paid().build()
        given(paymentRepository.getByPaymentId("tr_1")).willReturn(payment)
        given(checkoutProvider.getPaymentStatusById("tr_1")).willReturn(SimplifiedPaymentStatus.PAID)

        // when
        testee.updatePayment("tr_1")

        // then
        then(paymentRepository).should(never()).save(anyValue())
        assertThat(testee.paid).isEmpty()
    }

    @Test
    fun `should delete the payment and trigger the cancel hook when the provider reports it cancelled`() {
        // given
        val payment = activityRegistration().paid(false).build()
        given(paymentRepository.getByPaymentId("tr_1")).willReturn(payment)
        given(checkoutProvider.getPaymentStatusById("tr_1")).willReturn(SimplifiedPaymentStatus.CANCELLED)

        // when
        testee.updatePayment("tr_1")

        // then
        then(paymentRepository).should().delete(payment)
        assertThat(testee.canceled).containsExactly(payment)
    }

    @Test
    fun `should fail when a paid payment is reported cancelled`() {
        // given
        val payment = activityRegistration().paid().build()
        given(paymentRepository.getByPaymentId("tr_1")).willReturn(payment)
        given(checkoutProvider.getPaymentStatusById("tr_1")).willReturn(SimplifiedPaymentStatus.CANCELLED)

        // when / then
        assertThatThrownBy { testee.updatePayment("tr_1") }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessage("This payment should never have been marked as paid!")
        then(paymentRepository).should(never()).delete(anyValue())
    }

    @Test
    fun `should delete the payment and trigger the refund hook when the provider reports it refunded`() {
        // given
        val payment = activityRegistration().paid(false).build()
        given(paymentRepository.getByPaymentId("tr_1")).willReturn(payment)
        given(checkoutProvider.getPaymentStatusById("tr_1")).willReturn(SimplifiedPaymentStatus.REFUNDED)

        // when
        testee.updatePayment("tr_1")

        // then
        then(paymentRepository).should().delete(payment)
        assertThat(testee.refunded).containsExactly(payment)
    }

    @Test
    fun `should do nothing when the payment is still ongoing`() {
        // given
        val payment = activityRegistration().paid(false).build()
        given(paymentRepository.getByPaymentId("tr_1")).willReturn(payment)
        given(checkoutProvider.getPaymentStatusById("tr_1")).willReturn(SimplifiedPaymentStatus.ONGOING)

        // when
        testee.updatePayment("tr_1")

        // then
        then(paymentRepository).should(never()).save(anyValue())
        then(paymentRepository).should(never()).delete(anyValue())
        assertThat(testee.paid + testee.canceled + testee.refunded).isEmpty()
    }
}
