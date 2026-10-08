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

import java.util.Map;
import java.util.UUID;

/**
 * RazorpayController — Razorpay payment integration endpoints.
 *
 * STATUS: Integration prepared. RAZORPAY_ENABLED=false until credentials are configured.
 *
 * Endpoints (all disabled when RAZORPAY_ENABLED=false):
 *   POST /api/v1/commerce/payments/razorpay/create-order
 *       → Create Razorpay order for an existing pending order
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
     * Step 1: Frontend requests a Razorpay order for a confirmed application order.
     * Returns razorpay_order_id + public key_id for the frontend Checkout script.
     *
     * The amount is fetched from DB — never trusted from the client request.
     */
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/create-order")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createRazorpayOrder(
            @RequestBody Map<String, String> body) {

        if (!razorpayService.isEnabled()) {
            return ResponseEntity.ok(ApiResponse.success(Map.of(
                "status", "RAZORPAY_DISABLED",
                "message", "Razorpay integration is not yet enabled. Payment credentials pending from business owner."
            )));
        }

        try {
            UUID orderId = UUID.fromString(body.get("orderId"));
            // Fetch order from DB — amount is AUTHORITATIVE from backend, never from client
            var order = orderService.getOrderById(orderId, null, true);

            String razorpayOrderJson = razorpayService.createRazorpayOrder(
                    order.getTotalAmount(), "INR", order.getOrderNumber());

            return ResponseEntity.ok(ApiResponse.success(Map.of(
                "razorpayOrder", razorpayOrderJson,
                "keyId", razorpayService.getPublicKeyId(), // Public key only — safe to send
                "orderNumber", order.getOrderNumber()
            )));
        } catch (RazorpayException e) {
            log.error("Failed to create Razorpay order", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Payment gateway error. Please try again.", "PAYMENT_GATEWAY_ERROR"));
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
