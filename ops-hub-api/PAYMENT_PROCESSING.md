# Payment Processing Implementation Documentation

## Overview
This document describes the payment processing system for the Ops Hub application. The system supports UPI payments, handles gateway callbacks with idempotency, and ensures transactional integrity.

## Architecture

### Components

#### 1. **PaymentService** (`service/PaymentService.java`)
- Handles payment initiation and processing
- Manages payment status updates
- Ensures transactional integrity
- Integrates with gateway service
- Handles callbacks with idempotency
- Generates payment receipts
- Integrates with audit logging

#### 2. **PaymentGatewayService** (`service/PaymentGatewayService.java`)
- **Isolated gateway logic** - can be replaced without affecting payment service
- Handles UPI payment initiation
- Verifies callback signatures
- Normalizes gateway responses
- Gateway-specific operations

#### 3. **PaymentController** (`controller/PaymentController.java`)
- REST endpoints for payment operations
- Permission-protected endpoints using `@RequiresPermission`
- Public callback endpoint for gateway

### Database Tables

- **payments**: Payment records with status tracking
- **payment_events**: Gateway callbacks and payment lifecycle events
- **customers**: Customer information
- **users**: User information
- **audit_logs**: Logs all payment actions

## Payment Flow

### 1. Payment Initiation Flow

```
User (with COLLECT_PAYMENT) → Initiate Payment → Create Payment Record → 
Call Gateway (UPI) → Update Status → Create Event → Audit Log
```

**Steps:**
1. User with `COLLECT_PAYMENT` permission initiates payment
2. Payment record created with status `PENDING` (maps to INITIATED)
3. For UPI: Gateway service called to initiate payment
4. Payment status updated to `PROCESSING` (maps to INITIATED)
5. Gateway transaction ID stored
6. Payment event created
7. Audit log entry created

### 2. Gateway Callback Flow

```
Gateway → Callback Endpoint → Verify Signature → Check Idempotency → 
Update Payment Status → Create Event → Audit Log
```

**Steps:**
1. Gateway sends callback to `/payments/callback`
2. System verifies callback signature
3. System checks for duplicate callback (idempotency)
4. Payment status updated based on gateway response
5. Payment event created
6. Audit log entry created

### 3. Non-UPI Payment Flow

```
User → Initiate Payment (CASH/CARD) → Create Payment Record → 
Mark as SUCCESS → Create Event → Audit Log
```

**Steps:**
1. User initiates non-UPI payment
2. Payment record created
3. Status immediately set to `SUCCESS`
4. Transaction ID generated
5. Payment event created
6. Audit log entry created

## API Endpoints

### Initiate Payment (Requires COLLECT_PAYMENT)
```
POST /api/payments
Authorization: Bearer <token>
Content-Type: application/json

Request Body:
{
  "customerId": 1,
  "amount": 1000.00,
  "paymentMethod": "UPI",
  "upiId": "user@paytm",
  "currency": "INR",
  "notes": "Payment for services"
}

Response: 200 OK
{
  "id": 1,
  "paymentReference": "PAY-ABC123...",
  "customerId": 1,
  "amount": 1000.00,
  "paymentMethod": "UPI",
  "paymentStatus": "PROCESSING",
  "gatewayTransactionId": "TXN_XYZ789...",
  ...
}
```

### Payment Gateway Callback (Public)
```
POST /api/payments/callback
Content-Type: application/json

Request Body:
{
  "paymentReference": "PAY-ABC123...",
  "gatewayTransactionId": "TXN_XYZ789...",
  "status": "SUCCESS",
  "gatewayName": "UPI_GATEWAY",
  "signature": "signature_hash",
  "gatewayResponse": "{...}"
}

Response: 200 OK
{
  "id": 1,
  "paymentReference": "PAY-ABC123...",
  "paymentStatus": "SUCCESS",
  ...
}
```

### Get Payment by Reference
```
GET /api/payments/reference/{paymentReference}
Authorization: Bearer <token>

Response: 200 OK
{
  "id": 1,
  "paymentReference": "PAY-ABC123...",
  "paymentStatus": "SUCCESS",
  ...
}
```

### Get Payment Receipt
```
GET /api/payments/{paymentReference}/receipt
Authorization: Bearer <token>

Response: 200 OK
{
  "receiptNumber": "RCPT-PAY-ABC123...",
  "paymentReference": "PAY-ABC123...",
  "amount": 1000.00,
  "customerName": "John Doe",
  ...
}
```

### Get My Payments
```
GET /api/payments/my-payments
Authorization: Bearer <token>

Response: 200 OK
[
  {
    "id": 1,
    "paymentReference": "PAY-ABC123...",
    "amount": 1000.00,
    ...
  }
]
```

## Payment Status Mapping

| User Status | Database Status | Description |
|------------|----------------|-------------|
| INITIATED | PENDING / PROCESSING | Payment initiated, waiting for gateway |
| SUCCESS | SUCCESS | Payment completed successfully |
| FAILED | FAILED | Payment failed |

## Security Features

