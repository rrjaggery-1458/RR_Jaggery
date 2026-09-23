# Changelog

All notable changes to the **RR Jaggery Traders** platform will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.5.0] - Pre-Sprint 6: Business Access Model Revision & Gateway 502 Resolution

### Added
- **3-Role Authoritative Access Model (`ADMIN`, `MANAGER`, `CUSTOMER`):**
  * `MANAGER` role added to `Role` enum and `SecurityConstants` with full operational permissions across Inventory, Procurement, Mill Production, Customer Ledger, and Product Catalogue.
  * `AdminUserController` and `AdminUserService` in `auth-service` supporting Admin creation of Managers (`POST /api/v1/auth/admin/managers`), listing all user accounts (`GET /api/v1/auth/admin/users`), editing details (`PUT /api/v1/auth/admin/users/{id}`), toggling login status (`PATCH /api/v1/auth/admin/users/{id}/enabled`), and real-time user statistics (`GET /api/v1/auth/admin/stats`).
  * Default seed for Operations Manager (`manager@rrjaggery.com` / `Manager@123`).
  * Admin User Management UI in frontend for managing internal staff accounts.
  * Executive Overview Real-Data Dashboard in frontend aggregating live metrics from active microservices (Sprints 0–5) with explicit "Not available yet (Sprint 6)" placeholders for future finance/payroll metrics.

### Changed
- **502 Bad Gateway Root Cause Fix:**
  * Excluded `UserDetailsServiceAutoConfiguration` across all 8 Spring Boot microservices (`auth-service`, `commerce-service`, `customer-ledger-service`, `inventory-service`, `procurement-service`, `production-service`, `finance-service`, `notification-service`).
  * Resolved conflict where default in-memory UserDetailsService triggered Basic Auth challenge on JWT bearer requests, returning 401 upstream and 502 at the Nginx reverse proxy.
- **Role & Authorization Migration:**
  * Replaced legacy `PRODUCTION_MANAGER` and `EMPLOYEE` in all `@PreAuthorize` method annotations across all controllers with `ADMIN` and `MANAGER`.
  * Preserved `customer_schema.customers` wholesale business records (no portal login, managed internally by Admin/Manager).
  * Safely disabled portal login for `wholesale@example.com` (`enabled = false`) while preserving classification (`customer_type = REGISTERED_WHOLESALE`).
  * Updated self-registration in `auth-service` and frontend to strictly create `CUSTOMER` role with `RETAIL` customer type.

---

## [1.4.0] - Sprint 5: Production Management (Batches, BOM/Recipes, Consumption, Output, Wastage, Yield)

### Added
- **Production Recipes & BOM Master (`services/production-service`):**
  * `Recipe` and `RecipeItem` JPA entities in `production_schema` with support for stage definitions (`CRUSHING`, `BOILING`, `CLARIFYING`, `SETTING`, `COOLING`, `PACKING`).
  * Strict `BigDecimal` ratio calculations for input raw materials, standard yield percentage targets, and waste tolerances.
  * Deterministic seeding for authentic Mandya Cane Jaggery Block and Organic Powder recipes.
  * Recipe CRUD endpoints (`/api/v1/production/recipes/**`) protected by `ADMIN` and `PRODUCTION_MANAGER` roles.
- **Production Batch Lifecycle & State Machine (`services/production-service`):**
  * `ProductionBatch` entity with formatted batch numbering (`BATCH-YYYYMMDD-XXXX`) and lot identifiers (`LOT-YYYYMMDD-XXXX`).
  * Finite state machine: `PLANNED → MATERIALS_READY → IN_PRODUCTION → QUALITY_CHECK → COMPLETED` (or `CANCELLED`).
  * Strict validation rejecting invalid stage skipping with HTTP 400 (`BUSINESS_RULE_VIOLATION`).
  * Auto-transition to `IN_PRODUCTION` upon initial material consumption.
  * Strict immutability once batch is `COMPLETED` or `CANCELLED`.
- **Material Consumption & Stock Deduction Integration (`services/production-service`):**
  * `BatchConsumption` tracking raw material consumption by SKU, quantity, stage, temperature, Brix, moisture, and operator.
  * Negative-stock protection: queries `inventory-service` via REST client (`InventoryStockClient`) and rejects if stock is insufficient.
  * Automatic `inventory-service` stock deduction with movement type `PRODUCTION_CONSUMPTION` and reference `PRODUCTION_BATCH`.
  * Idempotent consumption handling via unique `idempotencyKey`.
