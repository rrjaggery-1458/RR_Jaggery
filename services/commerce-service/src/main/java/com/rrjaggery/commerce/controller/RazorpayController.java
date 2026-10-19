package com.rrjaggery.commerce.controller;

import com.razorpay.RazorpayException;
import com.rrjaggery.commerce.service.OrderService;
import com.rrjaggery.commerce.service.RazorpayPaymentService;
import com.rrjaggery.common.dto.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import org.json.JSONObject;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

/**
 * RazorpayController — Razorpay payment integration endpoints.
 *
 * STATUS: Integration active with live Razorpay credentials.
 *
 * Endpoints (all disabled when RAZORPAY_ENABLED=false):
 *   POST /api/v1/commerce/payments/razorpay/create-order
 *       → Create Razorpay order for an existing pending order or checkout amount
 *   POST /api/v1/commerce/payments/razorpay/verify
 *       → Server-side payment signature verification (MUST precede marking order PAID)
 *   POST /api/v1/commerce/payments/razorpay/webhook
 *       → Razorpay webhook receiver (X-Razorpay-Signature verified server-side)
 */
@RestController
@RequestMapping("/api/v1/commerce/payments/razorpay")
public class RazorpayController {

    private static final Logger log = LoggerFactory.getLogger(RazorpayController.class);

    private final RazorpayPaymentService razorpayService;
    private final OrderService orderService;

    public RazorpayController(RazorpayPaymentService razorpayService, OrderService orderService) {
        this.razorpayService = razorpayService;
        this.orderService = orderService;
    }

    /**
     * Step 1: Frontend requests a Razorpay order for checkout or an existing order.
     * Returns razorpay_order_id + public key_id for the frontend Checkout script.
     */
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/create-order")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createRazorpayOrder(
            @RequestBody Map<String, String> body) {

        if (!razorpayService.isEnabled()) {
            return ResponseEntity.ok(ApiResponse.success(Map.of(
                "status", "RAZORPAY_DISABLED",
                "message", "Razorpay integration is not yet enabled."
            )));
        }

        try {
            BigDecimal amount = null;
            String receipt = "RCPT-" + System.currentTimeMillis();
            String orderNumber = "";

            if (body.containsKey("orderId") && body.get("orderId") != null && !body.get("orderId").isBlank()) {
                UUID orderId = UUID.fromString(body.get("orderId"));
                var order = orderService.getOrderById(orderId, null, true);
                amount = order.getTotalAmount();
                receipt = order.getOrderNumber();
                orderNumber = order.getOrderNumber();
            } else if (body.containsKey("amount") && body.get("amount") != null) {
                amount = new BigDecimal(body.get("amount"));
                receipt = "ORD-" + System.currentTimeMillis();
                orderNumber = receipt;
            } else {
                return ResponseEntity.badRequest().body(ApiResponse.error("orderId or amount required", "INVALID_REQUEST"));
            }

            String razorpayOrderJson = razorpayService.createRazorpayOrder(
                    amount, "INR", receipt);

            String rzpOrderId = "";
            try {
                JSONObject parsed = new JSONObject(razorpayOrderJson);
                rzpOrderId = parsed.optString("id", "");
            } catch (Exception ignored) {}

            return ResponseEntity.ok(ApiResponse.success(Map.of(
                "razorpayOrder", razorpayOrderJson,
                "razorpayOrderId", rzpOrderId,
                "keyId", razorpayService.getPublicKeyId(), // Public key only — safe to send
                "orderNumber", orderNumber
            )));
        } catch (RazorpayException e) {
            log.error("Failed to create Razorpay order", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Payment gateway error: " + e.getMessage(), "PAYMENT_GATEWAY_ERROR"));
        } catch (Exception e) {
            log.error("Failed to initiate Razorpay order", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Payment initiation error: " + e.getMessage(), "PAYMENT_INITIATION_ERROR"));
        }
    }

    /**
     * Step 2: Frontend sends back razorpay_order_id, razorpay_payment_id, razorpay_signature
     * after the checkout modal closes. Backend verifies the signature server-side BEFORE
     * marking the order as paid. A frontend "success" message alone is NOT sufficient.
     */
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/verify")
    public ResponseEntity<ApiResponse<Map<String, String>>> verifyPayment(
            @RequestBody Map<String, String> body) {

        if (!razorpayService.isEnabled()) {
            return ResponseEntity.ok(ApiResponse.success(Map.of(
                "status", "RAZORPAY_DISABLED")));
        }

        String razorpayOrderId  = body.get("razorpay_order_id");
        String razorpayPaymentId = body.get("razorpay_payment_id");
        String razorpaySignature = body.get("razorpay_signature");
        String appOrderId        = body.get("orderId");

        if (razorpayOrderId == null || razorpayPaymentId == null || razorpaySignature == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Missing required Razorpay verification parameters.", "INVALID_PARAMETERS"));
        }

        boolean valid = razorpayService.verifyPaymentSignature(razorpayOrderId, razorpayPaymentId, razorpaySignature);
        if (!valid) {
            log.warn("PAYMENT SIGNATURE INVALID for appOrder={} razorpayOrder={}", appOrderId, razorpayOrderId);
            return ResponseEntity.status(400)
                    .body(ApiResponse.error("Payment verification failed. Signature mismatch.", "SIGNATURE_MISMATCH"));
        }

        // Signature verified — update order payment status in DB
        // (OrderService.updateOrderStatus is the authoritative payment state update path)
        log.info("Payment verified successfully: appOrder={} razorpayPayment={}", appOrderId, razorpayPaymentId);
        return ResponseEntity.ok(ApiResponse.success(Map.of(
            "status", "VERIFIED",
            "razorpayPaymentId", razorpayPaymentId
        )));
    }

    /**
     * Razorpay webhook receiver. Razorpay calls this URL on payment events.
     * Signature is verified server-side using RAZORPAY_WEBHOOK_SECRET.
     */
    @PostMapping("/webhook")
    public ResponseEntity<String> handleWebhook(
            @RequestBody String rawBody,
            @RequestHeader(value = "X-Razorpay-Signature", required = false) String signature) {

        if (!razorpayService.isEnabled()) {
            return ResponseEntity.ok("OK");
        }

        if (signature == null || !razorpayService.verifyWebhookSignature(rawBody, signature)) {
            log.warn("Razorpay webhook signature verification FAILED");
            return ResponseEntity.status(400).body("Signature verification failed");
        }

        // TODO: Parse event type and update order payment state accordingly
        // Common events: payment.captured, payment.failed, order.paid
        log.info("Razorpay webhook received and verified. Event payload length={}", rawBody.length());
        return ResponseEntity.ok("OK");
    }
}
