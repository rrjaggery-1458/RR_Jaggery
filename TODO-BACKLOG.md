# RR Jaggery Traders — TODO & Product Backlog

## 1. Immediate Sprint 0 Execution Backlog
- [x] Initialize Git repository and commit planning artifacts.
- [x] Create `.gitignore` tailored for Java 21, Maven/Gradle, Node.js, and Docker.
- [x] Scaffold React + TypeScript + Vite + Tailwind CSS frontend in `frontend/`.
- [x] Scaffold Spring Boot 3 / Java 21 service templates in `services/`:
  - `auth-service`
  - `commerce-service`
  - `customer-ledger-service`
  - `inventory-service`
  - `procurement-service`
  - `production-service`
  - `finance-service`
  - `notification-service`
- [x] Implement shared configuration conventions (Actuator `/health`, standard error response, CORS).
- [x] Create `docker-compose.yml` defining PostgreSQL 16, Redis 7, backend service templates, and Nginx.
- [x] Create PostgreSQL schema initialization script creating all 8 isolated schemas.
- [x] Create `.env.example` with standard development variables.
- [x] Scaffold GitHub Actions workflow (`.github/workflows/ci.yml`) to verify frontend and backend builds.
- [x] Verify local builds, run unit tests, verify Docker setup, and document evidence.

---

## 2. Business Clarifications Needed from Stakeholders
- [ ] **GST / HSN Rules:** Confirm default tax percentages and whether inter-state shipments (IGST) apply to wholesale deliveries.
- [ ] **Sugarcane Sourcing Rates:** Confirm whether cane purchase from farmers is by flat metric ton / quintal weight or adjusted by juice sucrose/Brix testing.
- [ ] **Credit Limit Enforcement:** Confirm if admin manager can override credit limit blocks with logged justifications.
- [ ] **Piece-Rate Wage Model:** Clarify whether per-KG production wages are paid individually or pooled across the boiling pan shift team.

---

## 3. Technical Debt & Architecture Guardrails
- [ ] Keep JVM heap allocations strictly capped (`-Xmx384m`) across microservice Docker containers to prevent VPS Out-of-Memory events.
- [ ] Ensure all financial and weight calculations avoid floating-point types (`BigDecimal` only).
- [ ] Verify that no cross-service direct SQL queries or database joins exist.
- [ ] Ensure zero passwords or secrets are committed to the repository.

---

## 4. Phase 2 / Future Enhancements Backlog
- [ ] Razorpay / UPI Payment Gateway integration for retail online checkout.
- [ ] Automated WhatsApp / SMS transactional updates to wholesale buyers.
- [ ] Barcode / QR Code scanning for goods receipt and pallet/box dispatch.
- [ ] Multi-warehouse location inventory tracking.
- [ ] Predictive demand and raw material requirement forecasting.
