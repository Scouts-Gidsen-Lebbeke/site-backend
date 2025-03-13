package be.sgl.backend.service.payment

import be.sgl.backend.entity.Payment
import be.sgl.backend.entity.SimplifiedPaymentStatus

interface CheckoutProvider {
    fun createCheckoutUrl(customer: String?, payment: Payment, domain: String): String
    fun getCheckoutUrl(payment: Payment): String
    fun getPaymentStatusById(paymentId: String): SimplifiedPaymentStatus
}