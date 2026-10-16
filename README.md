# RR Jaggery Traders

> **Unified Enterprise Business Platform:** Direct-to-Consumer E-Commerce, Multi-Tier B2B Wholesale, Cane-to-Jaggery Batch Manufacturing, Dynamic Forms, Inventory Outbox & Financial Accounting.

---

## 1. Project Overview

**RR Jaggery Traders** is an enterprise-grade ERP and e-commerce platform built for high-throughput, low-resource deployment (single 4GB/8GB VPS). It replaces disconnected spreadsheets and legacy systems with an integrated digital ecosystem:

- **Retail Storefront:** Chemical-free, 100% natural jaggery catalog, shopping cart, customer checkout, profile management, and direct contact inquiries.
- **B2B Wholesale Portal:** Unregistered and registered wholesale buyers, custom pricing, bulk credit terms (NET 15 / NET 30), and manual offline order generation.
- **Consolidated ERP Manager Portal:** Unified interface for orders, inventory adjustments, farmer cane procurement, production batches, shift attendance, wage payouts, and financial ledgers.
- **Batch-Based Manufacturing:** Cane procurement, chemical-free processing stages (crushing, boiling, clarifying, setting), material consumption tracking, yield calculation, and batch costing.
- **Transactional Stock Outbox:** Asynchronous, self-healing stock deduction ensuring zero inventory drift between commerce/offline orders and warehouse stock.
- **Financial Ledger & Payroll:** Double-entry customer and supplier ledgers, operating expenses, daily wage worker tracking, and automated pro-rata monthly payroll calculation.

---

## 2. Architecture & Service Topology

In Sprint 9, the architecture was consolidated from 8 independent JVMs down to **5 optimized Spring Boot microservices** behind an **Nginx Gateway Reverse Proxy**. This reduces JVM cold-start overhead and memory footprint to run smoothly within a 4GB VPS:

```
                      ┌──────────────────────────────────────────────┐
                      │            Nginx Gateway (:80)               │
                      │  - Single Entrypoint / Reverse Proxy         │
                      │  - Serves React Frontend SPA                 │
                      │  - Routes /api/v1/* to Domain Services       │
                      └──────────────────────┬───────────────────────┘
                                             │
      ┌──────────────────┬───────────────────┼───────────────────┬──────────────────┐
      ▼                  ▼                   ▼                   ▼                  ▼
┌──────────────┐   ┌──────────────┐   ┌──────────────┐   ┌──────────────┐   ┌──────────────┐
│ auth-service │   │   commerce   │   │  customer-   │   │  inventory-  │   │  production  │
│    (:8081)   │   │    service   │   │   finance    │   │ procurement  │   │   service    │
│              │   │    (:8082)   │   │   service    │   │   service    │   │   (:8086)    │
│ - Identity   │   │ - Products   │   │   (:8083)    │   │   (:8084)    │   │ - Recipes    │
│ - JWT & RBAC │   │ - Cart       │   │ - Wholesale  │   │ - Raw & FG   │   │ - Batches    │
│ - Email OTP  │   │ - Orders     │   │ - Expenses   │   │ - Suppliers  │   │ - Yield      │
│ - Pass Reset │   │ - Outbox     │   │ - Attendance │   │ - Cane POs   │   │ - Costing    │
└──────┬───────┘   └──────┬───────┘   │ - Payroll    │   └──────┬───────┘   └──────┬───────┘
       │                  │           │ - Contact    │          │                  │
       │                  │           └──────┬───────┘          │                  │
       └──────────────────┴──────────────────┼──────────────────┴──────────────────┘
                                             ▼
                      ┌──────────────────────────────────────────────┐
                      │          PostgreSQL 16 (:5432)               │
                      │  - auth_schema         - commerce_schema     │
                      │  - customer_schema     - finance_schema      │
                      │  - inventory_schema    - procurement_schema  │
                      │  - production_schema                         │
                      └──────────────────────────────────────────────┘
```

| Service | Port | Responsibilities | Key Features |
| :--- | :--- | :--- | :--- |
| **`gateway`** | `:80` | Reverse Proxy & Web Server | Nginx routing, SSL termination, static SPA hosting |
| **`auth-service`** | `:8081` | Security & Identity | JWT issuance, RBAC, Async Email OTP, First-Admin bootstrap |
| **`commerce-service`** | `:8082` | E-Commerce Engine | Product catalog, cart, checkout, transactional stock outbox |
| **`customer-finance-service`** | `:8083` | B2B & Accounting | Wholesale orders, customer ledgers, attendance, payroll, expenses |
| **`inventory-procurement-service`** | `:8084` | Supply Chain | Stock ledger, FIFO stock movements, cane procurement & farmer ledger |
| **`production-service`** | `:8086` | Manufacturing | Recipe BOMs, production batch stages, material consumption, yield |
| **`postgres`** | `:5432` | Relational Storage | Multi-schema logical separation, full auditability |

---

## 3. Technology Stack

