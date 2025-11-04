# Local Payment Stub for Development

This document explains how to use the local payment stub for testing payment flows without connecting to the real Mollie API.

## Overview

The local payment stub (`LocalCheckoutProvider`) provides a fully functional payment system for local development and testing. It simulates the entire Mollie payment flow including:

- Creating payments and checkout URLs
- Managing payment statuses (PAID, CANCELLED, REFUNDED, ONGOING)
- Triggering webhooks automatically
- Providing a simulated checkout page
- Storing payment data in memory for inspection

## Quick Start

### 1. Configuration

The stub is automatically enabled when the Mollie API key is **not** configured. This is the default in `application-local.yml`:

```yaml
organization:
  api-key:
    mollie:  # Leave blank for local stub
```

If you want to use the real Mollie API in local development, set your test API key:

```yaml
organization:
  api-key:
    mollie: "test_xxxxxxxxxxxxxxxxxx"
```

### 2. Start the Application

```bash
# Make sure your local environment is running (database, etc.)
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

You should see in the logs:
```
🎭 LocalCheckoutProvider initialized - Using LOCAL payment stub for development
💡 Tip: Use LocalPaymentController endpoints to manage test payments
```

### 3. Testing Payment Flow

#### Option A: Automatic Flow (Simulated Checkout)

1. Create a registration (event/activity/membership) through the normal API
2. You'll receive a checkout URL like: `http://localhost:8080/api/local-payments/checkout?paymentId=tr_local_xxx`
3. Open this URL in your browser to see the simulated checkout page
4. Click "Pay Now" to simulate successful payment or "Cancel" to simulate cancellation
5. The webhook will be triggered automatically, updating the registration status

#### Option B: Manual API Control

1. Create a registration to generate a payment
2. Use the Local Payment API endpoints to manage the payment:

```bash
# List all pending payments
GET http://localhost:8080/api/local-payments

# Get specific payment details
GET http://localhost:8080/api/local-payments/{paymentId}

# Mark payment as PAID (triggers webhook)
POST http://localhost:8080/api/local-payments/{paymentId}/pay

# Mark payment as CANCELLED (triggers webhook)
POST http://localhost:8080/api/local-payments/{paymentId}/cancel

# Mark payment as REFUNDED (triggers webhook)
POST http://localhost:8080/api/local-payments/{paymentId}/refund

# Clear all payments
POST http://localhost:8080/api/local-payments/clear
```

## API Endpoints

All endpoints are available at `/api/local-payments` and are documented in Swagger UI at `http://localhost:8080/`.

### List All Payments

```
GET /api/local-payments
```

Returns all payments currently in memory with their status.

**Response:**
```json
[
  {
    "paymentId": "tr_local_abc123def456",
    "description": "Event Registration: Summer Camp 2024",
    "customerName": "John Doe",
    "customerEmail": "john@example.com",
    "amount": 25.0,
    "status": "ONGOING",
    "domain": "events",
    "createdAt": "2024-11-04T10:30:00",
    "checkoutUrl": "http://localhost:8080/api/local-payments/checkout?paymentId=tr_local_abc123def456",
    "redirectUrl": "http://localhost:8080/api/events/confirmation.html?id=1&order_id=42"
  }
]
```

### Get Payment Details

```
GET /api/local-payments/{paymentId}
```

Returns detailed information about a specific payment.

### Process Payment Actions

```
POST /api/local-payments/{paymentId}/pay
POST /api/local-payments/{paymentId}/cancel
POST /api/local-payments/{paymentId}/refund
```

Each action:
1. Updates the payment status
2. Triggers the appropriate webhook (`/api/{domain}/updatePayment`)
3. Returns the new status and redirect URL

**Response:**
```json
{
  "success": true,
  "message": "Payment marked as PAID and webhook triggered",
  "paymentId": "tr_local_abc123def456",
  "newStatus": "PAID",
  "redirectUrl": "http://localhost:8080/api/events/confirmation.html?id=1&order_id=42"
}
```

### Checkout Page

```
GET /api/local-payments/checkout?paymentId={paymentId}
```

