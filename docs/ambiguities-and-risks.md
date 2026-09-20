# RR Jaggery Traders — Ambiguities, Risks & Mitigation Strategies

In accordance with architectural principles, all key business ambiguities are logged with operational rationale, potential interpretations, and recommended implementation placeholders to prevent blocking progress.

---

## 1. Business Ambiguities & Open Decision Register

### Ambiguity 1: GST / HSN Classification and Multi-State Taxation
- **Question:** Does the business sell jaggery inter-state (IGST) or strictly intra-state (CGST + SGST), and are different jaggery products (raw organic blocks vs. fortified/flavored powders) taxed at different GST rates?
- **Why it matters:** Affects invoice generation, line-item tax calculations, and customer ledger amounts. Under Indian GST law, unrefined cane jaggery (HSN 1701/1702) is frequently exempt or taxed at 5%, while value-added or packaged goods may carry differing schedules.
- **Possible Interpretations:**
  1. Flat 5% GST across all jaggery products with intra-state split (2.5% CGST + 2.5% SGST).
  2. Rule-based tax calculation based on seller state (Karnataka/Tamil Nadu/Maharashtra/AP) vs customer shipping state.
- **Recommended Implementation Placeholder:** Configure a dynamic `gst_rate` (default `5.00%`) per product in the catalog and support an `is_interstate` flag on the invoice to split tax into `(CGST + SGST)` or `IGST`.
- **Decision Required:** Confirm RR Jaggery Traders' home operating state and official HSN codes.

---

### Ambiguity 2: Cane Farmer Sugarcane Pricing: Fixed Rate vs Brix/Recovery-Based Rate
- **Question:** How is raw sugarcane purchase price determined from farmers/suppliers?
- **Why it matters:** Some mills buy raw cane by straight weight (e.g., ₹3,200 per metric ton / Quintal), whereas others pay based on sucrose content (Brix level) or statutory Fair and Remunerative Price (FRP).
- **Possible Interpretations:**
  1. Weight-only flat rate contract agreed per purchase order or daily yard rate.
  2. Quality/Brix adjusted pricing formula upon weighbridge delivery.
- **Recommended Implementation Placeholder:** Implement purchase orders with a configurable `unit_price_per_quintal` agreed at GRN receipt, allowing manual price adjustment at weighment.
- **Decision Required:** Confirm whether Brix/quality testing is conducted at gate intake or if cane is purchased at flat negotiated quintal rates.

---

### Ambiguity 3: Wholesale Credit Limit Enforcement: Hard Block vs Warning Override
- **Question:** When an offline or registered wholesale customer exceeds their credit limit or has overdue invoices past their credit days, should the system strictly block order creation or allow admin managerial override?
- **Why it matters:** Rigid blocks can disrupt real-world commercial sales during peak harvest/festive seasons; unrestricted orders risk bad debts.
- **Possible Interpretations:**
  1. Hard blocking: System strictly throws `CREDIT_LIMIT_EXCEEDED` error.
  2. Soft blocking with audit override: System warns and requires an admin override PIN/flag.
- **Recommended Implementation Placeholder:** Default to soft blocking where `ROLE_ADMIN` can check an `allowCreditOverride` toggle with a mandatory reason note recorded in the audit log.
- **Decision Required:** Confirm credit policy rules for B2B wholesale buyers.

---

### Ambiguity 4: Piece-Rate / Per-KG Worker Payroll Calculation
- **Question:** How are manufacturing workers paid when salary type is set to `PER_KG_PRODUCED`?
- **Why it matters:** Jaggery processing teams often work in gangs/groups (e.g., 6 workers share a boiling pan producing 1,200 kg jaggery daily).
- **Possible Interpretations:**
  1. Individual worker produces and weighs specific output.
  2. Batch-based group pool: Total batch output kg is multiplied by rate per kg and divided equally among workers present in that batch shift.
- **Recommended Implementation Placeholder:** Support both individual piece-work and batch-shift group allocation linked to batch attendance records.
- **Decision Required:** Confirm whether mill labor is paid individually or via team pooling.

---

## 2. Technical & Project Risks Register

| Risk ID | Risk Description | Severity | Likelihood | Mitigation Strategy |
| :--- | :--- | :---: | :---: | :--- |
| **RSK-01** | **Premature Microservice Overhead:** Running 8 separate JVMs on a single low-memory VPS causing OOM crashes. | High | Medium | Configure lightweight JVM memory flags (`-XX:MaxRAMPercentage=75.0 -Xss256k`), optimize Spring Boot 3 startup with virtual threads, and share common libraries. |
| **RSK-02** | **Discrepancies in Stock Tracking:** Inventory drifting due to concurrent order checkouts and production draws. | High | Low | Implement database row-level locking (`PESSIMISTIC_WRITE`) during stock decrement operations and require all changes to pass through append-only `StockMovement` transactions. |
| **RSK-03** | **Offline Order Reconciliation Issues:** Staff forgetting to enter cash receipts promptly, distorting customer ledger balances. | Medium | High | Introduce daily cashier/admin settlement reports and overdue ledger alerts on the main executive dashboard. |
| **RSK-04** | **Data Loss on Single VPS Deployment:** VPS disk failure or corrupted PostgreSQL volume. | Critical | Low | Implement automated nightly cron database dumps encrypted and transferred to an off-site object storage bucket (e.g., S3/Cloudflare R2), with documented restoration procedures. |
| **RSK-05** | **Scope Creep into Advanced AI / Complex ERP features:** Attempting to build predictive ML demand forecasting or multi-warehouse logistics before core MVP stability. | Medium | High | Strictly defer Phase 2 items (forecasting, mobile PWA, payment gateways) until Sprints 1–7 are verified and stable. |