| Layer | Technologies |
| :--- | :--- |
| **Frontend** | React 18, TypeScript, Vite, Vanilla CSS + Tailwind CSS, Lucide Icons |
| **Backend** | Java 21 LTS, Spring Boot 3.3.4, Spring Security, Spring Data JPA |
| **Database** | PostgreSQL 16 (Logical schema isolation per domain service) |
| **Optimization** | JVM TieredStopAtLevel=1, Spring lazy-initialization, Docker healthchecks |
| **Orchestration** | Docker Compose, Nginx Reverse Proxy |
| **CI / CD & VPS** | GitHub Actions, Systemd / Docker daemon, OVHcloud / VPS ready |

---

## 4. Documentation Sitemap

All project documentation is structured inside [`docs/`](docs/):

```
docs/
├── architecture/
│   ├── ARCHITECTURE_DECISIONS.md        # Core architectural decisions
│   ├── DECISIONS.md                     # Technical tradeoffs and rationales
│   └── DEPLOYMENT.md                    # Single-host and production deployment guide
├── developer-guides/
│   ├── CLAUDE_ANTIGRAVITY_DEVELOPMENT_GUIDE.md  # Engineering workflows & protocols
│   └── MASTER_PROMPT.md                 # System prompts and domain constraints
├── reports/
│   ├── PRE_PRODUCTION_READINESS_REPORT.md
│   ├── REPOSITORY_CLEANUP_REPORT.md
│   ├── RESPONSIVE_UI_UX_AUDIT_AND_VERIFICATION_REPORT.md
│   ├── SPRINT_0_TO_8_SERVICE_CONSOLIDATION_FEASIBILITY_REPORT.md
│   ├── SPRINT_9_SERVICE_CONSOLIDATION_IMPLEMENTATION_AND_VERIFICATION_REPORT.md
│   └── SPRINT_10_OVHCLOUD_PRODUCTION_DEPLOYMENT_AND_VERIFICATION_REPORT.md
├── requirements/
│   ├── RR_Jaggery_Traders_Product_Requirements_and_Agile_Sprint_Plan.docx
│   └── Update_RR_Jaggery_Traders_Product_Requirements_Access_Model_v1.1.docx
└── status/
    ├── IMPLEMENTATION_STATUS.md         # Sprint completion milestones
    └── TODO-BACKLOG.md                  # Backlog and enhancement tracking
```

---

## 5. Quickstart & Local Deployment

### Prerequisites
- [Docker](https://www.docker.com/) & Docker Compose (v2+)
- [Node.js](https://nodejs.org/) 20+ & npm (for local frontend dev)
- [Java 21 LTS](https://adoptium.net/) (for local Maven service runs)

### Run Entire Stack with Docker (Recommended)
```bash
# 1. Clone repository
git clone https://github.com/rrjaggery-1458/RR_Jaggery.git
cd RR_Jaggery

# 2. Configure environment
cp .env.example .env
# Edit .env with your SMTP and bootstrap credentials

# 3. Start all services, database & gateway
docker compose up -d --build

# 4. Open in browser
# Storefront & ERP: http://localhost
# Backend APIs:     http://localhost/api/v1/...
```

### Run Frontend in Development Mode
```bash
cd frontend
npm install
npm run dev
# Accessible at http://localhost:5173
```

---

## 6. Project Status & Sprint Roadmap

| Sprint | Milestone | Status | Key Deliverables |
| :---: | :--- | :---: | :--- |
| **Sprint 0** | Architecture & Foundation | ✅ Complete | Multi-schema DB, Docker compose, security skeleton |
| **Sprint 1** | Auth & Product Catalogue | ✅ Complete | JWT auth, RBAC, public catalog, image uploads |
| **Sprint 2** | Cart, Checkout & Orders | ✅ Complete | Dynamic cart, order placement, order history |
| **Sprint 3** | Customer & Wholesale Ledger | ✅ Complete | Unregistered wholesale B2B, credit terms, customer ledger |
| **Sprint 4** | Inventory & Procurement | ✅ Complete | Cane farmer suppliers, POs, weighment receipts, stock movements |
| **Sprint 5** | Production Management | ✅ Complete | Recipes/BOMs, batch stages, material consumption, yield |
| **Sprint 6** | Costing, Expenses & Payroll | ✅ Complete | Daily/monthly attendance, piece-rate payroll, operating expenses |
| **Sprint 7** | Dashboards & Reports | ✅ Complete | Financial balances, summary analytics, consolidated reports |
| **Sprint 8** | Dynamic Form Engine | ✅ Complete | Configurable dynamic forms with schema revisions & submissions |
| **Sprint 9** | Architecture Consolidation | ✅ Complete | 8→5 JVM service consolidation, cold start & memory tuning |
| **Sprint 10** | Production Deployment & Audit | ✅ Complete | CI/CD pipeline, backup/restore scripts, responsive UI/UX audit |

---

## 7. License

Proprietary — All rights reserved by **RR Jaggery Traders**.
