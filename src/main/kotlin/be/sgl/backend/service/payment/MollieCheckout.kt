package be.sgl.backend.service.payment

import be.sgl.backend.entity.Payment
import be.sgl.backend.entity.SimplifiedPaymentStatus
import be.sgl.backend.entity.user.User
import be.sgl.backend.util.appendRequestParameter
import be.woutschoovaerts.mollie.Client
import be.woutschoovaerts.mollie.data.common.Amount
import be.woutschoovaerts.mollie.data.payment.PaymentRequest
import be.woutschoovaerts.mollie.data.payment.PaymentStatus
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.util.*

@Service
@Primary
@ConditionalOnProperty(name = ["mollie.api.key"])
class MollieCheckout : CheckoutProvider {

    @Autowired
    private lateinit var mollieApiClient: Client
    @Value("\${app.base.url}")
    private lateinit var baseUrl: String

    override fun createCheckoutUrl(user: User, payment: Payment, domain: String): String {
        checkNotNull(payment.id)
        val request = PaymentRequest.builder()
            .customerId(Optional.ofNullable(user.customerId))
            .amount(Amount.builder()
                .value(BigDecimal(payment.price))
                .currency("EUR")
                .build())
            .description(payment.getDescription())
            .redirectUrl(appendRequestParameter("$baseUrl/$domain/confirmation", "order_id", payment.id))
            .webhookUrl(Optional.of("$baseUrl/api/$domain/updatePayment"))
            .metadata(mapOf("order_id" to payment.id))
            .build()
        val paymentResponse = mollieApiClient.payments().createPayment(request)
        payment.paymentId = paymentResponse.id
        return paymentResponse.links.checkout.href
    }

    override fun getPaymentStatusById(paymentId: String): SimplifiedPaymentStatus {
        val payment =  mollieApiClient.payments().getPayment(paymentId)
        if (!payment.amountRefunded.isEmpty || !payment.amountChargedBack.isEmpty) {
            return SimplifiedPaymentStatus.REFUNDED
        }
        return when (payment.status) {
            PaymentStatus.PAID -> SimplifiedPaymentStatus.PAID
            PaymentStatus.CANCELED, PaymentStatus.EXPIRED, PaymentStatus.FAILED -> SimplifiedPaymentStatus.CANCELLED
            else -> SimplifiedPaymentStatus.ONGOING
        }
    }
}