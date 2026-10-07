# RR JAGGERY TRADERS — BUSINESS & LEGAL INFORMATION CHECKLIST BEFORE LAUNCH

> **Status:** Pre-Production Mandatory Input Document  
> **Target Audience:** Business Owner / Management  
> **Purpose:** Document all official legal, compliance, tax, and policy details required before the production domain and public deployment go live.

---

## 1. Legal & Entity Identification

| Item | Current Status / Placeholder | Required Business Value from Owner |
| :--- | :--- | :--- |
| **Registered Business Name** | `[LEGAL_ENTITY_NAME_PLACEHOLDER]` | Exact registered firm or proprietorship name (e.g., *RR Jaggery Traders*) |
| **Business Registration / Type** | `[BUSINESS_ENTITY_TYPE_PLACEHOLDER]` | Sole Proprietorship / Partnership / LLP / Private Limited |
| **GSTIN Registration Number** | `37AABCR1234F1Z5` *(Dev/Mock Format)* | Official 15-digit GSTIN issued by the Commercial Taxes Department (Andhra Pradesh state code `37`) |
| **FSSAI License / Registration No.** | `[FSSAI_LICENSE_NUMBER_PLACEHOLDER]` | 14-digit FSSAI Food Safety and Standards Authority of India License or Registration Number |
| **Registered Business Address** | `Vollurupallem, Thotlavalluru, Andhra Pradesh 5221163` | Complete postal address of primary office / manufacturing unit |
| **Legal Jurisdiction** | `[LEGAL_JURISDICTION_CITY_PLACEHOLDER]` | City/District court jurisdiction for dispute settlement (e.g., *Krishna District, Andhra Pradesh*) |

---

## 2. Customer Communication & Support Channels

| Item | Current Status / Placeholder | Required Business Value from Owner |
| :--- | :--- | :--- |
| **Customer Support Email** | `[SUPPORT_EMAIL_PLACEHOLDER]` | Dedicated business email address (e.g., `support@rrjaggery.com` or `sales@rrjaggery.com`) |
| **Customer Support Phone** | `[SUPPORT_PHONE_PLACEHOLDER]` | Verified helpline phone number(s) |
| **Support Working Hours** | `[SUPPORT_HOURS_PLACEHOLDER]` | Working hours & days (e.g., *Monday - Saturday, 9:00 AM - 6:00 PM IST*) |
| **Grievance Redressal Officer** | `[GRIEVANCE_OFFICER_NAME_PLACEHOLDER]` | Name and contact details of designated grievance officer as required under Indian IT Rules |

---

## 3. Shipping & Logistics Policies

| Item | Current Status / Placeholder | Required Business Value from Owner |
| :--- | :--- | :--- |
| **Courier / Logistics Partners** | `[COURIER_PARTNERS_PLACEHOLDER]` | Contracted couriers (e.g., *Delhivery, DTDC, India Post Speed Post, Blue Dart*) |
| **Serviceable Delivery Pin Codes** | `[SHIPPING_SERVICEABLE_REGIONS_PLACEHOLDER]` | Delivery regions (e.g., *Pan-India, South India, or selected state pin codes*) |
| **Order Dispatch Timeline** | `[DISPATCH_TIMELINE_PLACEHOLDER]` | Standard time from order confirmation to handover to courier (e.g., *24 to 48 business hours*) |
| **Average Delivery Transit Time** | `[ESTIMATED_DELIVERY_DAYS_PLACEHOLDER]` | Expected transit duration (e.g., *3 to 7 business days*) |
| **Free Delivery Order Threshold** | Currently hardcoded at `₹999` in commerce service | Confirm or adjust free shipping order minimum |
| **Standard Delivery Charge** | Currently hardcoded at `₹60` in commerce service | Confirm or adjust shipping fee for orders below threshold |

---

## 4. Refund & Cancellation Terms (Agricultural Consumables)

| Item | Current Status / Placeholder | Required Business Value from Owner |
| :--- | :--- | :--- |
| **Cancellation Window** | Allowed while status is `PENDING` or `CONFIRMED` | Confirm whether customers can self-cancel prior to `DISPATCHED` |
| **Transit Defect Reporting Window** | `[RETURN_NOTIFICATION_HOURS_PLACEHOLDER]` | Time window for customer to report transit damage / defective seal (e.g., *within 48 hours of delivery*) |
| **Required Proof for Claims** | Photos of packaging and shipping label | Confirm acceptable evidence (unboxing video, photographs) |
| **Refund Processing Timeline** | `[REFUND_PROCESSING_DAYS_PLACEHOLDER]` | Number of days to refund to original payment method (e.g., *5 to 7 working days*) |
| **Return of Food Items** | Non-returnable once seal is broken | Reaffirm policy that consumable agricultural produce cannot be returned once opened |

---

## 5. Payment Gateway & Financial Configuration

| Item | Current Status / Placeholder | Required Business Value from Owner |
| :--- | :--- | :--- |
| **Production Payment Gateway** | `TEST_PAYMENT` / `CASH_ON_DELIVERY` | Approved payment gateway provider (e.g., *Razorpay, Cashfree, PhonePe Payment Gateway*) |
| **Payment Gateway API Keys** | Missing / Not Configured | Production Key ID & Secret Key from payment gateway merchant dashboard |
| **Bank Account for Settlements** | Internal Merchant Bank Account | Current account details configured in merchant dashboard for daily payouts |
| **Applicable GST Rates** | `5%` for natural jaggery products | Verify GST HSN Code (`1701 13` / `1701 14`) and tax rate (standard 5% GST: 2.5% CGST + 2.5% SGST) |

---

## 6. Production Domain & Infrastructure Access

| Item | Current Status / Placeholder | Required Business Value from Owner |
| :--- | :--- | :--- |
| **Production Domain Name** | `[PRODUCTION_DOMAIN_PLACEHOLDER]` | Registered domain name (e.g., `rrjaggery.com` or `rrjaggerytraders.com`) |
| **DNS Management Provider** | Cloudflare recommended | Cloudflare DNS account access or active nameservers |
| **OCI / VPS Hosting Server** | Not yet provisioned (pre-deployment state) | ARM64 Ampere A1 (or standard VPS) public IP address & SSH key |
| **Production Secrets** | To be generated before launch | Strong random secrets for `JWT_SECRET`, `DB_PASSWORD`, `BOOTSTRAP_ADMIN_PASSWORD` |

---

## 7. Action Plan

1. Owner fills out the actual values in this document.
2. Update `frontend/src/components/LegalPages.tsx` and `frontend/src/App.tsx` with confirmed entity and contact information.
3. Configure production `.env` file on VPS with production secrets and payment gateway keys.
4. Execute deployment runbook in `DEPLOYMENT.md`.
