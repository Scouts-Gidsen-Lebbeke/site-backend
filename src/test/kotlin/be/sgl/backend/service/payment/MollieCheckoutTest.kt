package be.sgl.backend.service.payment

import be.sgl.backend.dto.Customer
import be.sgl.backend.entity.PayableTestMother.activity
import be.sgl.backend.entity.PaymentTestMother.activityRegistration
import be.sgl.backend.entity.SimplifiedPaymentStatus
import be.sgl.backend.shared.util.argumentCaptor
import be.sgl.backend.shared.util.captureValue
import be.sgl.backend.shared.util.eqValue
import be.woutschoovaerts.mollie.Client
import be.woutschoovaerts.mollie.data.common.Amount
import be.woutschoovaerts.mollie.data.common.Link
import be.woutschoovaerts.mollie.data.common.Pagination
import be.woutschoovaerts.mollie.data.customer.CustomerRequest
import be.woutschoovaerts.mollie.data.customer.CustomerResponse
import be.woutschoovaerts.mollie.data.payment.PaymentLinks
import be.woutschoovaerts.mollie.data.payment.PaymentMethod
import be.woutschoovaerts.mollie.data.payment.PaymentRequest
import be.woutschoovaerts.mollie.data.payment.PaymentResponse
import be.woutschoovaerts.mollie.data.payment.PaymentStatus
import be.woutschoovaerts.mollie.data.refund.RefundListResponse
import be.woutschoovaerts.mollie.data.refund.RefundRequest
import be.woutschoovaerts.mollie.handler.payments.PaymentHandler
import be.woutschoovaerts.mollie.handler.payments.RefundHandler
import be.woutschoovaerts.mollie.handler.recurring.CustomerHandler
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.then
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.test.util.ReflectionTestUtils
import java.math.BigDecimal
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class MollieCheckoutTest {

    @Mock
    lateinit var mollieApiClient: Client
    @Mock
    lateinit var paymentHandler: PaymentHandler
    @Mock
    lateinit var refundHandler: RefundHandler
    @Mock
    lateinit var customerHandler: CustomerHandler
    @InjectMocks
    lateinit var testee: MollieCheckout

    private val baseUrl = "https://site.be"
    private val publicBaseUrl = "https://public.site.be"

    @BeforeEach
    fun setUp() {
        ReflectionTestUtils.setField(testee, "mollieApiClient", mollieApiClient)
        ReflectionTestUtils.setField(testee, "baseUrl", baseUrl)
        ReflectionTestUtils.setField(testee, "publicBaseUrl", publicBaseUrl)
    }

    private fun paymentResponse(status: PaymentStatus = PaymentStatus.OPEN, amountRefunded: Amount? = null) = PaymentResponse.builder()
        .id("tr_123")
        .status(status)
        .amountRefunded(Optional.ofNullable(amountRefunded))
        .links(PaymentLinks.builder().checkout(Link.builder().href("https://mollie/checkout").build()).build())
        .build()

    private fun refunds(count: Int) = Pagination.builder<RefundListResponse>().count(count).build()

    private fun eur(value: String) = Amount.builder().currency("EUR").value(BigDecimal(value)).build()

    @Test
    fun `should build the confirmation url with payable and order id`() {
        // given
        val payment = activityRegistration().id(7).build()

        // when
        val url = testee.createRedirectUrl(payment, "activities", 3)

        // then
        assertThat(url).isEqualTo("$baseUrl/activities/confirmation.html?id=3&order_id=7")
    }

    @Nested
    inner class CreateCheckoutUrl {

        @Test
        fun `should fail when the payment is not saved yet`() {
            // given
            val payment = activityRegistration().id(null).build()

            // when / then
            assertThatThrownBy { testee.createCheckoutUrl(Customer("Jan Peeters", "jan@corp.com", "cst_1"), payment, "activities", 3) }
                .isInstanceOf(IllegalStateException::class.java)
        }

        @Test
        fun `should create a bancontact payment for an existing customer and store the payment id`() {
            // given
            val payment = activityRegistration().id(7).price(12.5).paymentId(null).activity(activity().name("Kamp").build()).build()
            val captor = argumentCaptor<PaymentRequest>()
            given(mollieApiClient.payments()).willReturn(paymentHandler)
            given(paymentHandler.createPayment(captureValue(captor))).willReturn(paymentResponse())

            // when
            val url = testee.createCheckoutUrl(Customer("Jan Peeters", "jan@corp.com", "cst_1"), payment, "activities", 3)

            // then
            assertThat(url).isEqualTo("https://mollie/checkout")
            assertThat(payment.paymentId).isEqualTo("tr_123")
            val request = captor.value
            assertThat(request.customerId).contains("cst_1")
            assertThat(request.amount.currency).isEqualTo("EUR")
            assertThat(request.amount.value).isEqualByComparingTo("12.5")
            assertThat(request.method).contains(listOf(PaymentMethod.BANCONTACT))
            assertThat(request.description).isEqualTo("Kamp")
            assertThat(request.redirectUrl).isEqualTo("$baseUrl/activities/confirmation.html?id=3&order_id=7")
            assertThat(request.webhookUrl).contains("$publicBaseUrl/api/activities/updatePayment")
            assertThat(request.metadata).isEqualTo(mapOf("order_id" to 7))
            then(mollieApiClient).should(org.mockito.Mockito.never()).customers()
        }

        @Test
        fun `should create a mollie customer first when the customer has no id yet`() {
            // given
            val payment = activityRegistration().id(7).price(12.5).build()
            val customerCaptor = argumentCaptor<CustomerRequest>()
            val paymentCaptor = argumentCaptor<PaymentRequest>()
            given(mollieApiClient.customers()).willReturn(customerHandler)
            given(customerHandler.createCustomer(captureValue(customerCaptor))).willReturn(CustomerResponse.builder().id("cst_new").build())
            given(mollieApiClient.payments()).willReturn(paymentHandler)
            given(paymentHandler.createPayment(captureValue(paymentCaptor))).willReturn(paymentResponse())

            // when
            testee.createCheckoutUrl(Customer("Jan Peeters", "jan@corp.com"), payment, "events", 3)

            // then
            assertThat(customerCaptor.value.name).contains("Jan Peeters")
            assertThat(customerCaptor.value.email).contains("jan@corp.com")
            assertThat(paymentCaptor.value.customerId).contains("cst_new")
        }
    }

    @Nested
    inner class GetCheckoutUrl {

        @Test
        fun `should fail when the payment has no payment id`() {
            // given
            val payment = activityRegistration().paymentId(null).build()

            // when / then
            assertThatThrownBy { testee.getCheckoutUrl(payment) }
                .isInstanceOf(IllegalStateException::class.java)
        }

        @Test
        fun `should return the checkout link of the existing payment`() {
            // given
            val payment = activityRegistration().paymentId("tr_123").build()
            given(mollieApiClient.payments()).willReturn(paymentHandler)
            given(paymentHandler.getPayment("tr_123")).willReturn(paymentResponse())

            // when
            val url = testee.getCheckoutUrl(payment)

            // then
            assertThat(url).isEqualTo("https://mollie/checkout")
        }
    }

    @Nested
    inner class GetPaymentStatusById {

        @BeforeEach
        fun handlers() {
            given(mollieApiClient.payments()).willReturn(paymentHandler)
            given(mollieApiClient.refunds()).willReturn(refundHandler)
        }

        @Test
        fun `should be refunded when refunds exist and an amount was refunded`() {
            // given
            given(paymentHandler.getPayment("tr_123")).willReturn(paymentResponse(PaymentStatus.PAID, eur("9.00")))
            given(refundHandler.listRefunds("tr_123")).willReturn(refunds(1))

            // when
            val status = testee.getPaymentStatusById("tr_123")

            // then
            assertThat(status).isEqualTo(SimplifiedPaymentStatus.REFUNDED)
        }

        @Test
        fun `should be ongoing when refunds exist but nothing was refunded yet`() {
            // given
            given(paymentHandler.getPayment("tr_123")).willReturn(paymentResponse(PaymentStatus.PAID, eur("0.00")))
            given(refundHandler.listRefunds("tr_123")).willReturn(refunds(1))

            // when
            val status = testee.getPaymentStatusById("tr_123")

            // then
            assertThat(status).isEqualTo(SimplifiedPaymentStatus.ONGOING)
        }

        @Test
        fun `should be ongoing when refunds exist but no refunded amount is known`() {
            // given
            given(paymentHandler.getPayment("tr_123")).willReturn(paymentResponse(PaymentStatus.PAID))
            given(refundHandler.listRefunds("tr_123")).willReturn(refunds(1))

            // when
            val status = testee.getPaymentStatusById("tr_123")

            // then
            assertThat(status).isEqualTo(SimplifiedPaymentStatus.ONGOING)
        }

        @Test
        fun `should be paid when there are no refunds and the payment is paid`() {
            // given
            given(paymentHandler.getPayment("tr_123")).willReturn(paymentResponse(PaymentStatus.PAID))
            given(refundHandler.listRefunds("tr_123")).willReturn(refunds(0))

            // when
            val status = testee.getPaymentStatusById("tr_123")

            // then
            assertThat(status).isEqualTo(SimplifiedPaymentStatus.PAID)
        }

        @ParameterizedTest
        @EnumSource(value = PaymentStatus::class, names = ["CANCELED", "EXPIRED", "FAILED"])
        fun `should be cancelled when the payment is cancelled, expired or failed`(mollieStatus: PaymentStatus) {
            // given
            given(paymentHandler.getPayment("tr_123")).willReturn(paymentResponse(mollieStatus))
            given(refundHandler.listRefunds("tr_123")).willReturn(refunds(0))

            // when
            val status = testee.getPaymentStatusById("tr_123")

            // then
            assertThat(status).isEqualTo(SimplifiedPaymentStatus.CANCELLED)
        }

        @ParameterizedTest
        @EnumSource(value = PaymentStatus::class, names = ["OPEN", "PENDING", "AUTHORIZED"])
        fun `should be ongoing when the payment is not finished yet`(mollieStatus: PaymentStatus) {
            // given
            given(paymentHandler.getPayment("tr_123")).willReturn(paymentResponse(mollieStatus))
            given(refundHandler.listRefunds("tr_123")).willReturn(refunds(0))

            // when
            val status = testee.getPaymentStatusById("tr_123")

            // then
            assertThat(status).isEqualTo(SimplifiedPaymentStatus.ONGOING)
        }
    }

    @Nested
    inner class RefundPayment {

        @Test
        fun `should fail when the payment is not paid`() {
            // given
            val payment = activityRegistration().paid(false).price(10.0).build()

            // when / then
            assertThatThrownBy { testee.refundPayment(payment) }
                .isInstanceOf(IllegalStateException::class.java)
            then(mollieApiClient).shouldHaveNoInteractions()
        }

        @Test
        fun `should not refund when the price does not exceed the refund cost`() {
            // given
            val payment = activityRegistration().paid().price(testee.getRefundCost(activityRegistration().build())).build()

            // when
            val refunded = testee.refundPayment(payment)

            // then
            assertThat(refunded).isFalse()
            then(mollieApiClient).shouldHaveNoInteractions()
        }

        @Test
        fun `should refund the price minus the refund cost`() {
            // given
            val payment = activityRegistration().paid().price(10.0).paymentId("tr_123")
                .activity(activity().name("Kamp").build()).build()
            val captor = argumentCaptor<RefundRequest>()
            given(mollieApiClient.refunds()).willReturn(refundHandler)

            // when
            val refunded = testee.refundPayment(payment)

            // then
            assertThat(refunded).isTrue()
            then(refundHandler).should().createRefund(eqValue("tr_123"), captureValue(captor))
            assertThat(captor.value.amount.value).isEqualByComparingTo(BigDecimal.valueOf(payment.price - testee.getRefundCost(payment)))
            assertThat(captor.value.amount.currency).isEqualTo("EUR")
            assertThat(captor.value.description).contains("Kamp")
        }
    }
}