- **Finished Goods Output & Automatic Yield Analysis (`services/production-service`):**
  * `BatchOutput` recording finished goods SKU, quantity, quality grade (`GRADE_A`, `GRADE_B`, `COMMERCIAL`), Brix/purity, and lot number.
  * Automatic `inventory-service` stock credit with movement type `PRODUCTION_OUTPUT`.
  * Automatic yield percentage computation (`(actualQuantity / plannedQuantity) * 100`) stored on batch completion.
  * Batch Lot traceability preserved from batch creation through output and inventory movement ledger.
- **Wastage & Loss Tracking (`services/production-service`):**
  * `BatchWastage` tracking process losses (`BOILING_EVAPORATION`, `SCUM_REMOVAL`, `EQUIPMENT_RESIDUE`, `SPILLAGE`).
  * Stock adjustment integration with movement type `LOSS` for auditable waste reconciliation.
- **Service Boundaries & Docker Networking:**
  * Exclusive schema isolation: `production_schema` owned exclusively by `production-service`. Zero direct SQL or cross-schema joins.
  * All inter-service calls use internal JWT-authenticated REST APIs with Docker internal DNS (`http://inventory-service:8084`).
- **OpenAPI & Swagger Documentation:**
  * Comprehensive OpenAPI documentation on `GET /v3/api-docs` exposing 13 production endpoints.
- **Mill Production Frontend UI (`frontend/src/Sprint5Production.tsx`):**
  * Production Dashboard displaying active batches, completion stats, average yield %, and wastage rate.
  * Interactive batch planning modal, stage progression triggers, material consumption drawer, output recording modal, and wastage logger.
- **Integrated Verification & Persistence:**
  * 100% PASS across 26 Acceptance Gates executed against live Docker Compose multi-container stack.
  * Verified PostgreSQL restart persistence for all production batches, consumption records, output records, wastages, and stock movements.

---

## [1.3.0] - Sprint 4: Inventory & Procurement

### Added
- **Inventory ledger and stock integrity (`services/inventory-service`)**:
  * `inventory_items` and `stock_movements` with `BigDecimal`-based quantity tracking and movement audit records.
  * negative-stock enforcement at the service boundary to prevent invalid stock reductions.
  * create/adjust/receive endpoints for stock updates and movement history queries.
- **Procurement workflow (`services/procurement-service`)**:
  * supplier master data, purchase orders, and goods receipt capture.
  * strict validation for order quantity and receipt quantity before inventory is updated.
  * `PURCHASE_ORDER` receipt integration that calls the inventory service to increase stock.
- **Testing & Verification**:
  * targeted unit/integration validation for negative-stock rules, stock movement persistence, and procurement receipt flow.
  * confirmed Commerce orders call Inventory through the REST service boundary for idempotent finished-goods SALE deductions.
  * Inventory and Procurement operational endpoints enforce role-based authorization.
  * Live Docker/PostgreSQL multi-container runtime acceptance fully verified and accepted.

---

## [1.2.0] - Sprint 2: Cart, Checkout & Orders

### Added
- **Persistent Shopping Cart & Server-Side Pricing Engine (`services/commerce-service`):**
  * Database entity persistence in `commerce_schema.cart_items` bound to authenticated `userId`.
  * Authoritative server-side price calculation enforcing Retail vs Wholesale pricing based on customer type and MOQ.
  * Cart item management endpoints: add (`POST /api/v1/commerce/cart/items`), update quantity (`PUT /api/v1/commerce/cart/items/{id}`), remove (`DELETE /api/v1/commerce/cart/items/{id}`), and get cart (`GET /api/v1/commerce/cart`).
- **Atomic Transaction-Safe Checkout & Order Engine (`services/commerce-service`):**
  * Database entity persistence in `commerce_schema.orders` and `commerce_schema.order_items`.
  * Sequential unique order numbering format: `ORD-YYYYMMDD-XXXX`.
  * Spring `@Transactional` checkout clearing the user's cart and persisting order with immutable line totals.
  * Server-side authoritative recalculation of subtotal, GST (5% Organic Cane Jaggery), shipping fee (free > ₹500), and grand total using strict `BigDecimal`.
- **Order Lifecycle & Payment State Handling (`services/commerce-service`):**
  * Order status transitions: `PENDING`, `CONFIRMED`, `PROCESSING`, `DISPATCHED`, `DELIVERED`, `CANCELLED`.
  * Payment status handling: `PENDING`, `PAID`, `REFUNDED` with support for `TEST_PAYMENT` and `CASH_ON_DELIVERY`.
  * Customer order cancellation endpoint (`PUT /api/v1/commerce/orders/{id}/cancel`) restricted to `PENDING`/`CONFIRMED` states.
  * Admin order status update endpoint (`PUT /api/v1/commerce/admin/orders/{id}/status`).
