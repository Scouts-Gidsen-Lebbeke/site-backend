package be.sgl.backend.controller

import be.sgl.backend.entity.SimplifiedPaymentStatus
import be.sgl.backend.service.activity.ActivityRegistrationService
import be.sgl.backend.service.event.EventRegistrationService
import be.sgl.backend.service.membership.MembershipService
import be.sgl.backend.service.payment.LocalCheckoutProvider
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

private val logger = KotlinLogging.logger {}

/**
 * Controller for managing local test payments during development.
 *
 * Only available when LocalCheckoutProvider is active (i.e., when Mollie is not configured).
 *
 * Endpoints:
 * - GET /api/local-payments - List all payments
 * - GET /api/local-payments/{paymentId} - Get payment details
 * - POST /api/local-payments/{paymentId}/pay - Mark payment as PAID and trigger webhook
 * - POST /api/local-payments/{paymentId}/cancel - Mark payment as CANCELLED and trigger webhook
 * - POST /api/local-payments/{paymentId}/refund - Mark payment as REFUNDED and trigger webhook
 * - GET /api/local-payments/checkout - Simple checkout page simulation
 * - POST /api/local-payments/clear - Clear all payments
 */
@RestController
@RequestMapping("/api/local-payments")
@ConditionalOnBean(LocalCheckoutProvider::class)
@Tag(name = "Local Payments (Dev Only)", description = "Development-only endpoints for testing payments locally")
class LocalPaymentController {

    @Autowired
    private lateinit var localCheckoutProvider: LocalCheckoutProvider

    @Autowired
    private lateinit var eventRegistrationService: EventRegistrationService

    @Autowired
    private lateinit var activityRegistrationService: ActivityRegistrationService

    @Autowired
    private lateinit var membershipService: MembershipService

    @GetMapping
    @Operation(summary = "List all local payments", description = "Returns all payments stored in memory")
    fun listPayments(): ResponseEntity<List<PaymentListItem>> {
        val payments = localCheckoutProvider.getAllPayments().map { data ->
            PaymentListItem(
                paymentId = data.paymentId,
                description = data.payment.getDescription(),
                customerName = data.customer.name,
                customerEmail = data.customer.email,
                amount = data.payment.price,
                status = data.status,
                domain = data.domain,
                createdAt = data.createdAt.toString(),
                checkoutUrl = data.checkoutUrl,
                redirectUrl = data.redirectUrl
            )
        }
        return ResponseEntity.ok(payments)
    }

    @GetMapping("/{paymentId}")
    @Operation(summary = "Get payment details", description = "Returns detailed information about a specific payment")
    fun getPayment(@PathVariable paymentId: String): ResponseEntity<PaymentDetail> {
        val data = localCheckoutProvider.getPaymentData(paymentId)
            ?: return ResponseEntity.notFound().build()

        val detail = PaymentDetail(
            paymentId = data.paymentId,
            description = data.payment.getDescription(),
            customer = CustomerInfo(
                id = data.customer.id,
                name = data.customer.name,
                email = data.customer.email
            ),
            amount = data.payment.price,
            status = data.status,
            domain = data.domain,
            payableId = data.payableId,
            orderId = data.payment.id,
            paid = data.payment.paid,
            checkoutUrl = data.checkoutUrl,
            redirectUrl = data.redirectUrl,
            webhookUrl = getWebhookUrl(data.domain),
            createdAt = data.createdAt.toString(),
            updatedAt = data.updatedAt.toString()
        )
        return ResponseEntity.ok(detail)
    }

    @PostMapping("/{paymentId}/pay")
    @Operation(
        summary = "Mark payment as PAID",
        description = "Updates payment status to PAID and triggers the webhook"
    )
    fun markAsPaid(@PathVariable paymentId: String): ResponseEntity<PaymentActionResult> {
        val data = localCheckoutProvider.getPaymentData(paymentId)
            ?: return ResponseEntity.notFound().build()

        localCheckoutProvider.updatePaymentStatus(paymentId, SimplifiedPaymentStatus.PAID)
        triggerWebhook(data.domain, paymentId)

        logger.info { "✅ Payment $paymentId marked as PAID and webhook triggered" }
        return ResponseEntity.ok(
            PaymentActionResult(
                success = true,
                message = "Payment marked as PAID and webhook triggered",
                paymentId = paymentId,
                newStatus = SimplifiedPaymentStatus.PAID,
                redirectUrl = data.redirectUrl
            )
        )
    }

