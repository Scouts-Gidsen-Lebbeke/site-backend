package be.sgl.backend.service.payment

import be.sgl.backend.dto.Customer
import be.sgl.backend.entity.Payment
import be.sgl.backend.entity.SimplifiedPaymentStatus
import be.sgl.backend.util.appendRequestParameters
import be.woutschoovaerts.mollie.Client
import be.woutschoovaerts.mollie.data.common.Amount
import be.woutschoovaerts.mollie.data.customer.CustomerRequest
import be.woutschoovaerts.mollie.data.customer.CustomerResponse
import be.woutschoovaerts.mollie.data.payment.PaymentMethod
import be.woutschoovaerts.mollie.data.payment.PaymentRequest
import be.woutschoovaerts.mollie.data.payment.PaymentStatus
import be.woutschoovaerts.mollie.data.refund.RefundRequest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.util.*

@Service
@ConditionalOnBean(Client::class)
class MollieCheckout : CheckoutProvider {

    @Autowired
    private lateinit var mollieApiClient: Client
    @Value("\${spring.application.base-url}")
    private lateinit var baseUrl: String
    @Value("\${spring.application.public-base-url}")
    private lateinit var publicBaseUrl: String

    override fun createRedirectUrl(payment: Payment, domain: String, payableId: Int?): String {
        return appendRequestParameters("$baseUrl/$domain/confirmation.html", "id" to payableId, "order_id" to payment.id)
    }

    override fun createCheckoutUrl(customer: Customer, payment: Payment, domain: String, payableId: Int?): String {
        checkNotNull(payment.id)
        val customerId = customer.id ?: createCustomer(customer).id
        val request = PaymentRequest.builder()
            .customerId(Optional.ofNullable(customerId))
            .amount(Amount.builder()
                .value(BigDecimal(payment.price))
                .currency("EUR")
                .build())
            .method(Optional.of(listOf(PaymentMethod.BANCONTACT)))
            .description(payment.getDescription())
            .redirectUrl(createRedirectUrl(payment, domain, payableId))
            .webhookUrl(Optional.of("$publicBaseUrl/api/$domain/updatePayment"))
            .metadata(mapOf("order_id" to payment.id))
            .build()
        val paymentResponse = mollieApiClient.payments().createPayment(request)
        payment.paymentId = paymentResponse.id
        return paymentResponse.links.checkout.href
    }

    private fun createCustomer(customer: Customer): CustomerResponse {
        val request = CustomerRequest.builder()
            .name(Optional.of(customer.name))
            .email(Optional.of(customer.email))
            .build()
        return mollieApiClient.customers().createCustomer(request)
    }

    override fun getCheckoutUrl(payment: Payment): String {
        checkNotNull(payment.paymentId)
        return mollieApiClient.payments().getPayment(payment.paymentId).links.checkout.href
    }

    override fun getPaymentStatusById(paymentId: String): SimplifiedPaymentStatus {
        val payment =  mollieApiClient.payments().getPayment(paymentId)
        if (hasRefunds(paymentId)) {
            if (payment.amountRefunded.isPresent && payment.amountRefunded.get().value > BigDecimal.ZERO) {
                return SimplifiedPaymentStatus.REFUNDED
            }
            return SimplifiedPaymentStatus.ONGOING
        }
        return when (payment.status) {
            PaymentStatus.PAID -> SimplifiedPaymentStatus.PAID
            PaymentStatus.CANCELED, PaymentStatus.EXPIRED, PaymentStatus.FAILED -> SimplifiedPaymentStatus.CANCELLED
            else -> SimplifiedPaymentStatus.ONGOING
        }
    }

    private fun hasRefunds(paymentId: String): Boolean {
        return mollieApiClient.refunds().listRefunds(paymentId).count > 0
    }

    override fun refundPayment(payment: Payment) {
        check(payment.paid)
        val request = RefundRequest.builder()
            .description(Optional.of(payment.getDescription()))
            .amount(Amount.builder()
                .value(BigDecimal(payment.price - 1))
                .currency("EUR")
                .build()
            ).build()
        mollieApiClient.refunds().createRefund(payment.paymentId, request)
    }
}