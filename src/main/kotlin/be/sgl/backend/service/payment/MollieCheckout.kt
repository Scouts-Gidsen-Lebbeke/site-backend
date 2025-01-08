package be.sgl.backend.service.payment

import be.sgl.backend.entity.Payment
import be.sgl.backend.entity.user.User
import be.sgl.backend.util.appendRequestParameter
import be.woutschoovaerts.mollie.Client
import be.woutschoovaerts.mollie.data.common.Amount
import be.woutschoovaerts.mollie.data.payment.PaymentRequest
import org.springframework.beans.factory.annotation.Autowired
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

    override fun createCheckoutUrl(user: User, payment: Payment, webhook: String, redirectUrl: String, cancelUrl: String?): String {
        checkNotNull(payment.id)
        val request = PaymentRequest.builder()
            .customerId(Optional.ofNullable(user.customerId))
            .amount(Amount.builder()
                .value(BigDecimal(payment.price))
                .currency("EUR")
                .build())
            .description(payment.getDescription())
            .redirectUrl(appendRequestParameter(redirectUrl, "order_id", payment.id))
            .cancelUrl(Optional.ofNullable(cancelUrl))
            .webhookUrl(Optional.of(webhook))
            .metadata(mapOf("order_id" to payment.id))
            .build()
        val paymentResponse = mollieApiClient.payments().createPayment(request)
        payment.paymentId = paymentResponse.id
        return paymentResponse.links.checkout.href
    }
}