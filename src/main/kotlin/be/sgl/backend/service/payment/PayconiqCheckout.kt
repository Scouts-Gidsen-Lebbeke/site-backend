package be.sgl.backend.service.payment

import be.sgl.backend.dto.Customer
import be.sgl.backend.entity.Payment
import be.sgl.backend.entity.SimplifiedPaymentStatus
import be.sgl.backend.util.WhenNotBlank
import org.springframework.stereotype.Service

@Service
@WhenNotBlank("payconiq.api.key")
class PayconiqCheckout : CheckoutProvider {

    override fun createRedirectUrl(payment: Payment, domain: String, payableId: Int?): String {
        TODO("Not yet implemented")
    }

    override fun createCheckoutUrl(customer: Customer, payment: Payment, domain: String, payableId: Int?): String {
        TODO("Not yet implemented")
    }

    override fun getCheckoutUrl(payment: Payment): String {
        TODO("Not yet implemented")
    }

    override fun getPaymentStatusById(paymentId: String): SimplifiedPaymentStatus {
        TODO("Not yet implemented")
    }

    override fun refundPayment(payment: Payment) {
        TODO("Not yet implemented")
    }
}