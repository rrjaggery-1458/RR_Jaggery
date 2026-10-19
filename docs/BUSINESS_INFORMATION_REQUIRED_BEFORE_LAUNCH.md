# RR JAGGERY — AUTHORITATIVE BUSINESS & COMPLIANCE CONFIGURATION

> **Status:** Configured for Pre-Production Deployment Testing  
> **Authority:** Business Owner Provided  
> **Notice:** DO NOT modify these authoritative values without explicit business owner instruction.

---

## 1. Legal & Entity Identification

| Item | Authoritative Value | Notes |
| :--- | :--- | :--- |
| **Business Name** | `RR Jaggery` | Standard business name across all customer-facing surfaces |
| **Business Type** | `Jaggery Manufacturing` | Manufacturing unit |
| **GST Registration** | **NOT GST REGISTERED** | **CRITICAL:** Do NOT display GSTIN anywhere. No GSTIN: NA, no fake GSTIN, no GST claims. Zero GST charged on orders. |
| **FSSAI Number** | None available | Do NOT invent or display any FSSAI number |
| **Registered Business Address** | `Vallurupallem, Thotlavalluru, Andhra Pradesh, PIN 521163` | PIN is exactly `521163` |
| **Legal Jurisdiction** | `Krishna District / Vijayawada, Andhra Pradesh` | District court jurisdiction |

---

## 2. Customer Communication & Support Channels

| Item | Authoritative Value | Notes |
| :--- | :--- | :--- |
| **Support Email** | `rrjaggery@gmail.com` | Official support email |
| **Support Phone** | `9948533315` | Helpline phone & WhatsApp |
| **Support Hours** | `10:00 AM – 6:00 PM` | Monday – Saturday |
| **Grievance Officer** | `CH Padma` | Mandated under Indian IT Rules |

---

## 3. Shipping & Logistics Policies

| Item | Authoritative Value | Notes |
| :--- | :--- | :--- |
| **Primary Fast-Delivery Region** | `Vijayawada and Guntur` | Fast delivery available in and around Vijayawada and Guntur |
| **Other Locations** | Subject to office confirmation | "Fast delivery is available in and around Vijayawada and Guntur. For shipping to other locations, please contact our office for available shipping options." |
| **Delivery Timeline** | `3–5 business days` | Standard delivery window |
| **Shipping Fees** | Free above ₹500; ₹50 below ₹500 | Calculated transparently at checkout |

---

## 4. Refund & Cancellation Policy

| Item | Authoritative Value | Notes |
| :--- | :--- | :--- |
| **Refund Policy** | **NO REFUND** | Clear customer-facing wording. No promises of refunds or processing periods. |
| **Cancellation** | Customer may cancel only while `PENDING` | Cancellation does not grant monetary refund. |
| **Food Product Notice** | Consumable agricultural produce | Opened packages cannot be returned. |

---

## 5. Domain Configuration

| Item | Value | Notes |
| :--- | :--- | :--- |
| **Temporary Deployment / Testing Domain** | `rrdentalneeds.online` | Initial deployment and testing environment only |
| **Permanent Future Domain** | `rrjaggery.com` | Future domain — NOT currently active |
| **Configuration Principle** | Environment-configurable | Fully replaceable via `.env` (`DOMAIN_NAME`, `VITE_API_BASE_URL`, `ALLOWED_ORIGINS`) without code changes |

---

## 6. Payment Gateway Preparation (Razorpay)

| Item | Status | Notes |
| :--- | :--- | :--- |
| **Gateway** | Razorpay | Architecture prepared: order creation, frontend checkout script, server-side signature verification, webhook handler |
| **Production Credentials** | **PENDING FROM OWNER** | `RAZORPAY_KEY_ID`, `RAZORPAY_KEY_SECRET`, `RAZORPAY_WEBHOOK_SECRET` |
| **Current Operational Mode** | Scaffolding prepared (`RAZORPAY_ENABLED=false`) | Live payments inactive until owner supplies production credentials |
| **Authority** | Server-side authoritative | Orders are NEVER marked paid solely by frontend report |
