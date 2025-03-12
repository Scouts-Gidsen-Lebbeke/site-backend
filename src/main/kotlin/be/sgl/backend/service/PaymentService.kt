package be.sgl.backend.service

import be.sgl.backend.entity.Payment
import be.sgl.backend.entity.SimplifiedPaymentStatus
import be.sgl.backend.repository.PaymentRepository
import be.sgl.backend.service.payment.CheckoutProvider
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.jpa.repository.JpaRepository

abstract class PaymentService<T : Payment, R> where R : PaymentRepository<T>, R : JpaRepository<T, Int> {

    private val logger = KotlinLogging.logger {}

    protected abstract var paymentRepository: R
    @Autowired
    protected lateinit var checkoutProvider: CheckoutProvider
    @Autowired
    protected lateinit var mailService: MailService

    fun updatePayment(paymentId: String) {
        logger.debug { "Update payment request for payment $paymentId..." }
        val payment = paymentRepository.getByPaymentId(paymentId)
        if (payment == null) {
            logger.warn { "Payment $paymentId not found!" }
            return
        }
        when (checkoutProvider.getPaymentStatusById(paymentId)) {
            SimplifiedPaymentStatus.PAID -> {
                if (payment.paid) {
                    logger.info { "Paid payment update received for payment already marked as paid, skipped." }
                    return
                }
                logger.debug { "Paid payment, marking payment as paid..." }
                payment.markPaid()
                paymentRepository.save(payment)
                handlePaymentPaid(payment)
            }
            SimplifiedPaymentStatus.CANCELLED -> {
                check(!payment.paid) { "This payment should never have been marked as paid!" }
                paymentRepository.delete(payment)
                handlePaymentCanceled(payment)
            }
            SimplifiedPaymentStatus.REFUNDED -> {
                check(payment.paid) { "This payment should have been marked as paid!" }
                paymentRepository.delete(payment)
                handlePaymentRefunded(payment)
            }
            else -> {
                // do nothing, the payment is still ongoing
            }
        }
        logger.debug { "Update payment request handled." }
    }

    open fun handlePaymentPaid(payment: T) {}

    open fun handlePaymentCanceled(payment: T) {}

    open fun handlePaymentRefunded(payment: T) {}
}