    @PostMapping("/{paymentId}/cancel")
    @Operation(
        summary = "Mark payment as CANCELLED",
        description = "Updates payment status to CANCELLED and triggers the webhook"
    )
    fun markAsCancelled(@PathVariable paymentId: String): ResponseEntity<PaymentActionResult> {
        val data = localCheckoutProvider.getPaymentData(paymentId)
            ?: return ResponseEntity.notFound().build()

        localCheckoutProvider.updatePaymentStatus(paymentId, SimplifiedPaymentStatus.CANCELLED)
        triggerWebhook(data.domain, paymentId)

        logger.info { "❌ Payment $paymentId marked as CANCELLED and webhook triggered" }
        return ResponseEntity.ok(
            PaymentActionResult(
                success = true,
                message = "Payment marked as CANCELLED and webhook triggered",
                paymentId = paymentId,
                newStatus = SimplifiedPaymentStatus.CANCELLED,
                redirectUrl = data.redirectUrl
            )
        )
    }

    @PostMapping("/{paymentId}/refund")
    @Operation(
        summary = "Mark payment as REFUNDED",
        description = "Updates payment status to REFUNDED and triggers the webhook"
    )
    fun markAsRefunded(@PathVariable paymentId: String): ResponseEntity<PaymentActionResult> {
        val data = localCheckoutProvider.getPaymentData(paymentId)
            ?: return ResponseEntity.notFound().build()

        localCheckoutProvider.updatePaymentStatus(paymentId, SimplifiedPaymentStatus.REFUNDED)
        triggerWebhook(data.domain, paymentId)

        logger.info { "💰 Payment $paymentId marked as REFUNDED and webhook triggered" }
        return ResponseEntity.ok(
            PaymentActionResult(
                success = true,
                message = "Payment marked as REFUNDED and webhook triggered",
                paymentId = paymentId,
                newStatus = SimplifiedPaymentStatus.REFUNDED,
                redirectUrl = data.redirectUrl
            )
        )
    }

