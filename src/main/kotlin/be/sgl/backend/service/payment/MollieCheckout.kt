package be.sgl.backend.service.payment

import be.sgl.backend.dto.Customer
import be.sgl.backend.entity.Payment
import be.sgl.backend.entity.SimplifiedPaymentStatus
import be.sgl.backend.util.appendRequestParameters
import be.woutschoovaerts.mollie.Client
import be.woutschoovaerts.mollie.data.common.Amount
import be.woutschoovaerts.mollie.data.customer.CustomerRequest
import be.woutschoovaerts.mollie.data.customer.CustomerResponse
import be.woutschoovaerts.mollie.data.payment.PaymentRequest
import be.woutschoovaerts.mollie.data.payment.PaymentStatus
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

    override fun createCheckoutUrl(customer: Customer, payment: Payment, domain: String): String {
        checkNotNull(payment.id)
        val customerId = customer.id ?: createCustomer(customer).id
        val request = PaymentRequest.builder()
            //.customerId(Optional.ofNullable(customerId))
            .amount(Amount.builder()
                .value(BigDecimal(payment.price))
                .currency("EUR")
                .build())
            .description(payment.getDescription())
            .redirectUrl(appendRequestParameters("$baseUrl/$domain/confirmation.html", "id" to payment.id, "order_id" to payment.id))
            .webhookUrl(Optional.of("$publicBaseUrl/api/$domain/updatePayment"))
            .metadata(mapOf("order_id" to payment.id))
            .build()
        val paymentResponse = mollieApiClient.payments().createPayment(request)
        payment.paymentId = paymentResponse.id
        return paymentResponse.links.checkout.href
    }

    override fun getCheckoutUrl(payment: Payment): String {
        checkNotNull(payment.paymentId)
        return mollieApiClient.payments().getPayment(payment.paymentId).links.checkout.href
    }

    override fun getPaymentStatusById(paymentId: String): SimplifiedPaymentStatus {
        val payment =  mollieApiClient.payments().getPayment(paymentId)
        val isRefunded = payment.amountRefunded.isPresent && payment.amountRefunded.get().value != BigDecimal.ZERO.setScale(2)
        if (isRefunded || !payment.amountChargedBack.isEmpty) {
            return SimplifiedPaymentStatus.REFUNDED
        }
        return when (payment.status) {
            PaymentStatus.PAID -> SimplifiedPaymentStatus.PAID
            PaymentStatus.CANCELED, PaymentStatus.EXPIRED, PaymentStatus.FAILED -> SimplifiedPaymentStatus.CANCELLED
            else -> SimplifiedPaymentStatus.ONGOING
        }
    }

    private fun createCustomer(customer: Customer): CustomerResponse {
        val request = CustomerRequest.builder()
            .name(Optional.of(customer.name))
            .email(Optional.of(customer.email))
            .build()
        return mollieApiClient.customers().createCustomer(request)
    }
}