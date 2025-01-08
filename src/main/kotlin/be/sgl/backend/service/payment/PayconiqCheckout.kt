package be.sgl.backend.service.payment

import be.sgl.backend.entity.Payment
import be.sgl.backend.entity.user.User
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service

@Service
@ConditionalOnProperty(name = ["payconiq.api.key"])
class PayconiqCheckout : CheckoutProvider {
    override fun createCheckoutUrl(user: User, payment: Payment, webhook: String, redirectUrl: String, cancelUrl: String?): String {
        TODO("Not yet implemented")
    }
}