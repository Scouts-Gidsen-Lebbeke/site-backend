package be.sgl.backend.service.payment

import be.sgl.backend.dto.Customer
import be.sgl.backend.entity.Payment
import be.sgl.backend.entity.SimplifiedPaymentStatus
import be.sgl.backend.util.appendRequestParameters
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.stereotype.Service
import be.woutschoovaerts.mollie.Client
import java.time.LocalDateTime
import java.util.*
import java.util.concurrent.ConcurrentHashMap

private val logger = KotlinLogging.logger {}

/**
 * Local/Development implementation of CheckoutProvider for testing payments without Mollie API.
 *
 * This provider simulates the Mollie payment flow locally:
 * - Generates unique payment IDs
 * - Stores payment data in memory
 * - Supports all payment statuses (PAID, CANCELLED, REFUNDED, ONGOING)
 * - Provides deterministic behavior for testing
 *
 * Only activated when Mollie Client bean is not available (i.e., when organization.api-key.mollie is blank).
 *
 * Payment Status Control:
 * - Use paymentId patterns to control status: "PAID_xxx", "CANCELLED_xxx", "REFUNDED_xxx"
 * - Or use the LocalPaymentController to manually update payment statuses
 */
@Service
@ConditionalOnMissingBean(Client::class)
class LocalCheckoutProvider : CheckoutProvider {

    @Value("\${spring.application.base-url:http://localhost:8080}")
    private lateinit var baseUrl: String

    @Value("\${spring.application.public-base-url:http://localhost:8080}")
    private lateinit var publicBaseUrl: String

    // In-memory storage for testing
    private val payments = ConcurrentHashMap<String, PaymentData>()
    private val customers = ConcurrentHashMap<String, Customer>()

    data class PaymentData(
        val paymentId: String,
        val customer: Customer,
        val payment: Payment,
        val domain: String,
        val payableId: Int?,
        val checkoutUrl: String,
        val redirectUrl: String,
        var status: SimplifiedPaymentStatus = SimplifiedPaymentStatus.ONGOING,
        val createdAt: LocalDateTime = LocalDateTime.now(),
        var updatedAt: LocalDateTime = LocalDateTime.now()
    )

    init {
        logger.info { "🎭 LocalCheckoutProvider initialized - Using LOCAL payment stub for development" }
        logger.info { "💡 Tip: Use LocalPaymentController endpoints to manage test payments" }
    }

    override fun createRedirectUrl(payment: Payment, domain: String, payableId: Int?): String {
        val redirectUrl = appendRequestParameters(
            "$baseUrl/$domain/confirmation.html",
            "id" to payableId,
            "order_id" to payment.id
        )
        logger.debug { "Created redirect URL: $redirectUrl" }
        return redirectUrl
    }

    override fun createCheckoutUrl(
        customer: Customer,
        payment: Payment,
        domain: String,
        payableId: Int?
    ): String {
        checkNotNull(payment.id) { "Payment ID must not be null" }

        // Generate unique payment ID
        val paymentId = generatePaymentId()
        payment.paymentId = paymentId

        // Create customer ID if not exists
        val customerId = customer.id ?: createCustomerId(customer)
        val customerWithId = customer.copy(id = customerId)
        customers[customerId] = customerWithId

        // Create checkout URL
        val checkoutUrl = "$publicBaseUrl/api/local-payments/checkout?paymentId=$paymentId"
        val redirectUrl = createRedirectUrl(payment, domain, payableId)

        // Store payment data
        val paymentData = PaymentData(
            paymentId = paymentId,
            customer = customerWithId,
            payment = payment,
            domain = domain,
            payableId = payableId,
            checkoutUrl = checkoutUrl,
            redirectUrl = redirectUrl,
            status = SimplifiedPaymentStatus.ONGOING
        )
        payments[paymentId] = paymentData

        logger.info { "💳 Created local payment: $paymentId for ${payment.getDescription()}" }
        logger.info { "   Customer: ${customer.name} (${customer.email})" }
        logger.info { "   Amount: €${payment.price}" }
        logger.info { "   Checkout: $checkoutUrl" }
        logger.info { "   Domain: $domain" }

        return checkoutUrl
    }

    override fun getCheckoutUrl(payment: Payment): String {
        checkNotNull(payment.paymentId) { "Payment ID must not be null" }
        val paymentData = payments[payment.paymentId]
            ?: throw IllegalStateException("Payment ${payment.paymentId} not found in local storage")
        return paymentData.checkoutUrl
    }

    override fun getPaymentStatusById(paymentId: String): SimplifiedPaymentStatus {
        // Check if payment exists in memory
        val paymentData = payments[paymentId]

        if (paymentData != null) {
            logger.debug { "Retrieved payment status for $paymentId: ${paymentData.status}" }
            return paymentData.status
        }

        // Fallback: Support pattern-based status for testing
        // This allows tests to control status via paymentId naming
        val status = when {
            paymentId.startsWith("PAID_", ignoreCase = true) -> SimplifiedPaymentStatus.PAID
            paymentId.startsWith("CANCELLED_", ignoreCase = true) -> SimplifiedPaymentStatus.CANCELLED
            paymentId.startsWith("REFUNDED_", ignoreCase = true) -> SimplifiedPaymentStatus.REFUNDED
            paymentId.startsWith("ONGOING_", ignoreCase = true) -> SimplifiedPaymentStatus.ONGOING
            else -> {
                logger.warn { "Payment $paymentId not found in local storage, defaulting to ONGOING" }
                SimplifiedPaymentStatus.ONGOING
            }
        }

        logger.debug { "Retrieved payment status for $paymentId (pattern-based): $status" }
        return status
    }

    override fun refundPayment(payment: Payment) {
        check(payment.paid) { "Payment must be paid before refunding" }
        checkNotNull(payment.paymentId) { "Payment ID must not be null" }

        val paymentData = payments[payment.paymentId]
        if (paymentData != null) {
            paymentData.status = SimplifiedPaymentStatus.REFUNDED
            paymentData.updatedAt = LocalDateTime.now()
            logger.info { "💰 Refunded payment: ${payment.paymentId} (€${payment.price - 1})" }
        } else {
            logger.warn { "⚠️ Payment ${payment.paymentId} not found in local storage, but refund acknowledged" }
        }
    }

    /**
     * Update payment status (for use by LocalPaymentController)
     */
    fun updatePaymentStatus(paymentId: String, status: SimplifiedPaymentStatus): Boolean {
        val paymentData = payments[paymentId] ?: return false
        paymentData.status = status
        paymentData.updatedAt = LocalDateTime.now()
        logger.info { "✅ Updated payment $paymentId status to $status" }
        return true
    }

    /**
     * Get all payments (for debugging/testing)
     */
    fun getAllPayments(): List<PaymentData> = payments.values.toList()

    /**
     * Get payment data by ID
     */
    fun getPaymentData(paymentId: String): PaymentData? = payments[paymentId]

    /**
     * Clear all payments (for testing)
     */
    fun clearAllPayments() {
        payments.clear()
        customers.clear()
        logger.info { "🗑️ Cleared all local payments" }
    }

    private fun generatePaymentId(): String {
        return "tr_local_${UUID.randomUUID().toString().replace("-", "").substring(0, 16)}"
    }

    private fun createCustomerId(customer: Customer): String {
        return "cst_local_${UUID.randomUUID().toString().replace("-", "").substring(0, 16)}"
    }
}
