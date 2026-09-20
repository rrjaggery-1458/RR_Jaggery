# RR Jaggery Traders — API Boundaries & REST Conventions

## 1. REST Architecture Standards

All microservices adhere to uniform RESTful conventions, versioning schemes, and response contracts.

### 1.1 Uniform URL & Versioning Conventions
- Base Route: `/api/v1/{service-subdomain}`
- Resources are named in plural nouns (e.g., `/products`, `/orders`, `/ledger-entries`).
- Actions that do not map directly to CRUD operations use semantic sub-resources (e.g., `/api/v1/production/batches/{id}/start-stage`).

### 1.2 Standardized JSON Envelope Format
All endpoints return consistent JSON response envelopes:

```json
{
  "success": true,
  "timestamp": "2026-09-20T10:15:00.000Z",
  "data": { ... },
  "error": null
}
```

In the event of an error:

```json
{
  "success": false,
  "timestamp": "2026-09-20T10:15:00.000Z",
  "data": null,
  "error": {
    "code": "INSUFFICIENT_STOCK",
    "message": "Requested quantity exceeds available stock of Organic Block Jaggery",
    "details": [
      {
        "field": "quantity",
        "rejectedValue": 500,
        "message": "Available stock is only 120 KG"
      }
    ]
  }
}
```

### 1.3 Pagination & Sorting
Standard query parameters across collection endpoints:
- `page`: 0-indexed integer (default `0`)
- `size`: page size integer (default `20`, max `100`)
- `sort`: field and direction (e.g., `sort=createdAt,desc`)

---

## 2. Service API Boundary Catalog

### 2.1 Auth Service (`/api/v1/auth`)
| Method | Path | Description | Access |
| :--- | :--- | :--- | :--- |
| `POST` | `/login` | Authenticate with email/phone & password, issue JWT | Public |
| `POST` | `/register` | Self-register as retail or wholesale customer | Public |
| `POST` | `/refresh` | Exchange refresh token for new access token | Public |
| `POST` | `/logout` | Invalidate active refresh token in Redis | Authenticated |
| `GET` | `/me` | Get active user profile and roles | Authenticated |
| `GET` | `/health` | Service health status | Public |

---

### 2.2 Commerce Service (`/api/v1/commerce`)
| Method | Path | Description | Access |
| :--- | :--- | :--- | :--- |
| `GET` | `/products` | Filterable, paginated product catalog | Public |
| `GET` | `/products/{id}` | Detailed product information | Public |
| `POST` | `/products` | Create new product catalog item | `ADMIN` |
| `PUT` | `/products/{id}` | Update product pricing / details | `ADMIN` |
| `GET` | `/cart` | Retrieve current user shopping cart | `CUSTOMER` |
| `POST` | `/cart/items` | Add/update item in cart | `CUSTOMER` |
| `DELETE`| `/cart/items/{id}`| Remove item from cart | `CUSTOMER` |
| `POST` | `/orders` | Submit cart checkout to create online order | `CUSTOMER` |
| `GET` | `/orders/my` | View authenticated customer order history | `CUSTOMER` |
| `GET` | `/orders` | View all customer orders with filters | `ADMIN` |
| `PATCH`| `/orders/{id}/status` | Update fulfillment state (`PROCESSING`, etc.) | `ADMIN` |
| `GET` | `/invoices/{id}/pdf` | Download tax invoice PDF | `CUSTOMER` / `ADMIN` |
| `GET` | `/health` | Service health status | Public |

---

### 2.3 Customer & Ledger Service (`/api/v1/customers`)
| Method | Path | Description | Access |
| :--- | :--- | :--- | :--- |
| `GET` | `/` | Search/filter customer master list | `ADMIN` |
| `POST` | `/` | Create Retail, Registered B2B, or Offline Wholesaler | `ADMIN` |
| `GET` | `/{id}` | Fetch full customer profile & credit stats | `ADMIN` / Self |
| `PUT` | `/{id}/credit-terms`| Configure credit limit & credit days | `ADMIN` |
| `POST` | `/offline-orders` | Staff creates offline wholesale sale & invoice | `ADMIN` |
| `GET` | `/{id}/ledger` | View chronological ledger entries (Debit/Credit) | `ADMIN` / Self |
| `POST` | `/{id}/payments` | Record customer payment (Cash, UPI, Cheque) | `ADMIN` |
| `GET` | `/outstanding` | Report of all customers with overdue/balances | `ADMIN` |
| `GET` | `/health` | Service health status | Public |

