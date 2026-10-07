# SPRINT 7 — REPORTING, VISIBILITY & NOTIFICATIONS ARCHITECTURE
## RR JAGGERY TRADERS ERP

### 1. Overview & Core Principles

Sprint 7 delivers the business reporting, operational summaries, financial visibility, notifications, and period filtering capabilities for the RR Jaggery Traders ERP.
In accordance with the Sprint 7 Authoritative Rules:
- **No Hardcoded Financial Rates/KPIs**: All business metrics consume authoritative PostgreSQL-backed data from domain microservices.
- **Strict Accounting Separation**:
  - *Purchase Value*: Raw materials/cane asset receipts from `procurement-service`.
  - *Supplier Payables ("Money to Pay")*: Accounts payable ledger balances from `procurement-service`.
  - *Operating Expenses (OPEX)*: Period factory overheads from `finance-service`.
  - *COGM (Cost of Goods Manufactured)*: Direct production batch costing from `production-service`.
  - *Customer Receivables ("Money to Collect")*: Outstanding balances from wholesale trade clients via `customer-ledger-service`.
- **Honest Period Filtering**: Zero-data periods display "No data for selected period" / 0 values without silent fallback to prior months or stale all-time snapshots.

---

### 2. Report & Metric Specifications

| Domain Report | Metric / Card | Source Service | Endpoint | Entity / Table | Period Definition | Calculation / Treatment | Business Meaning |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **Overview & Sales** | Total Gross Revenue | `commerce-service` / `customer-ledger-service` | `GET /api/v1/commerce/orders`, `GET /api/v1/customers/wholesale-orders` | `orders`, `wholesale_orders` | Selected Month / Year | $\sum \text{invoiced order totals}$ | Combined B2B wholesale and retail revenue |
| **Overview & Customers** | Customer Receivables | `customer-ledger-service` | `GET /api/v1/customers/wholesale` | `wholesale_customers` | Live Physical State | $\sum \text{currentOutstanding}$ | "Money to Collect" from wholesale customers |
| **Overview & Procurement** | Supplier Payables | `procurement-service` | `GET /api/v1/procurement/suppliers` | `suppliers` | Live Physical State | $\sum \text{currentOutstanding}$ | "Money to Pay" to cane farmers & suppliers |
| **Overview & Finance** | Operating Expenses | `finance-service` | `GET /api/v1/finance/expenses?year={y}&month={m}` | `expenses` | Selected Month / Year | $\sum \text{amount}$ (by dynamic master category) | Factory OPEX (fuel, utilities, admin, maintenance) |
| **Overview & Finance** | Factory Payroll Disbursed | `finance-service` | `GET /api/v1/finance/payroll?year={y}&month={m}` | `payroll_records` | Selected Month / Year | $\sum \text{netAmountPaid}$ | Net wages disbursed across wage models |
| **Overview & Inventory** | Finished Goods Stock | `inventory-service` | `GET /api/v1/inventory/items` | `inventory_items` | Live Physical State | $\sum \text{currentQuantity}$ (itemType = 'FINISHED_GOODS') | Available organic jaggery inventory (KG) |
| **Overview & Inventory** | Raw Material Stock | `inventory-service` | `GET /api/v1/inventory/items` | `inventory_items` | Live Physical State | Count & Qty (itemType = 'RAW_MATERIAL') | Raw sugarcane, lime, and processing materials |
| **Overview & Production** | Completed Production Output | `production-service` | `GET /api/v1/production/batches` | `batches` | Selected Month / Year (completionDate) | $\sum \text{actualQuantity}$ (status = 'COMPLETED') | Manufactured organic jaggery (KG) |
| **Overview & Inventory** | Low Stock Alert Count | `inventory-service` | `GET /api/v1/inventory/items` | `inventory_items` | Live Physical State | Count where $\text{currentQuantity} \le \text{minimumStockLevel}$ | Items below configured minimum threshold |
| **Overview & Operations** | Pending Orders Count | `commerce-service` / `customer-ledger` | `GET /orders`, `GET /wholesale-orders` | `orders`, `wholesale_orders` | Selected Month / Year | Count where status = 'PENDING' | Orders requiring dispatch confirmation |
| **Financial P&L Statement** | Cost of Goods Manufactured | `production-service` + `procurement` | `GET /production/batches` | `batches`, `cost_masters` | Selected Month / Year | Output KG $\times$ Direct factory cost allocations | Direct material and production cost |
| **Financial P&L Statement** | Gross Operational Margin | Aggregated | Internal Formula | Calculated | Selected Month / Year | $\text{Gross Revenue} - \text{COGM}$ | Factory gross operating margin |
| **Financial P&L Statement** | Net Operational Income | Aggregated | Internal Formula | Calculated | Selected Month / Year | $\text{Gross Margin} - \text{OPEX}$ | Net operating surplus before tax provision |

---

### 3. In-App Notifications & Alerts System

The `notification-service` is backed by PostgreSQL schema `notification_schema.notifications`:
- **Low Stock Alerts**: Automatically triggers when physical inventory falls below the item's configured `minimumStockLevel`.
- **Supplier Payable Reminders**: Triggers on open supplier balances with due terms.
- **Customer Receivable Alerts**: Triggers on overdue wholesale balances exceeding credit terms.
- **Pending Order Warnings**: Triggers for orders requiring operational fulfillment.
- **REST Endpoints**:
  - `GET /api/v1/notifications` — Lists notifications filtered by authenticated role (`ADMIN` / `MANAGER`).
  - `GET /api/v1/notifications/unread-count` — Real-time badge counter.
  - `POST /api/v1/notifications/sync-operational-alerts` — Deduplicated idempotent operational alert synchronization.
  - `PUT /api/v1/notifications/{id}/read` — Marks single alert as read.
  - `PUT /api/v1/notifications/read-all` — Marks all alerts as read.
  - `DELETE /api/v1/notifications/{id}` — Dismisses alert.

---

### 4. Role-Based Access Control (RBAC)

- **ADMIN**: Complete system visibility across Overview Dashboard, Consolidated Financial P&L, All 8 Domain Reports, Business Masters Configuration, User & Access Management, and In-App Alerts.
- **MANAGER**: Operational Dashboard, Inventory & Stock Movement, Production Batches & Costing, Farmer Purchases & Supplier Bills, Operating Expense Vouchers, Staff Attendance & Payroll Records, Wholesale Accounts, Operational Reports, and In-App Alerts. Restricted from User Management and System Administration.
- **CUSTOMER**: Retail Storefront, Shopping Cart, Checkout, Order History, and Personal Profile. Strictly forbidden from internal dashboards, reports, and domain APIs (enforced with HTTP 403 Forbidden).
