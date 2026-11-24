package be.sgl.backend.service.payment

import be.sgl.backend.dto.Customer
import be.sgl.backend.entity.Payment
import be.sgl.backend.entity.SimplifiedPaymentStatus

interface CheckoutProvider {
    fun createRedirectUrl(payment: Payment, domain: String, payableId: Int?): String
    fun createCheckoutUrl(customer: Customer, payment: Payment, domain: String, payableId: Int?): String
    fun getCheckoutUrl(payment: Payment): String
    fun getPaymentStatusById(paymentId: String): SimplifiedPaymentStatus
    /**
     * Requests a refund of the [payment], minus the refund cost.
     * Returns whether a refund was actually requested, which is not the case when nothing is left after the refund cost.
     */
    fun refundPayment(payment: Payment): Boolean
    fun getRefundCost(payment: Payment) = 1.0
}