# RR Jaggery Traders — Implementation Status Matrix

**Current Status:** ACCESS MODEL REVISION COMPLETED & VERIFIED (PRE-SPRINT 6)

Sprint 0 — **ACCEPTED**  
Sprint 1 — **ACCEPTED**  
Sprint 2 — **ACCEPTED**  
Sprint 3 — **ACCEPTED**  
Sprint 4 — **ACCEPTED**  
Sprint 5 — **ACCEPTED**  
Access Model Revision — **COMPLETED & VERIFIED** (Pre-Sprint 6)

---

## Pre-Sprint 6: Business Access Model Revision Summary

### Core Architecture & Authorization Changes:
* **3-Role Authoritative Login Model**: Exactly 3 application login roles: `ADMIN`, `MANAGER`, and `CUSTOMER`.
* **Manager Role Added**: `MANAGER` role created with full operational access across Inventory, Procurement, Mill Production, Customer Ledger, and Product Catalogue.
* **No-Login Business Records**: Wholesale customers (`customer_schema.customers`) and mill workforce records are internal business entities with NO portal logins. Self-registration is strictly for Retail Customers.
* **502 Bad Gateway Definitively Resolved**: Excluded conflicting `UserDetailsServiceAutoConfiguration` across all 8 microservices, allowing JWT stateless authentication to operate cleanly without competing in-memory Basic Auth challenges.
* **Admin User Management**: Admin endpoints (`/api/v1/auth/admin/**`) for creating Managers, listing accounts, and toggling enable/disable status.
* **Executive Real-Data Overview**: Real-time aggregation across all active microservices (Sprints 0–5) with "Not available yet (Sprint 6)" placeholders for future finance/payroll metrics.

---

## Sprint 5 Execution & Verification Summary (Production Domain)

### Implemented Features:
* **Production Recipes & BOM (`services/production-service`)**:
  * Recipe master data entity model (`recipes`, `recipe_items`) with stage definitions (CRUSHING, BOILING, CLARIFYING, SETTING, COOLING, PACKING).
  * Strict `BigDecimal` ratios, input raw materials, output finished goods, standard yield %, and waste tolerance thresholds.
  * Standard Mandya Cane Jaggery Block and Organic Powder recipes seeded.
* **Production Batch Lifecycle & Execution (`services/production-service`)**:
  * `production_batches` entity with deterministic numbering (`BATCH-YYYYMMDD-XXXX`), lot numbering (`LOT-YYYYMMDD-XXXX`), and state machine validation:
    `PLANNED → MATERIALS_READY → IN_PRODUCTION → QUALITY_CHECK → COMPLETED` (or `CANCELLED`).
  * Invalid state transitions rejected with `400 BUSINESS_RULE_VIOLATION`.
  * Auto-transition from `PLANNED`/`MATERIALS_READY` to `IN_PRODUCTION` upon first material consumption.
  * Strict immutability once batch is `COMPLETED` or `CANCELLED`.
* **Raw Material Consumption & Inventory Integration**:
  * `batch_consumptions` entity tracking consumed SKU, quantity, stage, temperature, Brix, moisture, and operator.
  * Negative stock protection: consumption queries `inventory-service` via REST client (`InventoryStockClient`) and rejects if stock is insufficient.
  * Automatic `inventory-service` deduction with movement type `PRODUCTION_CONSUMPTION` and reference `PRODUCTION_BATCH`.
  * Idempotency guarantee via `idempotencyKey` preventing duplicate stock deductions on network retries.
* **Finished Goods Output & Automatic Yield Calculation**:
  * `batch_outputs` entity recording produced SKU, quantity, quality grade (`GRADE_A`, `GRADE_B`, `COMMERCIAL`), Brix/sucrose purity, and batch lot.
  * Automatic `inventory-service` stock credit with movement type `PRODUCTION_OUTPUT`.
  * Automatic yield percentage computation (`(actualQuantity / plannedQuantity) * 100`) stored on batch completion.
  * Batch Lot traceability preserved from batch creation through output and inventory movement ledger.
* **Wastage & Loss Tracking**:
  * `batch_wastages` entity logging loss reason (`BOILING_EVAPORATION`, `SCUM_REMOVAL`, `EQUIPMENT_RESIDUE`, `SPILLAGE`), stage, quantity, and notes.
  * Inventory stock deduction with movement type `LOSS` for auditable waste reconciliation.
* **Service Boundaries & Zero Cross-Schema SQL**:
  * `production-service` has exclusive ownership of `production_schema`.
  * Zero direct SQL or cross-schema joins to `inventory_schema` or `commerce_schema`.
  * Inter-service communication with `inventory-service` exclusively via internal JWT-authenticated REST APIs using Docker service DNS (`http://inventory-service:8084`).
* **Operational Frontend (`frontend/src/Sprint5Production.tsx`)**:
  * Full Mill Production Operations dashboard: Active Batches, Completed Today, Avg Yield %, Wastage Rate %.
  * Interactive batch planning modal, stage transition controls, material consumption drawer, output recording modal, wastage logging, and batch detail drawer.
  * Clean TypeScript compilation and Vite production build.

### Verification Evidence:
* **Full Backend Maven Reactor**: 9 / 9 modules compiled and tested with `BUILD SUCCESS` (0 failures, 0 errors).
* **Integrated Multi-Container Runtime Acceptance**: 26 / 26 Acceptance Gates **PASSED** against live Docker Compose stack with PostgreSQL 16 and Redis 7.
* **PostgreSQL Restart Persistence**: Verified batch state, consumptions, outputs, wastages, and stock movements persist accurately across container restarts.

---

## Sprint Roadmap

| Sprint | Description | Status |
| :--- | :--- | :---: |
| **Sprint 0** | Foundation & Architecture | **COMPLETED & ACCEPTED** |
| **Sprint 1** | Authentication & Product Catalogue | **COMPLETED & ACCEPTED** |
| **Sprint 2** | Cart, Checkout & Orders | **COMPLETED & ACCEPTED** |
| **Sprint 3** | Customer, Wholesale & Ledger (Offline B2B P0) | **COMPLETED & ACCEPTED** |
| **Sprint 4** | Inventory & Procurement (Auditable Stock Movements) | **COMPLETED & ACCEPTED** |
| **Sprint 5** | Production Management (Batch, BOM, Wastage, Yield) | **COMPLETED & ACCEPTED** |
| **Sprint 6** | Costing, Expenses & Payroll (BigDecimal, Cost/KG) | `Ready to Start` |
| **Sprint 7** | Dashboards, Reports & Notifications | `Not Started` |
| **Sprint 8** | Hardening, Security, Audit Logging & Backup Testing | `Not Started` |
| **Sprint 9** | OCI Ampere A1 ARM64 Production Deployment & Stabilization | `Not Started` |