1. **Permission-Based Authorization**: Uses `@RequiresPermission("COLLECT_PAYMENT")`
2. **Signature Verification**: Gateway callbacks verified with signature
3. **Idempotency**: Duplicate callbacks are detected and ignored
4. **Never Trust Client**: Payment status always updated from gateway callback
5. **Transactional Integrity**: All operations are transactional
6. **Audit Trail**: All actions logged with full context

## Idempotency Implementation

The system ensures idempotency for callbacks:

1. **Unique Check**: Before processing callback, system checks if event already exists
2. **Payment Reference + Gateway Transaction ID**: Used as unique identifier
3. **Duplicate Detection**: If callback already processed, returns existing payment
4. **Event Logging**: All callbacks logged in `payment_events` table

**Implementation:**
```java
paymentEventRepository.findByPaymentReferenceAndGatewayTransactionId(
    paymentReference, gatewayTransactionId)
    .ifPresent(existing -> {
        throw new IllegalStateException("Callback already processed");
    });
```

## Gateway Integration

### PaymentGatewayService

The `PaymentGatewayService` is isolated and can be replaced with actual gateway integration:

**Current Implementation:**
- Simulated gateway responses
- Placeholder for signature verification
- Status normalization

**Production Integration:**
1. Replace `initiateUpiPayment()` with actual gateway API call
2. Implement `verifyCallbackSignature()` with HMAC verification
3. Update `processCallback()` to handle actual gateway response format
4. Configure gateway credentials in `getGatewayConfig()`

**Example Gateway Integrations:**
- Razorpay
- PayU
- PhonePe
- Paytm
- Stripe (for international)

## Payment Receipt

The system generates payment receipt data ready for:
- PDF generation
- Email sending
- Print formatting
- Digital storage

**Receipt Includes:**
- Receipt number
- Payment reference
- Customer information
- Payment details
- Transaction IDs
- Collection information

## Audit Logging

All payment actions are logged in `audit_logs` table with:
- Action type (CREATE, UPDATE)
- Entity type (PAYMENT)
- Entity ID (payment ID)
- Old and new values (JSON)
- User information
- IP address and user agent
- Request URL and method
- Status (SUCCESS/FAILURE)

## Error Handling

- **Invalid Customer**: Returns 400 Bad Request
- **Permission Denied**: Returns 403 Forbidden
- **Invalid UPI ID**: Returns 400 Bad Request (for UPI payments)
- **Payment Not Found**: Returns 404 Not Found
- **Duplicate Callback**: Returns 400 Bad Request
- **Invalid Signature**: Returns 403 Forbidden
- **Gateway Error**: Payment marked as FAILED, error logged
- **Transaction Failures**: Rolled back automatically

## Best Practices

1. **Never Trust Client Status**: Always update from gateway callback
2. **Idempotency**: Always check for duplicate callbacks
3. **Signature Verification**: Always verify gateway signatures
4. **Transactional Safety**: All operations are transactional
5. **Gateway Isolation**: Keep gateway logic separate and replaceable
6. **Error Handling**: Gracefully handle gateway failures
7. **Audit Everything**: Log all payment actions

## Gateway Callback Security

**Important**: The callback endpoint is publicly accessible but should be secured:

1. **Signature Verification**: Always verify gateway signature
2. **IP Whitelisting**: Restrict callback endpoint to gateway IPs (configure in production)
3. **HTTPS Only**: Use HTTPS in production
4. **Rate Limiting**: Implement rate limiting for callback endpoint

## Usage Examples

### Initiate UPI Payment
```java
InitiatePaymentDTO dto = new InitiatePaymentDTO();
dto.setCustomerId(1L);
dto.setAmount(new BigDecimal("1000.00"));
dto.setPaymentMethod("UPI");
dto.setUpiId("user@paytm");

Payment payment = paymentService.initiatePayment(dto, httpRequest);
```

### Handle Gateway Callback
```java
PaymentCallbackDTO callback = new PaymentCallbackDTO();
callback.setPaymentReference("PAY-ABC123...");
callback.setGatewayTransactionId("TXN_XYZ789...");
callback.setStatus("SUCCESS");
callback.setSignature("signature_hash");

Payment payment = paymentService.handleCallback(callback, httpRequest);
```

### Get Payment Receipt
```java
PaymentReceiptDTO receipt = paymentService.getPaymentReceipt("PAY-ABC123...");
// Use receipt data to generate PDF, send email, etc.
```

## Testing

Example test scenarios:

```java
@Test
@WithMockUser(authorities = "PERMISSION_COLLECT_PAYMENT")
void testInitiatePayment() {
    // Test payment initiation
}

@Test
void testHandleCallback() {
    // Test callback handling
}

@Test
void testIdempotency() {
    // Test duplicate callback handling
}

@Test
void testSignatureVerification() {
    // Test signature verification
}
```

## Future Enhancements

1. **Multiple Gateways**: Support multiple payment gateways
2. **Refund Processing**: Handle payment refunds
3. **Payment Links**: Generate payment links for customers
4. **Recurring Payments**: Support subscription/recurring payments
5. **Payment Analytics**: Track payment metrics and trends
6. **Automated Reconciliation**: Auto-reconcile payments with gateway
7. **Webhook Retry**: Retry failed webhook deliveries
8. **Payment Notifications**: Notify users on payment status changes
