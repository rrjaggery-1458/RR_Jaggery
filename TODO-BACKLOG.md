# RR Jaggery Traders — TODO & Product Backlog

## 1. Completed & Accepted Sprints
- [x] **Sprint 0:** Foundation & Architecture (Microservices scaffolding, PostgreSQL multi-schema, Docker Compose, Nginx, CI/CD).
- [x] **Sprint 1:** Authentication & Product Catalogue (Stateless JWT, RBAC, Storefront & Admin Catalogue).
- [x] **Sprint 2:** Cart, Checkout & Orders (Persistent cart, pricing engine, transaction-safe checkout, GST invoice, order lifecycle).
- [x] **Sprint 3:** Customer, Wholesale & Ledger (Offline B2B wholesale, credit cycles, partial payments, running ledger balance).
- [x] **Sprint 4:** Inventory & Procurement (Raw materials/FG stock ledger, POs, Goods Receipts, auditable stock movements).
- [x] **Sprint 5:** Production Management (Recipe/BOM master, batch execution, material consumption, output recording, yield %, loss tracking, multi-container runtime verified).

---

## 2. Next Up — Sprint 6 Backlog (Costing, Expenses & Payroll)
- [ ] **Direct & Operating Expenses (`services/finance-service`)**:
  - Direct production expenses (firewood, transport, bagasse fuel, processing chemicals/lime).
  - Indirect & overhead operating expenses (electricity, mill maintenance, packaging supplies).
  - Expense categories and payment mode tracking (CASH, BANK_TRANSFER, UPI).
- [ ] **Batch Costing Engine**:
  - Aggregation of raw material costs (from inventory/procurement PO cost).
  - Apportionment of direct boiling/processing expenses to batches.
  - Final calculated Cost per KG for finished goods lots.
- [ ] **Employee Attendance & Payroll**:
  - Employee master (boiling masters, crusher operators, general mill workers).
  - Attendance logging (daily shift, half day, overtime).
  - Wage calculation: Monthly fixed salary vs Piece-rate production wages (₹ per KG produced / boiled).
  - Advance payment tracking and net monthly payout slip generation.
- [ ] **Frontend Finance & Payroll Console (`frontend/`)**:
  - Expense voucher entry, batch cost analyzer, worker attendance ledger, and wage sheet generator.

---

## 3. Business Clarifications Needed from Stakeholders
- [ ] **Piece-Rate Wage Model:** Clarify whether per-KG production wages are paid individually or pooled across the boiling pan shift team.
- [ ] **Expense Allocation Method:** Confirm default allocation basis for firewood/fuel across parallel batches (equal split vs weight-weighted).
- [ ] **GST / HSN Rules:** Confirm default tax percentages and whether inter-state shipments (IGST) apply to wholesale deliveries.
- [ ] **Sugarcane Sourcing Rates:** Confirm whether cane purchase from farmers is by flat metric ton / quintal weight or adjusted by juice sucrose/Brix testing.

---

## 4. Technical Debt & Architecture Guardrails
- [x] Keep JVM heap allocations strictly capped (`-Xmx384m`) across microservice Docker containers to prevent VPS Out-of-Memory events.
- [x] Ensure all financial and weight calculations avoid floating-point types (`BigDecimal` only).
- [x] Verify that no cross-service direct SQL queries or database joins exist.
- [x] Ensure zero passwords or secrets are committed to the repository.

---

## 5. Phase 2 / Future Enhancements Backlog
- [ ] Razorpay / UPI Payment Gateway integration for retail online checkout.
- [ ] Automated WhatsApp / SMS transactional updates to wholesale buyers.
- [ ] Barcode / QR Code scanning for goods receipt and pallet/box dispatch.
- [ ] Multi-warehouse location inventory tracking.
- [ ] Predictive demand and raw material requirement forecasting.