---

### 2.4 Inventory Service (`/api/v1/inventory`)
| Method | Path | Description | Access |
| :--- | :--- | :--- | :--- |
| `GET` | `/items` | List raw materials, packaging, finished goods | `ADMIN`, `PROD_MGR` |
| `POST` | `/items` | Register new Item Master | `ADMIN` |
| `GET` | `/stock-levels` | Real-time on-hand, reserved, available stock | `ADMIN`, `PROD_MGR` |
| `POST` | `/movements` | Append auditable stock movement | Internal / `ADMIN` |
| `GET` | `/movements` | Audit history of stock movements | `ADMIN` |
| `POST` | `/adjustments` | Physical stock take / reconciliation | `ADMIN` |
| `GET` | `/alerts` | Active low-stock alerts | `ADMIN`, `PROD_MGR` |
| `GET` | `/health` | Service health status | Public |

---

### 2.5 Procurement Service (`/api/v1/procurement`)
| Method | Path | Description | Access |
| :--- | :--- | :--- | :--- |
| `GET` | `/suppliers` | Supplier master directory | `ADMIN` |
| `POST` | `/suppliers` | Register cane/fuel/packaging supplier | `ADMIN` |
| `POST` | `/purchase-orders` | Create Purchase Order (PO) | `ADMIN` |
| `POST` | `/goods-receipts`| Record Goods Receipt Note (GRN) & quality | `ADMIN`, `INVENTORY` |
| `GET` | `/suppliers/{id}/ledger` | Supplier debit/credit ledger & balance | `ADMIN` |
| `POST` | `/suppliers/{id}/payments` | Record payment made to supplier | `ADMIN`, `FINANCE` |
| `GET` | `/health` | Service health status | Public |

---

### 2.6 Production Service (`/api/v1/production`)
| Method | Path | Description | Access |
| :--- | :--- | :--- | :--- |
| `GET` | `/recipes` | View production recipes/BOM | `PROD_MGR`, `ADMIN` |
| `POST` | `/recipes` | Configure recipe with raw material ratios | `ADMIN` |
| `POST` | `/batches` | Plan new production batch | `PROD_MGR`, `ADMIN` |
| `GET` | `/batches/{id}/check-materials` | Verify raw material stock availability | `PROD_MGR` |
| `POST` | `/batches/{id}/consume-materials` | Record actual material consumption | `PROD_MGR` |
| `PATCH`| `/batches/{id}/stages` | Advance stage (Crushing, Boiling, Setting) | `PROD_MGR` |
| `POST` | `/batches/{id}/complete`| Record final yield output, wastage & complete | `PROD_MGR` |
| `GET` | `/batches/{id}/cost`| Fetch batch material, labor, fuel cost & Cost/KG| `ADMIN`, `FINANCE` |
| `GET` | `/health` | Service health status | Public |

---

### 2.7 Finance Service (`/api/v1/finance`)
| Method | Path | Description | Access |
| :--- | :--- | :--- | :--- |
| `GET` | `/expenses` | List business expenses with category filters | `ADMIN`, `FINANCE` |
| `POST` | `/expenses` | Record operating/production expense | `ADMIN`, `FINANCE` |
| `GET` | `/employees` | Master list of mill/store staff | `ADMIN`, `FINANCE` |
| `POST` | `/employees/attendance` | Record daily or batch attendance | `ADMIN`, `PROD_MGR` |
| `POST` | `/employees/payroll/calculate`| Compute salaries, advances & deductions | `ADMIN`, `FINANCE` |
| `POST` | `/employees/{id}/disburse` | Record salary payment & update ledger | `ADMIN`, `FINANCE` |
| `GET` | `/health` | Service health status | Public |

---

### 2.8 Notification Service (`/api/v1/notifications`)
| Method | Path | Description | Access |
| :--- | :--- | :--- | :--- |
| `POST` | `/dispatch` | Trigger transactional notification dispatch | Internal Services |
| `GET` | `/user` | Fetch active user in-app notifications | Authenticated |
| `PATCH`| `/{id}/read` | Mark notification as read | Authenticated |
| `GET` | `/health` | Service health status | Public |