Returns an HTML page simulating the Mollie checkout experience with:
- Payment details (description, customer, amount)
- "Pay Now" button (marks as PAID and triggers webhook)
- "Cancel Payment" button (marks as CANCELLED and triggers webhook)
- Automatic redirect to the original confirmation page after action

## Example Workflow

### Testing Event Registration

1. **Create an event registration:**
   ```bash
   curl -X POST http://localhost:8080/api/events/1/register \
     -H "Content-Type: application/json" \
     -d '{
       "firstName": "John",
       "name": "Doe",
       "email": "john@example.com",
       "mobile": "+32475123456",
       "additionalData": {}
     }'
   ```

   **Response:**
   ```json
   {
     "url": "http://localhost:8080/api/local-payments/checkout?paymentId=tr_local_abc123def456"
   }
   ```

2. **Check the payment was created:**
   ```bash
   curl http://localhost:8080/api/local-payments
   ```

3. **Option 1 - Use the checkout page:**
   - Open `http://localhost:8080/api/local-payments/checkout?paymentId=tr_local_abc123def456` in browser
   - Click "Pay Now"
   - Observe webhook being triggered in logs
   - Redirected to confirmation page

4. **Option 2 - Manually mark as paid:**
   ```bash
   curl -X POST http://localhost:8080/api/local-payments/tr_local_abc123def456/pay
   ```

5. **Verify the registration is marked as paid:**
   The webhook automatically updates the registration status and sends confirmation email.

### Testing Refunds

1. **First, create and pay for a registration** (see above)

2. **Cancel the registration:**
   ```bash
   curl -X POST http://localhost:8080/api/events/registrations/1/cancel
   ```

3. **The stub automatically:**
   - Marks the payment as REFUNDED
   - Triggers the webhook
   - Sends refund confirmation email

## Testing Different Scenarios

### Pattern-Based Status (For Tests)

The stub supports pattern-based payment IDs for integration tests:

```kotlin
// In test code, you can create payments with specific IDs:
payment.paymentId = "PAID_test_payment_1"    // Returns PAID status
payment.paymentId = "CANCELLED_test_payment_2" // Returns CANCELLED status
payment.paymentId = "REFUNDED_test_payment_3"  // Returns REFUNDED status
payment.paymentId = "ONGOING_test_payment_4"   // Returns ONGOING status
```

This is useful for testing webhook handlers without needing to make API calls.

## Logging

The stub logs all payment operations for debugging:

```
💳 Created local payment: tr_local_abc123def456 for Event Registration: Summer Camp
   Customer: John Doe (john@example.com)
   Amount: €25.0
   Checkout: http://localhost:8080/api/local-payments/checkout?paymentId=tr_local_abc123def456
   Domain: events

✅ Updated payment tr_local_abc123def456 status to PAID
```

## Differences from Real Mollie

While the stub simulates the Mollie flow, there are some differences:

| Feature | Real Mollie | Local Stub |
|---------|-------------|------------|
| API Key Required | Yes | No |
| External Checkout Page | Yes | Local HTML page |
| Webhook Delay | Varies | Immediate |
| Payment IDs | `tr_xxx` format | `tr_local_xxx` format |
| Customer IDs | `cst_xxx` format | `cst_local_xxx` format |
| Transaction Fees | Real fees applied | Simulated (€1 deduction on refund) |
| Payment Methods | Multiple supported | Simulated BANCONTACT |
| Persistent Storage | Mollie servers | In-memory (cleared on restart) |

## Architecture

### Components

1. **LocalCheckoutProvider** (`service/payment/LocalCheckoutProvider.kt`)
   - Implements `CheckoutProvider` interface
   - Stores payments in `ConcurrentHashMap`
   - Only active when Mollie Client bean is missing
   - Uses `@ConditionalOnMissingBean(Client::class)`

2. **LocalPaymentController** (`controller/LocalPaymentController.kt`)
   - REST API for managing test payments
   - Provides checkout page HTML
   - Triggers webhooks on status changes
   - Only active when `LocalCheckoutProvider` is available
   - Uses `@ConditionalOnBean(LocalCheckoutProvider::class)`

