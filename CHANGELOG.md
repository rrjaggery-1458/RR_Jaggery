# Changelog

All notable changes to the **RR Jaggery Traders** platform will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [Unreleased] - Sprint 0: Architecture & Foundation

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
  - Automated database backup script (`infrastructure/scripts/backup.sh`) with retention cleanup.
  - GitHub Actions CI pipeline (`.github/workflows/ci.yml`) for automated frontend and backend test verification.

