package be.sgl.backend.config

import be.sgl.backend.dto.Customer
import be.sgl.backend.entity.Payment
import be.sgl.backend.entity.SimplifiedPaymentStatus
import be.sgl.backend.service.payment.CheckoutProvider
import org.springframework.stereotype.Service

@Service
class MockedCheckoutProvider : CheckoutProvider {

    override fun createRedirectUrl(payment: Payment, domain: String, payableId: Int?): String {
        return DEFAULT_CHECKOUT_URL
    }

    override fun createCheckoutUrl(customer: Customer, payment: Payment, domain: String, payableId: Int?): String {
        payment.paymentId = SimplifiedPaymentStatus.PAID.name
        return DEFAULT_CHECKOUT_URL
    }

    override fun getCheckoutUrl(payment: Payment): String {
        checkNotNull(payment.paymentId)
        return DEFAULT_CHECKOUT_URL
    }

    override fun getPaymentStatusById(paymentId: String): SimplifiedPaymentStatus {
        return SimplifiedPaymentStatus.valueOf(paymentId)
    }

    override fun refundPayment(payment: Payment) {
        check(payment.paid)
    }

    companion object {
        private const val DEFAULT_CHECKOUT_URL = "http://localhost:8080/checkout"
    }
}