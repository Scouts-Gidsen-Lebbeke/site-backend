package be.sgl.backend.service.payment

import be.sgl.backend.dto.Customer
import be.sgl.backend.entity.Payment
import be.sgl.backend.entity.SimplifiedPaymentStatus

interface CheckoutProvider {
    fun createRedirectUrl(payment: Payment, domain: String, payableId: Int?): String
    fun createCheckoutUrl(customer: Customer, payment: Payment, domain: String, payableId: Int?): String
    fun getCheckoutUrl(payment: Payment): String
    fun getPaymentStatusById(paymentId: String): SimplifiedPaymentStatus
}