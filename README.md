# RR Jaggery Traders

> **Unified Business Platform:** E-Commerce, Cane-to-Jaggery Manufacturing, Inventory, Multi-Tier Wholesale & Financial Ledger Management.

---

## 1. Project Overview

**RR Jaggery Traders** is an end-to-end enterprise solution engineered to replace fragmented spreadsheets and disconnected point-of-sale systems with a unified digital backbone. The platform powers:
- **Retail & Wholesale E-Commerce:** Modern storefront with tiered wholesale pricing and minimum order quantities.
- **Offline Wholesale Operations:** Unregistered B2B buyer management, manual invoice generation, credit cycles, and partial payment reconciliation without customer login requirements.
- **Batch-Based Manufacturing:** Cane procurement, chemical-free processing stages (crushing, boiling, clarifying, setting), material consumption tracking, yield calculation, and batch costing.
- **Auditable Stock Movements:** Immutable stock movements for both raw materials (cane, lime, fuel) and finished goods (blocks, powder, cubes).
- **Financial Accounting:** Customer ledgers, supplier ledgers, operating expenses, and employee piece-rate/monthly payroll.

---

## 2. Technology Stack

| Tier | Technologies |
| :--- | :--- |
| **Frontend** | React 18, TypeScript, Vite, Tailwind CSS, Lucide Icons |
| **Backend** | Java 21 LTS, Spring Boot 3.3+, Spring Security, Spring Data JPA |
| **Persistence** | PostgreSQL 16 (Logical schema isolation per domain service) |
| **Caching** | Redis 7 (Token revocation, rate limits, session store) |
| **Orchestration** | Docker, Docker Compose, Nginx Reverse Proxy |
| **CI / CD** | GitHub Actions |

---

## 3. Architecture & Service Topology

The platform consists of 8 logical microservices designed to run cooperatively in a low-cost single-host environment while maintaining strict domain autonomy:

1. **`auth-service`** (`:8081`): Identity, JWT issuance, and Role-Based Access Control.
2. **`commerce-service`** (`:8082`): Product catalog, shopping cart, online order lifecycle, and invoices.
3. **`customer-ledger-service`** (`:8083`): Retail, B2B wholesale, and offline wholesale management with auditable ledgers.
4. **`inventory-service`** (`:8084`): Raw materials, finished goods, and immutable stock movement tracking.
5. **`procurement-service`** (`:8085`): Cane farmer suppliers, purchase orders, goods receipts, and supplier ledgers.
6. **`production-service`** (`:8086`): Recipes/BOMs, batch execution, material consumption, and yield analysis.
7. **`finance-service`** (`:8087`): Direct & operating expenses, employee attendance, and payroll ledgers.
8. **`notification-service`** (`:8088`): In-app alerts, transactional emails, and event dispatch.

Comprehensive architectural specifications are located in the [`docs/`](file:///e:/RR%20Jaggery/docs) directory:
- [Architecture & Topology](file:///e:/RR%20Jaggery/docs/architecture.md)
- [Service Boundaries & Dependencies](file:///e:/RR%20Jaggery/docs/service-boundaries.md)
- [Domain Model & ERD](file:///e:/RR%20Jaggery/docs/domain-model.md)
- [Database Ownership & Segregation](file:///e:/RR%20Jaggery/docs/database-ownership.md)
- [API Boundaries & REST Catalog](file:///e:/RR%20Jaggery/docs/api-boundaries.md)
- [Core Business Data Flows](file:///e:/RR%20Jaggery/docs/data-flows.md)
- [Security Model](file:///e:/RR%20Jaggery/docs/security-model.md)
- [Technical Decisions & ADRs](file:///e:/RR%20Jaggery/docs/technical-decisions.md)
- [Ambiguities & Risks](file:///e:/RR%20Jaggery/docs/ambiguities-and-risks.md)
- [Deployment Architecture](file:///e:/RR%20Jaggery/docs/deployment-architecture.md)
- [Testing Strategy](file:///e:/RR%20Jaggery/docs/testing-strategy.md)

---

## 4. Local Development Quickstart

### Prerequisites
- Java 21 LTS
- Node.js 20+ & npm
- Docker & Docker Compose
- Git

### Quick Setup
```bash
# 1. Clone repository
git clone <repo-url>
cd "RR Jaggery"

# 2. Configure environment
cp .env.example .env

# 3. Start PostgreSQL and Redis infrastructure
docker-compose up -d postgres redis

# 4. Start frontend shell
cd frontend
npm install
npm run dev

# 5. Build and run backend services (e.g., auth-service)
cd ../services/auth-service
./mvnw clean spring-boot:run
```

---

## 5. Agile Sprint Roadmap

- **Sprint 0:** Foundation, Architecture & DevOps Skeleton *(Current)*
- **Sprint 1:** Authentication & Product Catalogue
- **Sprint 2:** Cart, Checkout & Orders
- **Sprint 3:** Customer, Wholesale & Ledger (Offline Wholesaler Focus)
- **Sprint 4:** Inventory & Procurement
- **Sprint 5:** Production Management
- **Sprint 6:** Costing, Expenses & Payroll
- **Sprint 7:** Dashboard, Reports & Notifications
- **Sprint 8:** Hardening, Security & Production Readiness
- **Sprint 9:** Production Deployment & Stabilization
