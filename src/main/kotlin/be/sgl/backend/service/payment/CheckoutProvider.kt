package be.sgl.backend.service.payment

import be.sgl.backend.entity.Payment
import be.sgl.backend.entity.user.User

interface CheckoutProvider {
    fun createCheckoutUrl(user: User, payment: Payment, webhook: String, redirectUrl: String, cancelUrl: String?): String
}