    @GetMapping("/checkout", produces = [MediaType.TEXT_HTML_VALUE])
    @Operation(
        summary = "Checkout page simulation",
        description = "Simple HTML page to simulate Mollie checkout flow"
    )
    fun checkoutPage(@RequestParam paymentId: String): ResponseEntity<String> {
        val data = localCheckoutProvider.getPaymentData(paymentId)
            ?: return ResponseEntity.ok("""
                <!DOCTYPE html>
                <html>
                <head>
                    <title>Payment Not Found</title>
                    <style>
                        body { font-family: Arial, sans-serif; max-width: 600px; margin: 50px auto; padding: 20px; }
                        .error { color: red; }
                    </style>
                </head>
                <body>
                    <h1 class="error">Payment Not Found</h1>
                    <p>Payment ID: <code>$paymentId</code></p>
                </body>
                </html>
            """.trimIndent())

        val html = """
            <!DOCTYPE html>
            <html>
            <head>
                <title>Local Payment Checkout - ${data.payment.getDescription()}</title>
                <meta charset="UTF-8">
                <style>
                    body {
                        font-family: Arial, sans-serif;
                        max-width: 600px;
                        margin: 50px auto;
                        padding: 20px;
                        background-color: #f5f5f5;
                    }
                    .container {
                        background: white;
                        border-radius: 8px;
                        padding: 30px;
                        box-shadow: 0 2px 10px rgba(0,0,0,0.1);
                    }
                    h1 { color: #333; margin-top: 0; }
                    .info { margin: 20px 0; }
                    .info-row {
                        display: flex;
                        justify-content: space-between;
                        padding: 10px 0;
                        border-bottom: 1px solid #eee;
                    }
                    .label { font-weight: bold; color: #666; }
                    .value { color: #333; }
                    .amount { font-size: 24px; color: #0077cc; font-weight: bold; }
                    .buttons { margin-top: 30px; }
                    button {
                        padding: 12px 24px;
                        margin: 5px;
                        border: none;
                        border-radius: 4px;
                        font-size: 16px;
                        cursor: pointer;
                        transition: background-color 0.3s;
                    }
                    .btn-pay { background-color: #0077cc; color: white; }
                    .btn-pay:hover { background-color: #005fa3; }
                    .btn-cancel { background-color: #dc3545; color: white; }
                    .btn-cancel:hover { background-color: #c82333; }
                    .notice {
                        background-color: #fff3cd;
                        border: 1px solid #ffc107;
                        border-radius: 4px;
                        padding: 15px;
                        margin-top: 20px;
                    }
                    .notice strong { color: #856404; }
                    #message {
                        margin-top: 20px;
                        padding: 15px;
                        border-radius: 4px;
                        display: none;
                    }
                    .success { background-color: #d4edda; color: #155724; border: 1px solid #c3e6cb; }
                    .error { background-color: #f8d7da; color: #721c24; border: 1px solid #f5c6cb; }
                </style>
            </head>
            <body>
                <div class="container">
                    <h1>🏕️ Local Payment Checkout</h1>

                    <div class="info">
                        <div class="info-row">
                            <span class="label">Description:</span>
                            <span class="value">${data.payment.getDescription()}</span>
                        </div>
                        <div class="info-row">
                            <span class="label">Customer:</span>
                            <span class="value">${data.customer.name}</span>
                        </div>
                        <div class="info-row">
                            <span class="label">Email:</span>
                            <span class="value">${data.customer.email}</span>
                        </div>
                        <div class="info-row">
                            <span class="label">Amount:</span>
                            <span class="value amount">€${String.format("%.2f", data.payment.price)}</span>
                        </div>
                        <div class="info-row">
                            <span class="label">Payment ID:</span>
                            <span class="value"><code>${data.paymentId}</code></span>
                        </div>
                    </div>

                    <div class="notice">
                        <strong>⚠️ Development Mode:</strong> This is a simulated payment page for local testing.
                        Click "Pay Now" to simulate a successful payment, or "Cancel" to simulate a cancelled payment.
                    </div>

                    <div class="buttons">
                        <button class="btn-pay" onclick="processPayment('pay')">💳 Pay Now</button>
                        <button class="btn-cancel" onclick="processPayment('cancel')">❌ Cancel Payment</button>
                    </div>

                    <div id="message"></div>
                </div>

                <script>
                    async function processPayment(action) {
                        const paymentId = '${data.paymentId}';
                        const messageDiv = document.getElementById('message');

                        try {
                            const response = await fetch('/api/local-payments/' + paymentId + '/' + action, {
                                method: 'POST'
                            });

                            if (response.ok) {
                                const result = await response.json();
                                messageDiv.className = 'success';
                                messageDiv.style.display = 'block';
                                messageDiv.textContent = result.message;

                                // Redirect after 2 seconds
                                setTimeout(() => {
                                    window.location.href = result.redirectUrl;
                                }, 2000);
                            } else {
                                messageDiv.className = 'error';
                                messageDiv.style.display = 'block';
                                messageDiv.textContent = 'Failed to process payment. Please try again.';
                            }
                        } catch (error) {
                            messageDiv.className = 'error';
                            messageDiv.style.display = 'block';
                            messageDiv.textContent = 'Error: ' + error.message;
                        }
                    }
                </script>
            </body>
            </html>
        """.trimIndent()

        return ResponseEntity.ok(html)
    }

    @PostMapping("/clear")
    @Operation(
        summary = "Clear all payments",
        description = "Removes all payments from memory (for testing cleanup)"
    )
    fun clearPayments(): ResponseEntity<Map<String, String>> {
        localCheckoutProvider.clearAllPayments()
        return ResponseEntity.ok(mapOf("message" to "All payments cleared"))
    }

    private fun triggerWebhook(domain: String, paymentId: String) {
        when (domain) {
            "events" -> eventRegistrationService.updatePayment(paymentId)
            "activities" -> activityRegistrationService.updatePayment(paymentId)
            "memberships" -> membershipService.updatePayment(paymentId)
            else -> logger.warn { "Unknown domain: $domain" }
        }
    }

    private fun getWebhookUrl(domain: String): String {
        return "/api/$domain/updatePayment"
    }

    // DTOs
    data class PaymentListItem(
        val paymentId: String,
        val description: String,
        val customerName: String,
        val customerEmail: String,
        val amount: Double,
        val status: SimplifiedPaymentStatus,
        val domain: String,
        val createdAt: String,
        val checkoutUrl: String,
        val redirectUrl: String
    )

    data class PaymentDetail(
        val paymentId: String,
        val description: String,
        val customer: CustomerInfo,
        val amount: Double,
        val status: SimplifiedPaymentStatus,
        val domain: String,
        val payableId: Int?,
        val orderId: Int?,
        val paid: Boolean,
        val checkoutUrl: String,
        val redirectUrl: String,
        val webhookUrl: String,
        val createdAt: String,
        val updatedAt: String
    )

    data class CustomerInfo(
        val id: String?,
        val name: String,
        val email: String
    )

    data class PaymentActionResult(
        val success: Boolean,
        val message: String,
        val paymentId: String,
        val newStatus: SimplifiedPaymentStatus,
        val redirectUrl: String
    )
}