- **GST Tax Invoice Engine & Printable Modal:**
  * Invoice generation with Mandya mill details: `RR JAGGERY TRADERS`, Mandya, Karnataka, GSTIN `29AABCR1234F1Z5`.
  * Accurate HSN Code `17011490` (Cane Jaggery) with CGST (2.5%) and SGST (2.5%) breakdown.
  * Dedicated invoice data endpoint (`GET /api/v1/commerce/orders/{id}/invoice`).
  * React `InvoiceModal` with printable styling (`window.print()`), mill header, customer billing/shipping details, itemized tax table, and totals breakdown.
- **Frontend Cart, Checkout & Orders UI (`frontend/src`):**
  * `CartDrawer` with live badge count, item controls, and financial summary.
  * `CheckoutModal` with recipient name, phone, full address, notes, and payment method selector.
  * `OrderConfirmationModal` with order number, payment confirmation, and direct links to invoice and history.
  * `OrderHistoryView` displaying all placed orders with real-time status badges and cancel triggers.
  * `AdminOrderManagement` fulfillment console with status filters, inline stage transition dropdowns, and invoice inspection.
- **Verification & Automation:**
  * Automated JUnit 5 + MockMvc test suite in `commerce-service` expanded to 10 tests (all passing).
  * 15-step End-to-End browser verification in Microsoft Edge (`frontend/e2e_sprint2_verification.cjs`) verifying complete customer checkout, GST invoice, and admin fulfillment with 0 console or network errors.

## [1.1.0] - Sprint 1: Authentication & Product Catalogue

### Added
- **Stateless JWT Security & Role Authorization (`services/common-library`):**
  - `JwtTokenProvider` HMAC-SHA256 token generation, expiration enforcement, and claims parsing.
  - `JwtAuthenticationFilter` processing Bearer tokens and authenticating Spring `SecurityContextHolder`.
  - Standardized role constants (`ROLE_ADMIN`, `ROLE_CUSTOMER`, `ROLE_PRODUCTION_MANAGER`, `ROLE_EMPLOYEE`).
- **Authentication & User Management Service (`services/auth-service`):**
  - JPA entity persistence in `auth_schema.users` with support for `RETAIL`, `REGISTERED_WHOLESALE`, and internal roles.
  - Spring Security `BCryptPasswordEncoder` for credential protection.
  - Public registration (`POST /api/v1/auth/register`) and login (`POST /api/v1/auth/login`).
  - Authenticated profile endpoint (`GET /api/v1/auth/me`).
  - Deterministic development seed data for Admin, Retail, and Wholesale customer accounts.
  - Complete unit and integration test suite passing (6/6 tests).
- **Product & Category Catalogue Master Service (`services/commerce-service`):**
  - JPA entities in `commerce_schema.categories` and `commerce_schema.products` using strict `BigDecimal` for currency and weights.
  - Public catalogue endpoints (`GET /api/v1/commerce/categories`, `GET /api/v1/commerce/products`) with search and category filtering.
  - Role-protected Admin management endpoints (`/api/v1/commerce/admin/**` requiring `ROLE_ADMIN`).
  - Support for authentic Mandya jaggery grades and packaging configurations.
  - Complete unit and integration test suite passing (5/5 tests).
- **React Frontend Storefront & Admin UI (`frontend/src`):**
  - `AuthContext` and `AuthModal` with tabbed login and registration for Retail & Wholesale users.
  - Public Storefront with category filter pills, grade badges, search bar, and `ProductDetailModal`.
  - Role-protected Admin Product Master console with search, category filters, `+ Add Product` modal, `+ Add Category` modal, status toggles, and edit modals.
  - Clean production build with Vite + TypeScript (`0 errors`).
- **End-to-End Browser Automation Harness (`frontend/e2e_full_verification.cjs`):**
  - Verified 14 end-to-end user flows in headless Microsoft Edge browser: public browsing, search/filter, detail modals, registration, invalid login rejections, Admin CRUD persistence, active status toggling, and localStorage reload persistence.

## [1.0.0] - Sprint 0: Architecture & Foundation

