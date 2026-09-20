# RR Jaggery Traders — Technical Decisions & Architecture Decision Records (ADRs)

## ADR-001: Logical Service Modularity with Low-Cost Single-Host Packaging
- **Status:** Approved
- **Context:** The product requires 8 distinct business domains (Auth, Commerce, Customer/Ledger, Inventory, Procurement, Production, Finance, Notification). Deploying 8 individual microservices across independent Kubernetes pods or cloud containers would incur unsustainable infrastructure hosting bills ($200+/month) and high network serialization latency for an early-stage SME.
- **Decision:** Structure the codebase into modular Spring Boot applications/modules sharing uniform architectural conventions. For Sprint 0 and initial deployment, run these services as lightweight Docker containers on a single Linux VPS (4GB-8GB RAM), orchestrated via Docker Compose and reverse-proxied by Nginx.
- **Consequences:** Low cost (~$15-$30/month VPS), rapid local development, zero inter-service network sprawl, while preserving clean architectural boundaries so any service can be spun out independently at scale.

---

## ADR-002: Multi-Schema Isolation in a Single PostgreSQL Instance
- **Status:** Approved
- **Context:** Managing 8 separate PostgreSQL database servers or managed cloud instances is economically prohibitive for initial launch. However, putting all tables into a shared `public` schema leads to high coupling and accidental cross-domain joins.
- **Decision:** Utilize a single PostgreSQL 16 server partitioned into distinct logical schemas (`auth_schema`, `commerce_schema`, `customer_schema`, `inventory_schema`, `procurement_schema`, `production_schema`, `finance_schema`, `notification_schema`). No cross-schema foreign keys or SQL joins are allowed.
- **Consequences:** Clean domain boundaries, simple single-instance backups, and effortless future migration to standalone databases by dumping/restoring individual schemas.

---

## ADR-003: Strict Use of BigDecimal for Monetary and Fractional Quantities
- **Status:** Approved
- **Context:** Floating-point representations (`float`, `double`) cause rounding inaccuracies that compound across multi-line tax calculations, jaggery yield percentages, and customer ledger balances.
- **Decision:** Mandate `java.math.BigDecimal` in Java and `NUMERIC(15,2)` (for currency) or `NUMERIC(12,4)` (for yields, rates, weights) in PostgreSQL. Floating point primitives are strictly forbidden in business logic.
- **Consequences:** Exact penny-accurate ledgers, compliance with GST regulations, and zero reconciliation drift.

---

## ADR-004: Append-Only Auditable Stock Movements
- **Status:** Approved
- **Context:** Jaggery manufacturing involves natural variations: cane moisture, boiling evaporation, bagasse scrap, bag packaging defects, and physical inventory shrinkage. Directly overwriting `stock = stock - x` destroys traceability.
- **Decision:** The inventory system forbids direct stock mutations. All stock changes are recorded as append-only `StockMovement` rows linked to a business document (PO receipt, sales invoice, production batch, or physical stock reconciliation). Current stock levels are derived or maintained synchronously in a read-optimized table updated atomically.
- **Consequences:** Unmatched audit compliance, dispute resolution with wholesale buyers/suppliers, and pinpoint wastage analysis.

---

## ADR-005: First-Class Offline Wholesale Customer Domain
- **Status:** Approved
- **Context:** Many traditional Indian jaggery traders operate via telephone or in-person visits without web accounts or email addresses, yet require formal GST invoices, credit limits, 30-day payment cycles, and partial payment tracking.
- **Decision:** Model `Customer` with `customer_type = OFFLINE_WHOLESALE` with `user_id` as nullable. Admin and sales staff have full capabilities to generate orders, issue invoices, disburse shipments, and post ledger credits/debits on behalf of offline clients.
- **Consequences:** Eliminates friction for non-digital clients while consolidating all sales and receivables inside the centralized financial ledger.

---

## ADR-006: Unified React Frontend with Role-Gated Layouts
- **Status:** Approved
- **Context:** The system serves both public consumers purchasing jaggery blocks/powder and internal operators running mill batches, warehouse movements, and employee payroll.
- **Decision:** Build a cohesive Single Page Application (SPA) using React, Vite, TypeScript, and Tailwind CSS. Use role-based routing to provide two distinct experiences within the same design system:
  1. **Public Storefront:** E-commerce catalog, shopping cart, customer checkout.
  2. **Admin ERP Portal:** Navigation sidebar, dense data tables, batch trackers, ledger summaries, and KPI metric widgets.
- **Consequences:** Single frontend build pipeline, shared UI component library, unified state management, and optimized asset delivery via Nginx.
