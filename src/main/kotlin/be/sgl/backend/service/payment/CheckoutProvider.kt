package be.sgl.backend.service.payment

import be.sgl.backend.entity.Payment
import be.sgl.backend.entity.SimplifiedPaymentStatus
import be.sgl.backend.entity.user.User

interface CheckoutProvider {
    fun createCheckoutUrl(user: User, payment: Payment, domain: String): String
    fun getCheckoutUrl(payment: Payment): String
    fun getPaymentStatusById(paymentId: String): SimplifiedPaymentStatus
}