### Added
- **Requirements Analysis:** Extracted and fully analyzed official requirements from `RR_Jaggery_Traders_Product_Requirements_and_Agile_Sprint_Plan (1).docx`.
- **System Architecture Documentation:**
  - `docs/architecture.md`: Overall topology, design principles, technology stack matrix, and high-level Mermaid architecture diagram.
  - `docs/service-boundaries.md`: Strict boundaries for all 8 microservices, port allocations, entity lists, and dependency mapping.
  - `docs/domain-model.md`: Comprehensive domain model, Entity-Relationship Diagram (ERD), schema mappings, and exact data types (strict `BigDecimal` for currency and weights).
  - `docs/database-ownership.md`: PostgreSQL schema segregation policy (`auth_schema`, `commerce_schema`, `customer_schema`, etc.) and prohibition of cross-schema joins.
  - `docs/api-boundaries.md`: REST guidelines, response envelope format, standard status codes, and complete endpoint catalog.
  - `docs/data-flows.md`: Sequence diagrams for end-to-end supply-to-sale, offline wholesale lifecycle, batch production & costing, and ledger balance audits.
  - `docs/security-model.md`: Stateless JWT + Redis revocation, RBAC permissions, BCrypt password hashing, and OWASP defenses.
  - `docs/technical-decisions.md`: Architectural Decision Records (ADR 001 through ADR 006).
  - `docs/ambiguities-and-risks.md`: Formal register of business questions (GST, cane pricing, credit limits, piece-rate wages) and risk mitigations.
  - `docs/deployment-architecture.md`: Low-cost single VPS deployment plan, Docker Compose resource limits, and automated backup strategies.
  - `docs/testing-strategy.md`: Testing pyramid, toolsets, and high-priority domain test scenarios.
- **Sprint & Agile Management:**
  - Initialized `IMPLEMENTATION_STATUS.md` tracking all product features from Sprint 0 to Sprint 9.
  - Initialized `TODO-BACKLOG.md` tracking immediate, future, and technical debt items.
  - Initialized `README.md` with full project overview and setup guidelines.
- **Frontend Application Shell:**
  - Scaffolded React 18 + TypeScript + Vite + Tailwind CSS application in `frontend/`.
  - Implemented responsive, high-aesthetic dark-mode UI with dual persona toggles (Customer Storefront and Admin ERP Operations Center).
  - Built Service Mesh connection status monitor, workflow architecture visualizer, and Sprint Roadmap tracker.
  - Production bundle verified with `npm run build` (0 TypeScript / bundling errors).
- **Backend Service Templates (Java 21 & Spring Boot 3.3.4):**
  - Created multi-module Maven structure with parent POM in `services/pom.xml`.
  - Implemented `common-library` with uniform `ApiResponse<T>`, `ErrorResponse`, `FieldErrorDetail`, and `GlobalExceptionHandler`.
  - Scaffolded all 8 logical microservices (`auth`, `commerce`, `customer-ledger`, `inventory`, `procurement`, `production`, `finance`, `notification`) on dedicated ports (8081-8088).
  - Implemented REST health endpoints (`/api/v1/{service}/health`) returning service status, port, and owned database schema.
  - Added JUnit 5 + MockMvc test suites across all 8 services; 100% tests passing (`BUILD SUCCESS` across all 10 modules).
- **Docker & DevOps Infrastructure:**
  - PostgreSQL 16 multi-schema initialization script (`infrastructure/docker/postgres/init-schemas.sql`) creating all 8 domain schemas.
  - Multi-stage Dockerfile (`infrastructure/docker/Dockerfile.service`) with Alpine runtime and low-cost VPS memory tuning.
  - Reverse proxy configuration (`infrastructure/nginx/nginx.conf`) routing API ingress paths and serving frontend assets with Gzip.
  - Root `docker-compose.yml` defining PostgreSQL 16, Redis 7, 8 Spring Boot services, and Nginx gateway.
- **Sprint 0 Runtime Verification:**
  - Configured `@CrossOrigin(origins = "*")` across all 8 microservice health controllers to support decoupled browser API access.
  - Added live health polling, dynamic latency measurement, and diagnostics inspection in `frontend/src/App.tsx`.
  - Created `infrastructure/scripts/run-services.js` with `-Xms64m -Xmx128m -XX:TieredStopAtLevel=1` memory optimizations for local Windows and OCI ARM64 hosting.
  - Verified 100% live HTTP REST connectivity across all 8 Spring Boot services (ports 8081-8088) with response times under 25ms.
  - Verified clean stop, clean restart, and reproducible build/test pipelines (`10/10` Maven modules passing with `0` failures, Vite production build passing with `0` errors).

