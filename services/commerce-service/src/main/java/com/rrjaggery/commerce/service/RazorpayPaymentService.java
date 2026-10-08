package com.rrjaggery.commerce.service;

import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

/**
 * RazorpayPaymentService — Integration scaffolding for Razorpay payment gateway.
 *
 * STATUS: Architecture prepared. Production credentials NOT yet configured.
 * Set RAZORPAY_ENABLED=true and provide credentials via environment variables
 * before processing any live payments.
 *
 * Flow:
 *   1. Frontend triggers checkout → Backend calls createRazorpayOrder()
 *   2. Backend returns razorpay_order_id (+ public KEY_ID) to frontend
 *   3. Frontend opens Razorpay Checkout modal using the order_id
 *   4. On payment, Razorpay calls our webhook → verifyWebhookSignature()
 *   5. Backend also verifies payment on callback → verifyPaymentSignature()
 *   6. ONLY after server-side verification is the order marked as PAID
 *
 * The backend is AUTHORITATIVE. A frontend "success" message alone is NOT sufficient.
 */
@Service
public class RazorpayPaymentService {

    private static final Logger log = LoggerFactory.getLogger(RazorpayPaymentService.class);

    @Value("${razorpay.key.id:PENDING}")
    private String keyId;

    @Value("${razorpay.key.secret:PENDING}")
    private String keySecret;

    @Value("${razorpay.webhook.secret:PENDING}")
    private String webhookSecret;

    @Value("${razorpay.enabled:false}")
    private boolean razorpayEnabled;

    /**
     * Create a Razorpay order. Returns the Razorpay order_id that the frontend uses
     * to open the Checkout modal.
     *
     * @param amountInRupees  Exact order total (must match DB amount — backend is authoritative)
     * @param currency        "INR"
     * @param receiptRef      Internal order reference (e.g. our ORD-YYYYMMDD-XXXX number)
     * @return Razorpay order JSON string (contains id, amount, currency)
     * @throws RazorpayException  if Razorpay API call fails
     * @throws IllegalStateException  if Razorpay is not enabled
     */
    public String createRazorpayOrder(BigDecimal amountInRupees, String currency, String receiptRef)
            throws RazorpayException {

        assertEnabled();

        // Razorpay requires amount in smallest currency unit (paise for INR)
        long amountInPaise = amountInRupees.multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.HALF_UP).longValueExact();

        RazorpayClient client = new RazorpayClient(keyId, keySecret);
        JSONObject orderRequest = new JSONObject();
        orderRequest.put("amount", amountInPaise);
        orderRequest.put("currency", currency != null ? currency : "INR");
        orderRequest.put("receipt", receiptRef);
        orderRequest.put("payment_capture", 1); // Auto-capture after payment

        com.razorpay.Order razorpayOrder = client.orders.create(orderRequest);
        log.info("Razorpay order created: id={} for receipt={}", razorpayOrder.get("id"), receiptRef);
        return razorpayOrder.toString();
    }

    /**
     * Verify the Razorpay payment signature returned by the frontend after checkout.
     * This MUST be called server-side before marking any order as PAID.
     *
     * The frontend sends: razorpay_order_id + razorpay_payment_id + razorpay_signature.
     * We recompute HMAC-SHA256(order_id + "|" + payment_id, KEY_SECRET) and compare.
     *
     * @return true if signature is valid (payment is genuine)
     */
    public boolean verifyPaymentSignature(String razorpayOrderId, String razorpayPaymentId, String razorpaySignature) {
        assertEnabled();
        try {
            String payload = razorpayOrderId + "|" + razorpayPaymentId;
            String computed = hmacSha256(payload, keySecret);
            boolean valid = computed.equalsIgnoreCase(razorpaySignature);
            if (!valid) {
                log.warn("Razorpay payment signature MISMATCH for orderId={} paymentId={}", razorpayOrderId, razorpayPaymentId);
            }
            return valid;
        } catch (Exception e) {
            log.error("Error verifying Razorpay payment signature", e);
            return false;
        }
    }

    /**
     * Verify a Razorpay webhook signature.
     * Called from the webhook endpoint to authenticate incoming Razorpay event payloads.
     *
     * @param rawBody        Raw request body bytes exactly as received
     * @param headerSignature  Value of the X-Razorpay-Signature header
     * @return true if webhook payload is authentic
     */
    public boolean verifyWebhookSignature(String rawBody, String headerSignature) {
        assertEnabled();
        try {
            String computed = hmacSha256(rawBody, webhookSecret);
            boolean valid = computed.equalsIgnoreCase(headerSignature);
            if (!valid) {
                log.warn("Razorpay webhook signature MISMATCH");
            }
            return valid;
        } catch (Exception e) {
            log.error("Error verifying Razorpay webhook signature", e);
            return false;
        }
    }

    /**
     * Returns the public Razorpay Key ID for the frontend checkout script.
     * This is the ONLY Razorpay credential safe to expose to the browser.
     */
    public String getPublicKeyId() {
        assertEnabled();
        return keyId;
    }

    public boolean isEnabled() {
        return razorpayEnabled;
    }

    private void assertEnabled() {
        if (!razorpayEnabled) {
            throw new IllegalStateException(
                "Razorpay integration is not enabled. Set RAZORPAY_ENABLED=true and configure credentials.");
        }
    }

    private String hmacSha256(String data, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        mac.init(secretKey);
        byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(hash);
    }
}
