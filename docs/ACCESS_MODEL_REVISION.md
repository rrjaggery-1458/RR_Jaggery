# RR Jaggery Traders — Business Access Model Revision
## Formal Amendment to Product Requirements (Pre-Sprint 6)

> **Status:** APPROVED & IMPLEMENTED  
> **Effective:** Pre-Sprint 6 (September 2026)  
> **Supersedes:** Section 3 (Users and Roles) of the original Product Requirements document  
> **All sprints from Sprint 6 onward must use this access model.**

---

## 1. Authoritative Portal Login Roles

The application enforces **exactly three** portal login roles. No other login roles are valid.

| Role | Authority | Portal Access | Who |
| :--- | :--- | :--- | :--- |
| `ADMIN` | `ROLE_ADMIN` | Full platform | Business owner / general manager |
| `MANAGER` | `ROLE_MANAGER` | All operational modules | Operations supervisor, mill supervisor |
| `CUSTOMER` | `ROLE_CUSTOMER` | Retail storefront only | Retail customers (self-registered) |

### Roles Removed as Portal Login Roles

The following roles existed in earlier sprints as portal login roles. They are now **removed as login roles**. Any remaining database records for these roles were migrated per the rules below.

| Legacy Role | Migration |
| :--- | :--- |
| `PRODUCTION_MANAGER` | Accounts migrated to `MANAGER` |
| `EMPLOYEE` | Portal accounts disabled; no replacement login |
| `INVENTORY_STAFF` | Absorbed into `MANAGER` |
| `FINANCE` | Absorbed into `ADMIN`/`MANAGER` (Sprint 6+) |
| `REGISTERED_WHOLESALE` (as a login role) | Portal logins disabled; wholesale records preserved |

---

## 2. Role Capabilities

### 2.1 ADMIN

ADMIN has **all MANAGER capabilities** plus:

- User & Manager lifecycle management (create, enable, disable Manager accounts)
- Master configuration (product catalogue, pricing rules, recipe/BOM master data)
- Executive dashboard (consolidated statistics: sales, stock, production, receivables, payables)
- System-level configuration
- All operational modules listed under MANAGER

### 2.2 MANAGER

MANAGER has full operational access:

- Procurement: create suppliers, purchase orders, record goods receipts, manage supplier ledger
- Inventory: view and update raw material and finished goods stock, reconciliation, adjustments
- Production: create and manage production batches, record material consumption, output, wastage
- Customer/Wholesale management: create and manage retail and wholesale customer records, ledger entries, offline orders, payments
- Product catalogue: operational view and order processing
- Offline orders: enter wholesale/B2B orders on behalf of customers

MANAGER does **not** have access to:

- User Management (creating/disabling accounts)
- Master Configuration panel
- Executive Statistics dashboard

### 2.3 CUSTOMER

CUSTOMER (Retail only) has access to:

- Public storefront (browse products, search, filter)
- Self-service cart and checkout
- Self-placed online orders and order history
- Self-profile management
- Invoice download for own orders

CUSTOMER does **not** have access to any operational, administrative, or internal module.

---

## 3. Non-Login Business Record Types

The following entity types are **business records only** and carry **no portal login account**.

### 3.1 Wholesale Customers

- Stored in `customer_schema.customers` with `customer_type = 'WHOLESALE'`
- All business attributes preserved: GSTIN, credit limit, credit days, payment terms, MOQ, outstanding balance
- Historical orders, ledger entries, and invoices preserved
- All future wholesale orders are entered by `MANAGER` or `ADMIN`
- Wholesale procurement/stock entries entered by `MANAGER` or `ADMIN`
- Portal login: **NONE** (accounts disabled or never created)

### 3.2 Employees

- Employee records are internal business data (attendance, salary, advances)
- **No portal login account**
- All employee data (attendance, salary, payroll) is entered by `MANAGER` or `ADMIN`
- Sprint 6 Finance/Payroll module uses `ADMIN`/`MANAGER` access to manage employee data

---

## 4. Sprint 6 Access Model Constraints

Sprint 6 (Costing, Expenses & Payroll) must comply with this access model:

| Feature | Who Enters Data | Portal Role Required |
| :--- | :--- | :--- |
| Expense categories & entries | Manager / Admin | `MANAGER` or `ADMIN` |
| Production cost allocation & batch cost/KG | Manager / Admin | `MANAGER` or `ADMIN` |
| Employee master data | Admin | `ADMIN` |
| Employee attendance | Manager / Admin | `MANAGER` or `ADMIN` |
| Salary rules configuration | Admin | `ADMIN` |
| Salary computation & payroll run | Admin / Manager | `ADMIN` or `MANAGER` |
| Advance & deduction recording | Manager / Admin | `MANAGER` or `ADMIN` |
| Salary payment recording | Admin | `ADMIN` |
| Employee ledger view | Manager / Admin | `MANAGER` or `ADMIN` |

Sprint 6 must **not** introduce any new portal login roles.

---

## 5. Seeded Default Accounts

| Email | Role | Password | Purpose |
| :--- | :--- | :--- | :--- |
| `admin@rrjaggery.com` | `ADMIN` | `Admin@123` | Platform owner |
| `manager@rrjaggery.com` | `MANAGER` | `Manager@123` | Default operations supervisor |
| `retail@example.com` | `CUSTOMER` | `Customer@123` | Sample retail customer |

---

## 6. JWT Authority Rules

JWTs issued by `auth-service` must carry **exactly one** authority per user:

- `ADMIN` accounts → JWT contains `ROLE_ADMIN` only
- `MANAGER` accounts → JWT contains `ROLE_MANAGER` only
- `CUSTOMER` accounts → JWT contains `ROLE_CUSTOMER` only

No user may hold multiple roles. No legacy role strings (`ROLE_PRODUCTION_MANAGER`, `ROLE_EMPLOYEE`, `ROLE_REGISTERED_WHOLESALE`) may appear in any JWT.

---

## 7. References

- Implementation: `services/auth-service/src/main/java/com/rrjaggery/auth/config/DataInitializer.java`
- Security Model: `docs/security-model.md`
- Implementation Status: `IMPLEMENTATION_STATUS.md`
- Changelog: `CHANGELOG.md` (version 1.5.0)
- Commit: `9db5076` (initial revision), `9e8efb2` (toggle-status), `d4656af` (security-model.md update)