### Bean Conditions

The application uses Spring's conditional beans to automatically select the right provider:

```kotlin
// Production - Only created when API key is set
@Bean
@WhenNotBlank("organization.api-key.mollie")
fun mollieApiClient(@Value("\${organization.api-key.mollie}") apiKey: String): Client

// Production - Only created when Client bean exists
@Service
@ConditionalOnBean(Client::class)
class MollieCheckout : CheckoutProvider

// Development - Only created when Client bean is missing
@Service
@ConditionalOnMissingBean(Client::class)
class LocalCheckoutProvider : CheckoutProvider
```

This ensures only one `CheckoutProvider` implementation is active at a time.

## Troubleshooting

### Stub not activating

**Symptom:** Application tries to use real Mollie API in local mode

**Solution:** Check that `organization.api-key.mollie` is blank or missing in `application-local.yml`

### Webhooks not triggering

**Symptom:** Payment status updates but registration doesn't update

**Solution:** The controller automatically triggers webhooks when you use the `/pay`, `/cancel`, or `/refund` endpoints. If using the checkout page, webhooks are triggered via JavaScript after the action completes.

### Payments disappear after restart

**Expected behavior:** Payments are stored in memory and cleared on application restart. This is intentional for development. Use the `/api/local-payments` endpoint to view active payments.

### Cannot see Local Payment endpoints in Swagger

**Check:** The `LocalPaymentController` is only registered when `LocalCheckoutProvider` bean exists. Verify the stub is active by checking logs for the initialization message.

## Testing with Postman/Insomnia

Import this collection to test the payment flow:

```json
{
  "name": "Local Payment Testing",
  "requests": [
    {
      "name": "List Payments",
      "method": "GET",
      "url": "http://localhost:8080/api/local-payments"
    },
    {
      "name": "Mark as Paid",
      "method": "POST",
      "url": "http://localhost:8080/api/local-payments/{{paymentId}}/pay"
    },
    {
      "name": "Mark as Cancelled",
      "method": "POST",
      "url": "http://localhost:8080/api/local-payments/{{paymentId}}/cancel"
    },
    {
      "name": "Clear Payments",
      "method": "POST",
      "url": "http://localhost:8080/api/local-payments/clear"
    }
  ]
}
```

## Integration Tests

The stub is designed to work seamlessly with integration tests:

```kotlin
@SpringBootTest
@AutoConfigureMockMvc
class PaymentFlowIntegrationTest {

    @Autowired
    private lateinit var localCheckoutProvider: LocalCheckoutProvider

    @Test
    fun `should process payment successfully`() {
        // Create registration
        val response = mockMvc.perform(post("/api/events/1/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""{"firstName":"John","name":"Doe","email":"john@example.com"}"""))
            .andExpect(status().isOk)
            .andReturn()

        // Extract payment ID
        val url = jacksonObjectMapper().readTree(response.response.contentAsString)
            .get("url").asText()
        val paymentId = url.substringAfter("paymentId=")

        // Mark as paid
        localCheckoutProvider.updatePaymentStatus(paymentId, SimplifiedPaymentStatus.PAID)

        // Trigger webhook
        mockMvc.perform(post("/api/events/updatePayment")
            .param("id", paymentId))
            .andExpect(status().isOk)

        // Verify registration is paid
        // ... assertions
    }
}
```

## Migration to Production

When deploying to production, simply set the Mollie API key:

```bash
export ORGANIZATION_API_KEY_MOLLIE="live_xxxxxxxxxxxxxxxxxx"
```

The application will automatically:
1. Create the Mollie `Client` bean
2. Activate `MollieCheckout` instead of `LocalCheckoutProvider`
3. Disable `LocalPaymentController`
4. Use real Mollie API for all payments

No code changes required!

## Summary

The local payment stub provides a complete, production-like payment experience for development:

✅ No external dependencies (no Mollie account needed)
✅ Full control over payment status
✅ Automatic webhook triggering
✅ Visual checkout page
✅ Complete API for testing
✅ Seamless transition to production
✅ Integration test friendly

Happy testing! 🚀
