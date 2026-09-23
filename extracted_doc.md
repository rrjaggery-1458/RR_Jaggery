RR JAGGERY TRADERS
E-Commerce + Manufacturing + Inventory + Ledger Management Platform
Product Requirements, Architecture, Agile Sprint Plan & DevOps Roadmap
Version 1.0 | September 2026
1. Project Overview
RR Jaggery Traders will be developed as a unified business platform rather than only a conventional e-commerce store. The platform will allow retail and registered wholesale customers to purchase jaggery online, while the business can also manage offline/unregistered wholesalers, inventory, procurement, production batches, expenses, employee salaries, customer/supplier ledgers and operational dashboards.
The solution will use an Agile delivery model, independently deployable backend services, Docker-based development and deployment, GitHub-based source control and CI/CD, and a low-cost cloud architecture. The first production release should avoid unnecessary infrastructure complexity and can run multiple containers on a single OCI Always Free Ampere A1 ARM64 VM.
2. Project Goals
Provide an online storefront for different types and grades of jaggery.
Support both retail and wholesale pricing and ordering.
Track wholesalers who do not have website accounts through internal business records.
Maintain customer ledgers, credit limits, payments and outstanding balances.
Track raw materials, finished goods and all stock movements.
Manage jaggery production through batch-based production cycles.
Calculate production cost, yield and wastage.
Manage suppliers, procurement and supplier balances.
Track employee attendance, salary calculations, advances and payments.
Record business expenses and link costs to production where appropriate.
Provide owner/admin dashboards and operational reports.
Use Agile, Docker, CI/CD and DevOps practices throughout development.
Keep initial infrastructure and operating costs as low as practical.
3. Users and Roles

--- TABLE START ---
| Role | Primary Responsibilities | Access |
| Customer - Retail | Browse products, cart, online orders, payments, invoices, order tracking | Own account/data |
| Customer - Registered Wholesale | Bulk ordering, negotiated/wholesale pricing, invoices, account history | Own business account/data |
| Offline/Unregistered Wholesale | Business record maintained by staff; no website login required | Admin-managed |
| Owner/Admin | Full business operations, finance, stock, production, reports and user management | Full |
| Production Manager | Raw materials, production batches, stages, yield, wastage and finished stock | Production + relevant inventory |
| Employee | Assigned work, attendance and permitted operational functions | Restricted |
--- TABLE END ---

4. Functional Requirements
4.1 E-Commerce
Product catalogue with categories, grades, sizes, weights, images, SKU and descriptions.
Retail pricing, wholesale pricing and quantity-based pricing.
Product search, filtering and availability display.
Customer registration/login and profile management.
Shopping cart and checkout.
Address management.
Order creation, order status and order history.
Online payment integration can be introduced after the MVP.
Invoice generation and downloadable invoices.
Order tracking from placement through delivery.
Wishlist and repeat ordering can be added in Phase 2.
4.2 Retail and Wholesale Customer Management
Customer classification: Retail, Registered Wholesale and Offline Wholesale.
Business customer profile with business name, contact details, GSTIN, billing/shipping address and payment terms.
Customer-specific pricing where required.
Minimum order quantity for wholesale products.
Credit limit, credit days and outstanding balance.
Partial and full payment recording.
Customer transaction history.
Offline sales/order creation by admin.
Customer ledger with debit/credit transactions.
Overdue and credit-limit alerts.
4.3 Inventory Management
Separate raw-material and finished-goods inventory.
Raw materials such as sugarcane, clarifying materials, fuel and packaging materials.
Finished jaggery products such as blocks, powder, cubes and other grades.
Stock-in, stock-out, production, sales, damage and adjustment transactions.
Warehouse/location support can be added when required.
Minimum stock levels and low-stock alerts.
Stock ledger and movement history.
Batch/lot traceability for produced jaggery.
Inventory reconciliation.
4.4 Procurement and Supplier Management
Supplier master data.
Supplier material/rate information.
Purchase requisitions and purchase orders.
Goods receipt and quantity verification.
Supplier invoices and payment records.
Supplier ledger and outstanding balance.
Purchase history and price trends.
Raw-material shortage can generate a purchase request.
4.5 Production Management
Production batch creation with batch ID, product, target quantity and dates.
Configurable production stages.
Raw-material requirement planning using configurable recipes/BOMs.
Availability check before production starts.
Material consumption tracking.
Production output and finished quantity.
Wastage and loss recording.
Yield calculation.
Batch status: Planned, Materials Ready, In Production, Quality Check, Completed, Cancelled.
Production cost calculation.
Finished stock automatically updated after completed production.
4.6 Expense Management
Expense categories: raw materials, fuel, electricity, transport, packaging, maintenance, labour, rent, repairs and miscellaneous.
Expense entry with date, amount, category, supplier/payee, payment mode and description.
Receipt attachment support.
Production-linked cost allocation where applicable.
Expense reports by day/month/category.
4.7 Employee and Payroll Management
Employee profile and role.
Salary type: monthly, daily, batch-based, per-KG or contract.
Attendance tracking.
Salary calculation.
Advance and deduction tracking.
Salary payment recording.
Employee salary ledger.
Payroll reports.
4.8 Dashboard and Reporting
Today's sales and monthly sales.
Order counts and pending orders.
Finished-stock and raw-material stock levels.
Receivables and payables.
Production quantity versus target.
Expenses.
Low-stock, overdue-payment and production alerts.
Sales, production, expense and stock trends.
Product profitability and production-cost analysis can be added in Phase 2.
4.9 Audit and Security
Role-based access control.
Secure authentication and authorization.
Audit trail for important changes to stock, invoices, ledgers, production and expenses.
Old value/new value and user/time information for sensitive changes.
API validation and centralized exception handling.
Backup and restore strategy.
HTTPS in production.
5. Core Business Flows
5.1 End-to-end supply-to-sale flow
Supplier → Purchase Order → Raw Material Inventory → Production Batch → Finished Goods Inventory → Customer Order/Offline Sale → Invoice → Customer Ledger → Payment
5.2 Production cost flow
Raw Material Cost + Labour Cost + Fuel Cost + Packaging Cost + Other Allocated Costs → Total Batch Cost → Cost per KG
5.3 Offline wholesale flow
Admin creates Business Customer → Creates Offline Order → Stock is reserved/issued → Invoice generated → Credit/Payment recorded → Customer Ledger updated → Outstanding balance recalculated
6. Proposed Technical Architecture
Initial service boundaries:

--- TABLE START ---
| Service | Responsibilities |
| Auth Service | Authentication, roles, access control and token/session functions |
| Commerce Service | Products, categories, cart and online orders |
| Customer & Ledger Service | Retail, registered wholesale and offline wholesale customers; ledgers and credit |
| Inventory Service | Raw materials, finished goods, stock movements and stock alerts |
| Procurement Service | Suppliers, purchase orders, receipts and supplier ledger |
| Production Service | Recipes/BOM, batches, stages, material consumption, output, yield and wastage |
| Finance Service | Expenses, payments and payroll |
| Notification Service | Email/web notifications and later SMS/WhatsApp integration |
--- TABLE END ---

7. Recommended Technology Stack

--- TABLE START ---
| Layer | Technology |
| Frontend | React + TypeScript + Vite + Tailwind CSS |
| Backend | Java 21 + Spring Boot + Spring Security + Spring Data JPA |
| API | REST APIs; OpenAPI/Swagger documentation |
| Database | PostgreSQL |
| Caching | Redis, introduced where useful |
| Messaging | RabbitMQ in a later phase if asynchronous workflows require it |
| Containerization | Docker + Docker Compose for initial deployment |
| Source Control | Git + GitHub |
| CI/CD | GitHub Actions |
| Reverse Proxy | Nginx or equivalent |
| Monitoring | Health checks initially; Prometheus/Grafana/Loki later |
| Cloud | Oracle Cloud Infrastructure (OCI) Always Free Ampere A1 ARM64 VM initially; scale architecture only when required |
--- TABLE END ---

Backup and restore procedures remain mandatory. Financially sensitive PostgreSQL data must be backed up automatically, and restoration should be tested periodically without exposing credentials or production secrets.
The deployment target is intended to minimize infrastructure cost, not to guarantee zero cost under all circumstances. OCI Always Free limits, domain-registration costs, storage requirements, backup requirements and future scale may introduce costs.
Do not introduce Kubernetes, multiple VMs, separate infrastructure for every service, Oracle Database, RabbitMQ/Kafka, Elasticsearch, or heavy monitoring infrastructure unless a demonstrated business, performance, availability or scaling requirement justifies it.
The application should remain Dockerized from development through production. Local Windows development should use Docker Compose, while production should use Docker Compose on the OCI ARM64 VM.
Cloudflare will provide DNS and can provide the public HTTPS/protection layer as appropriate. Nginx remains the reverse proxy for routing frontend and backend traffic on the OCI VM.
ARM64 compatibility is mandatory for production components. Every Docker image, base image and dependency introduced for deployment must support linux/arm64, and CI should verify ARM64-compatible builds before production deployment.
PostgreSQL remains the application database from Day 1. Oracle Cloud does not imply a requirement to use Oracle Database. The database design should remain portable so the application can later move to another VPS or cloud provider.
Production topology: User → Cloudflare → Nginx → React + Spring Boot → PostgreSQL, with Redis only where actually required. These components should run on a single OCI Ampere A1 ARM64 VM using Docker Compose for the initial release.
The initial production deployment will target Oracle Cloud Infrastructure (OCI) Always Free Ampere A1 ARM64 resources, subject to the applicable Always Free service limits and availability. The application should be designed so that the initial deployment can operate without paid infrastructure under normal usage and within those limits.
OCI Deployment Decision
8. Database/Domain Strategy
For the initial low-cost deployment, services can share one PostgreSQL server while maintaining clear ownership of their data. The design should avoid cross-service direct table access. Services should communicate through APIs or events. If the platform grows, individual services can be moved to separate databases.
Core domain entities:
User, Role, Customer, CustomerAddress, CustomerLedgerEntry
Product, Category, PriceRule, ProductStock
Cart, Order, OrderItem, Payment, Invoice
Supplier, PurchaseOrder, PurchaseOrderItem, GoodsReceipt, SupplierLedgerEntry
RawMaterial, StockMovement, Warehouse/Location
Recipe/BOM, ProductionBatch, ProductionStage, MaterialConsumption, ProductionOutput, Wastage
Expense, ExpenseCategory
Employee, Attendance, Salary, SalaryPayment, EmployeeLedgerEntry
Notification, AuditLog
9. DevOps Strategy
Development-to-production flow:
Developer → Feature Branch → Pull Request → Automated Build → Unit Tests → Integration Tests → Code Quality/Security Checks → Docker Image Build → Registry → Deployment → Health Check
Use feature branches and pull requests.
Protect the main branch.
Run tests automatically on pull requests.
Build Docker images using reproducible builds.
Tag images with commit SHA/version.
Use environment variables/secrets for configuration.
Never commit database passwords, API keys or production secrets.
Run database backups and periodically test restoration.
Use health endpoints for services.
Add basic monitoring before production launch.
• Keep the application portable so it can later move from OCI to another VPS/cloud provider without changing the application database from PostgreSQL.
• Keep configuration and secrets externalized through environment variables or a secure secret mechanism; never commit production credentials.
• Use Cloudflare for DNS and Nginx as the reverse proxy; enable HTTPS in production.
• Production deployment should use Docker Compose on the single OCI VM initially.
• CI/CD should include an ARM64-compatible build check before production deployment.
• All production Docker images and dependencies must support linux/arm64.
• Target production platform: OCI Always Free Ampere A1 ARM64 VM.
10. Agile Delivery Model
Sprint completion rule: every sprint must produce a runnable checkpoint, a stakeholder demo, and explicit acceptance evidence. For sprints containing user-facing functionality, frontend, backend and real PostgreSQL persistence must be verified together; frontend-only or backend-only completion is not sufficient.
Recommended sprint length: 2 weeks. Each sprint should end with a demonstrable increment, tested code and updated documentation. A short Sprint 0 is used for architecture and DevOps foundations.

--- TABLE START ---
| Cadence | Activity |
| Daily | 15-minute stand-up: progress, blockers, next task |
| Start of Sprint | Planning, story selection, acceptance criteria and estimates |
| During Sprint | Development, testing, code review and CI |
| End of Sprint | Demo/review and stakeholder feedback |
| End of Sprint | Retrospective: what went well, problems, actions |
| Backlog | Prioritize new features, defects and technical debt |
--- TABLE END ---

11. Detailed Sprint Plan
Sprint 0 (1 week) – Foundation & Architecture
Finalize requirements and user roles.
Create GitHub repositories/monorepo structure.
Define service boundaries and API conventions.
Create architecture and data-flow diagrams.
Set up React/Vite frontend shell.
Create Spring Boot service templates.
Set up PostgreSQL and Docker Compose locally.
Set up GitHub Actions skeleton.
Define coding standards, branch strategy and PR template.
Define initial database/entity ownership.
Definition of Done: Project builds locally; Docker Compose starts required infrastructure; CI executes successfully; architecture and backlog are documented.
Sprint 1 (2 weeks) – Authentication & Product Catalogue
Implement authentication and role-based authorization.
Create admin/product management.
Create product categories, SKU, images/metadata and pricing.
Build public product listing and product details.
Add search/filter basics.
Add Swagger/OpenAPI documentation.
Add unit tests and API validation.
Definition of Done: Admin can manage products; customer can browse catalogue; authentication works; APIs are tested and documented.
Sprint 2 (2 weeks) – Cart, Checkout & Orders
Customer cart.
Address management.
Order creation and order items.
Order status workflow.
Basic payment-recording abstraction; real payment gateway can follow.
Invoice data model.
Customer order history.
Admin order management.
Definition of Done: Retail customer can complete a test order end-to-end and admin can process it.
Sprint 3 (2 weeks) – Customer, Wholesale & Ledger
Retail/registered wholesale/offline wholesale customer types.
Business customer profile with GSTIN and payment terms.
Wholesale price rules and minimum order quantity.
Admin-created offline sales/orders.
Customer credit limits and credit days.
Customer ledger entries.
Payment and partial-payment recording.
Outstanding and overdue calculations.
Definition of Done: Admin can create an unregistered wholesaler, sell to them on credit, record payments and see the correct ledger balance.
Sprint 4 (2 weeks) – Inventory & Procurement
Raw material and finished goods inventory.
Stock movement model.
Supplier management.
Purchase orders and goods receipt.
Supplier ledger.
Minimum stock levels.
Low-stock alerts.
Stock reconciliation and adjustment workflow.
Definition of Done: Purchase/receipt increases raw-material stock; sales decrease finished stock; every stock movement is auditable.
Sprint 5 (2 weeks) – Production Management
Production recipe/BOM configuration.
Production batch creation.
Production stages and status workflow.
Material availability check.
Material consumption.
Production output.
Wastage recording.
Yield calculation.
Automatic finished-stock update.
Batch history and traceability.
Definition of Done: A complete production batch can consume raw materials, record output/wastage and update finished stock.
Sprint 6 (2 weeks) – Costing, Expenses & Payroll
Expense categories and expense entry.
Production cost allocation.
Batch total cost and cost/KG.
Employee master.
Attendance.
Salary rules.
Advances/deductions.
Salary payment and employee ledger.
Definition of Done: Owner can see production cost/KG, expenses and employee salary balances.
Sprint 7 (2 weeks) – Dashboards, Reports & Notifications
Owner dashboard.
Sales and order reports.
Inventory dashboard.
Production dashboard.
Receivables/payables.
Expense reports.
Employee/payroll reports.
Low-stock and overdue alerts.
Email/web notification basics.
Definition of Done: Owner can understand current sales, stock, production, expenses, receivables and payables from one dashboard.
Sprint 8 (2 weeks) – Hardening, Security & Production Readiness
End-to-end testing.
Integration tests.
Security review.
API rate limiting/basic abuse protection.
Audit logs.
Database backup automation.
Monitoring and health checks.
Production Docker images.
Reverse proxy and HTTPS.
Deployment pipeline.
Performance testing of critical APIs.
Bug fixing and release documentation.
Definition of Done: Release candidate passes functional/security/backup/health checks and can be deployed reproducibly.
Sprint 9 (2 weeks) – Production Deployment & Stabilization
Deploy to OCI Always Free Ampere A1 ARM64 VM.
Configure domain and HTTPS.
Run database migration.
Seed production configuration safely.
Smoke test customer and admin workflows.
Monitor logs and service health.
Fix launch issues.
Document operations and rollback steps.
Definition of Done: Production system is accessible, monitored, backed up and operational with documented recovery procedures.
Runnable Checkpoint: Deploy the production release to the approved OCI Always Free Ampere A1 ARM64 target using Docker Compose, with Cloudflare/DNS as applicable and Nginx as reverse proxy, then verify the live application.
Demo: Access the production HTTPS application → verify frontend → authenticate → execute a representative customer/admin workflow → verify backend health → verify PostgreSQL persistence → verify backup readiness → show deployment and rollback procedure.
Acceptance: Production images/builds are ARM64-compatible; deployment is repeatable; Nginx/HTTPS and environment configuration work; secrets are externalized; health checks pass; representative critical workflows work in production; backups are configured and recovery procedures documented; stabilization issues are resolved or explicitly tracked. The deployment target remains within applicable OCI Always Free limits where possible, without assuming a guaranteed zero-cost outcome.
Sprint 9 – Production Deployment & Stabilization
Runnable Checkpoint: Run the release candidate locally in the production-like Docker configuration and execute the full critical regression, security, health and backup/restore checks.
Demo: Start the release candidate → execute critical customer/admin/wholesale/inventory/production/finance flows → show authorization/validation checks → health endpoints → backup creation → restore into a test database/environment → show CI/security/dependency scan results.
Acceptance: Critical workflows pass; no known blocking defect remains; authorization and validation checks pass; backup restoration is proven; health checks work; security/dependency checks are reviewed; production runbook, rollback procedure and troubleshooting guide are complete.
Sprint 8 – Hardening, Security & Production Readiness
Runnable Checkpoint: Run the integrated application with known test data and verify that the owner/admin dashboard and reports reflect the underlying orders, stock, production, receivables, payables and expenses.
Demo: Open dashboard → show sales/orders → stock → production → receivables/payables → expenses → low-stock and overdue-payment notifications/alerts → open the agreed operational reports.
Acceptance: Dashboard calculations match known test data; alert conditions trigger correctly; report values reconcile with source transactions; critical dashboard APIs perform acceptably; frontend browser verification and backend tests pass.
Sprint 7 – Dashboards, Reports & Notifications
Runnable Checkpoint: Demonstrate production costing plus an employee salary cycle using real persisted data.
Demo: Open a completed production batch → allocate applicable costs → show batch total cost and cost/KG; then create employee/attendance/salary inputs → apply configured salary rules, advances and deductions → record salary payment → show employee ledger/payment balance; also record an expense.
Acceptance: Money uses Decimal/BigDecimal; cost/KG calculations are reproducible; expense and payroll records persist; salary/payment balances are correct; financial calculations have automated tests; frontend values match backend results.
Sprint 6 – Costing, Expenses & Payroll
Runnable Checkpoint: Complete one production batch from recipe/BOM and material availability through consumption, production stages, output/wastage/yield and finished-stock update.
Demo: Select recipe/BOM → create batch → verify material availability → move through Planned/Materials Ready/In Production/Quality Check/Completed as applicable → record material consumption → record output and wastage → complete batch → show finished stock and batch traceability.
Acceptance: Material consumption is traceable to the batch; output/wastage/yield are recorded; finished stock is updated correctly; configurable recipe/material rules are used; invalid material availability/status transitions are handled; end-to-end batch tests pass.
Sprint 5 – Production Management
Runnable Checkpoint: Demonstrate procurement-to-stock and sales-to-stock integration with auditable stock movements.
Demo: Admin creates supplier/purchase order → records goods receipt → raw-material stock increases; then process a supported sale/order → finished-goods stock decreases; show stock movement history, supplier ledger and low-stock alert.
Acceptance: Purchase receipt increases the correct raw-material stock; sales reduce the correct finished stock; every change has an auditable stock movement; reconciliation/adjustment requires a reason; integration tests pass; frontend displays the resulting stock correctly.
Sprint 4 – Inventory & Procurement
Runnable Checkpoint: Demonstrate the complete offline-wholesale workflow using a real PostgreSQL-backed frontend/backend integration.
Demo: Admin creates an Offline Wholesale customer → enters GSTIN/contact/payment terms/credit limit → creates an offline order → applies wholesale pricing/MOQ rules → generates invoice → records cash/partial/credit payment → updates customer ledger → displays outstanding balance.
Acceptance: Offline wholesale customer requires no login; order/invoice/payment/ledger data persist correctly; debit/credit balance and outstanding amount are correct; partial payment is reflected correctly; ledger operations are auditable; exact end-to-end browser flow and backend tests pass.
Sprint 3 – Customer, Wholesale & Ledger
Runnable Checkpoint: Complete a retail order end-to-end in the browser against the Spring Boot APIs and PostgreSQL.
Demo: Customer login → browse product → add to cart → manage address → checkout/test payment recording → create order → view order history; then Admin opens the order and processes it through the supported status workflow.
Acceptance: Order, order items, invoice data and payment-recording data persist correctly; transaction-sensitive operations are safe; customer and admin views agree; backend integration tests and critical browser flow pass; no mock persistence is used.
Sprint 2 – Cart, Checkout & Orders
Runnable Checkpoint: Run frontend + backend + PostgreSQL locally and verify authentication, role restrictions, product/category CRUD and public catalogue through the browser using real persisted data.
Demo: Admin logs in, creates/updates a category and product, then a customer logs in or accesses the public catalogue and views product details/search/filter results. Show Swagger/OpenAPI for the relevant APIs.
Acceptance: Authentication works; unauthorized role access is rejected; product changes persist in PostgreSQL and appear in the frontend; validation and API tests pass; critical browser flow passes; Docker build succeeds.
Sprint 1 – Authentication & Product Catalogue
Runnable Checkpoint: Start the complete local Docker Compose development environment and verify the React/Vite frontend shell, Spring Boot service health endpoints and PostgreSQL connectivity. CI must execute successfully. The environment must be repeatable from a clean checkout using documented commands.
Demo: Open the frontend in a browser, show the application shell, verify backend health endpoints, verify database connectivity, and show a successful CI run/build.
Acceptance: Docker Compose starts without blocking errors; frontend builds and loads; backend health checks pass; PostgreSQL is reachable; CI passes; architecture, backlog and setup documentation are present. No business feature is required yet.
Sprint 0 – Foundation & Architecture
Every sprint must end with a locally runnable, integrated and demonstrable increment. A sprint is not considered complete merely because source code has been written or individual tests pass. Whenever a sprint contains user-facing functionality, the React frontend, Spring Boot backend and real PostgreSQL persistence must be verified together. The end-of-sprint review must include the runnable checkpoint, demo flow, test evidence and acceptance result below. If the checkpoint fails, the sprint remains incomplete and the failure must be recorded in IMPLEMENTATION_STATUS.md/CHANGELOG and carried into the backlog.
11.1 Runnable Checkpoint / Demo / Acceptance Requirements
12. Initial Product Backlog

--- TABLE START ---
| Epic | Priority | Representative User Story |
| Authentication | P0 | As an admin, I want secure role-based access so that business data is protected. |
| Product Catalogue | P0 | As a customer, I want to browse jaggery products and prices so that I can choose what to buy. |
| Orders | P0 | As a customer, I want to place and track an order. |
| Wholesale | P0 | As an admin, I want wholesale pricing and bulk ordering so that I can manage business customers. |
| Offline Customers | P0 | As an admin, I want to create an unregistered wholesaler so that offline sales are tracked. |
| Customer Ledger | P0 | As an admin, I want to record credit sales and payments so that outstanding balances are accurate. |
| Inventory | P0 | As a stock manager, I want every stock movement recorded so that inventory is traceable. |
| Procurement | P1 | As a purchase manager, I want purchase orders and receipts so that raw materials are controlled. |
| Production | P0 | As a production manager, I want batch-based production tracking so that output, wastage and yield are known. |
| Costing | P1 | As an owner, I want production cost/KG so that I can understand manufacturing cost. |
| Expenses | P1 | As an owner, I want business expenses recorded by category. |
| Payroll | P1 | As an owner, I want employee salary and payment tracking. |
| Dashboard | P1 | As an owner, I want a dashboard showing sales, stock, production and receivables. |
| Notifications | P2 | As an admin, I want alerts for low stock and overdue payments. |
| Analytics | P2 | As an owner, I want profitability and trend reports. |
| Forecasting | P3 | As an owner, I want demand/raw-material forecasts based on historical data. |
--- TABLE END ---

13. Testing Strategy
Unit tests for service/business logic.
Controller/API tests for REST endpoints.
Repository tests for database behavior.
Integration tests using PostgreSQL/Testcontainers where practical.
End-to-end tests for critical flows.
Contract/API tests between services as the architecture matures.
Security tests for authentication and authorization.
Regression tests before every release.
Manual smoke test after deployment.
14. Low-Cost Deployment Plan
The initial production environment should minimize recurring infrastructure costs. A single OCI Always Free Ampere A1 ARM64 VM can host the Dockerized backend services, reverse proxy, Redis and PostgreSQL initially. The frontend can use static hosting if appropriate. GitHub Actions can handle CI/CD. Cloudflare or an equivalent DNS/CDN layer can be used where appropriate, and HTTPS should be enabled.

--- TABLE START ---
| Component | Initial Approach | Scale-up Trigger |
| Frontend | Static hosting/CDN | High traffic or advanced server-side needs |
| Backend | Multiple Docker containers on one VPS | CPU/memory/load or availability requirements |
| PostgreSQL | Single PostgreSQL instance | Database load, HA or isolation requirements |
| Redis | Container on same VPS | Cache/message workload increases |
| RabbitMQ | Defer until needed | Asynchronous workflows become significant |
| Kubernetes | Do not require for MVP | Multiple nodes, scaling and orchestration needs |
| Monitoring | Logs + health checks | More services/traffic; then Prometheus/Grafana/Loki |
| Object storage | Add when file volume grows | Large receipt/image/document storage |
--- TABLE END ---

15. Phase 2 / Future Enhancements
Online payment gateway integration.
WhatsApp/SMS notifications.
Customer-specific negotiated pricing.
Wholesale quotation workflow.
Repeat-order and subscription-style ordering.
Advanced profitability reports.
Demand forecasting.
Raw-material requirement forecasting.
Production efficiency analytics.
Batch genealogy and advanced traceability.
Quality-control module.
Multiple warehouses.
Mobile/PWA improvements.
RabbitMQ event-driven workflows.
Kubernetes deployment when scale justifies it.
Prometheus/Grafana/Loki observability stack.
16. Key Risks and Mitigations

--- TABLE START ---
| Risk | Mitigation |
| Too many microservices too early | Start with 8 logical services and deploy them together on a low-cost environment. |
| Complex financial calculations | Define ledger rules and transaction types before implementation; add automated tests. |
| Incorrect stock values | Use immutable/auditable stock movements rather than directly overwriting stock. |
| Offline customer data inconsistency | Use a single Customer/Ledger service for registered and unregistered business customers. |
| Production cost inaccuracies | Use configurable recipes, actual material consumption and batch-level cost allocation. |
| Deployment cost growth | Use one OCI Always Free Ampere A1 ARM64 VM initially and scale only after measurable need. |
| Security issues | RBAC, HTTPS, secret management, validation, audit logs and dependency/security scanning. |
| Scope creep | Prioritize P0 MVP features and move advanced analytics/AI to later phases. |
--- TABLE END ---

17. MVP Release Definition
The MVP is ready when all of the following work:
Customer can browse products and place an order.
Admin can manage products and orders.
Admin can create registered and offline wholesale customers.
Admin can create offline wholesale sales.
Customer ledger correctly reflects sales, payments and outstanding balances.
Raw-material and finished-goods stock are tracked through stock movements.
Suppliers and purchase receipts are recorded.
Production batches consume raw materials and create finished stock.
Production yield, wastage and cost/KG are calculated.
Expenses and employee salary payments are recorded.
Owner dashboard displays the core operational metrics.
CI pipeline runs automatically.
Application runs in Docker.
Production deployment is repeatable and backed up.
Critical workflows have automated tests.
18. Suggested Team/Ownership

--- TABLE START ---
| Role | Responsibilities |
| Product Owner / Business Owner | Requirements, priorities, acceptance and business validation |
| Backend Developer | Spring Boot services, APIs, database and business logic |
| Frontend Developer | React UI, dashboards and customer/admin workflows |
| DevOps Engineer/Developer | Docker, CI/CD, VPS, monitoring, backups and deployment |
| QA/Developer | Test cases, automation, regression and acceptance testing |
--- TABLE END ---

For a small project, one developer can perform multiple roles. The sprint plan is intentionally structured so that development can proceed incrementally without requiring a large team.
19. Immediate Next Steps
Confirm the business workflow and user roles with RR Jaggery Traders.
Finalize product categories, grades, package sizes and retail/wholesale pricing rules.
Define the exact production stages and raw-material consumption ratios used by the business.
Define customer credit/ledger rules and payment terms.
Define employee salary and attendance rules.
Create the product backlog and Sprint 0 GitHub issues.
Create the repository and Docker Compose development environment.
Design the service boundaries and initial database model.
Start Sprint 0 before implementing business features.
The initial production deployment should run on a single OCI Always Free Ampere A1 ARM64 VM using Docker Compose, with Cloudflare DNS, Nginx reverse proxy and PostgreSQL. The application should remain portable and able to move to another VPS/cloud provider later without requiring a database-platform change.
20. Target End State
The final platform should act as the operational backbone of RR Jaggery Traders: customers can purchase products online, wholesalers can be managed whether or not they have website accounts, inventory can be traced from procurement through production to sale, production cost can be calculated per batch/KG, employees and salaries can be tracked, and the owner can see the business position through a centralized dashboard.
The architecture should remain modular enough to evolve into a larger ERP-like platform, while the first release stays deliberately simple and inexpensive to operate.
21. AI-Assisted Development Tools
The project can be developed substantially faster with agentic coding tools. These tools can plan, edit multiple files, run commands, test applications and, in some cases, operate browsers or work asynchronously. They should be treated as development agents rather than as a replacement for human review.
21.1 Recommended Tools
Google Antigravity
Primary choice for this project. It supports multi-step agents, browser interaction, artifacts, parallel agents/subagents and end-to-end development workflows. It is particularly suitable because the project requires frontend, backend, Docker, testing and browser verification.
Recommended use: Use as the main development orchestrator.
OpenAI Codex
Strong option for end-to-end engineering tasks, feature implementation, refactoring, migrations, tests and parallel agent workflows.
Recommended use: Useful as a second coding/review agent or for selected services.
Cursor Cloud/Background Agents
Cloud agents can work on features, fix bugs, write tests and create pull requests in isolated environments.
Recommended use: Useful for parallel feature development and bug fixing.
Claude Code
Strong terminal-based coding agent for repository-level implementation, debugging, refactoring and test-driven work.
Recommended use: Useful for backend-heavy tasks and independent code review.
GitHub Actions
Not an AI coding agent, but essential for automated build, test, Docker image creation and deployment.
Recommended use: Use as the CI/CD backbone.
21.2 Suggested Tool Strategy
For this project, avoid using several agents to modify the same files simultaneously without coordination. Use Antigravity as the primary orchestrator and assign clearly separated tasks to subagents. Use Git branches, pull requests and automated tests to verify each increment.
Suggested workflow: Antigravity → plan/architecture → parallel service work → tests → browser verification → Git commit/PR → GitHub Actions → Docker build → deployment.
22. Master Prompt for Google Antigravity
Copy the following prompt into Antigravity at the beginning of the project. Give Antigravity the Word document and any additional business information before asking it to implement code.
22.1 Master Prompt
You are the lead software architect, senior full-stack engineer, DevOps engineer, QA engineer and Agile delivery manager for the RR Jaggery Traders platform.PROJECT:Build a production-oriented web application for RR Jaggery Traders. This is not only an e-commerce website. It is a combined:1. Customer e-commerce platform2. Wholesale/offline sales management system3. Customer and supplier ledger system4. Inventory management system5. Raw-material procurement system6. Jaggery production/batch management system7. Expense management system8. Employee attendance/payroll system9. Business dashboard/reporting system10. DevOps-enabled microservice applicationSOURCE OF TRUTH:Use the attached "RR Jaggery Traders Product Requirements and Agile Sprint Plan" document as the primary functional specification. Do not silently remove or simplify requirements. If a requirement is ambiguous, document the ambiguity and ask for clarification before implementing a business rule that could affect money, stock, production quantities, salary or ledger balances.IMPORTANT BUSINESS REQUIREMENT:The system must support three customer categories:- Retail customer with a website account- Registered wholesale customer with a website account- Offline/unregistered wholesale business customer managed internally by adminAn offline wholesale customer must NOT need a login account. Admin staff must be able to create an offline order, invoice it, record credit/payment, and update the customer's ledger and outstanding balance.TECHNOLOGY:Frontend:- React- TypeScript- Vite- Tailwind CSSBackend:- Java 21- Spring Boot- Spring Security- Spring Data JPA- REST APIs- OpenAPI/SwaggerData:- PostgreSQL- Redis only where justifiedInfrastructure:- Docker- Docker Compose initially- GitHub Actions- Nginx/reverse proxy- Low-cost VPS deployment- HTTPS- Automated database backupsARCHITECTURE:Use clear service boundaries:1. Auth Service2. Commerce Service3. Customer & Ledger Service4. Inventory Service5. Procurement Service6. Production Service7. Finance Service8. Notification ServiceDo not create unnecessary microservices. Each service must own its business logic and data access. Services must not directly modify another service's tables. Use APIs/events for inter-service communication.CORE RULES:- Never directly overwrite stock without creating an auditable stock movement.- Ledger transactions must be auditable.- Money calculations must use appropriate decimal types; never floating-point money.- Production batches must have traceable material consumption, output and wastage.- Important changes must create audit records.- Do not hard-code business tax/pricing rules that are expected to change.- Use configuration for pricing, credit days, minimum stock, production recipes and other business rules.- Do not expose secrets in source code.- Do not use mock data as a substitute for real implementation unless explicitly marked as development seed data.- Do not claim a feature is complete unless it has been implemented and tested. At the end of every Sprint 0–9, execute the sprint-specific Runnable Checkpoint / Demo / Acceptance requirements in Section 11.1 and record exact evidence.DEVELOPMENT METHOD:Follow Agile with two-week sprints and a short Sprint 0.Sprint order:0. Foundation and architecture1. Authentication and product catalogue2. Cart, checkout and orders3. Customer, wholesale and ledger4. Inventory and procurement5. Production management6. Costing, expenses and payroll7. Dashboard, reports and notifications8. Hardening, security and production readiness9. Production deployment and stabilizationDo not jump directly to all features at once.FIRST TASK — DO NOT START FULL IMPLEMENTATION:1. Inspect the entire repository.2. Read the attached requirements document completely.3. Create a project architecture document.4. Create a service dependency diagram.5. Create an initial domain model/ERD.6. Create an API boundary document.7. Create a database ownership document.8. Create a sprint backlog with user stories and acceptance criteria.9. Create a technical decision log.10. Identify ambiguities and risks.11. Create a proposed repository structure.12. Create Docker Compose architecture.13. Create the GitHub Actions CI/CD plan.14. Present the plan before implementing major business features.After the plan is approved, implement Sprint 0 first.CODING QUALITY:- Use clean architecture principles where practical.- Keep controllers thin.- Put business rules in services/domain logic.- Use DTOs rather than exposing persistence entities directly.- Validate all incoming requests.- Use consistent API error responses.- Add global exception handling.- Use constructor dependency injection.- Use database migrations rather than relying on destructive schema auto-generation in production.- Add meaningful logging without exposing secrets or sensitive information.- Add unit and integration tests for business-critical logic.- Add API documentation.TESTING:For every completed feature:1. Build the code.2. Run unit tests.3. Run integration tests where applicable.4. Start the application with Docker.5. Test critical APIs.6. Use browser verification for frontend flows.7. Fix failures before marking the feature complete.8. Report exactly what was tested and the result. At sprint end, also report the runnable checkpoint, browser demo flow where applicable, acceptance result, known issues and evidence.UI:Build a clean, modern and responsive UI suitable for a real jaggery trading/manufacturing business.Provide separate navigation/permissions for:- Customer storefront- Admin dashboard- Production management- Inventory/procurement- Finance/payrollImportant dashboard metrics:- Today's/monthly sales- Orders- Finished stock- Raw material stock- Production today vs target- Receivables- Payables- Expenses- Low-stock alerts- Overdue customer paymentsOFFLINE WHOLESALE FLOW:Implement this end-to-end:Admin → Create Business Customer → Type = Offline Wholesale → Add GSTIN/contact/payment terms/credit limit → Create Offline Order → Add products and quantities → Apply wholesale pricing → Generate invoice → Choose cash/partial/credit → Update inventory → Create customer ledger transaction → Calculate outstanding → Display in customer profile/dashboard.PRODUCTION FLOW:Implement:Recipe/BOM → Production Plan → Raw Material Availability → Production Batch → Material Consumption → Production Stages → Output → Wastage → Yield → Cost Calculation → Finished Stock.FINANCIAL INTEGRITY:Treat stock, customer ledgers, supplier ledgers and payroll as financially sensitive.Use transactions where multiple related records must update atomically.Prevent negative stock unless an explicit business rule allows it.Prevent duplicate payment/order/ledger operations through suitable identifiers/idempotency controls.DEVOPS:Create:- Dockerfiles for services- docker-compose.yml for local development- environment configuration examples- GitHub Actions workflows- test/build pipeline- Docker image build pipeline- deployment workflow- health checks- database backup script- rollback documentation- production deployment documentationLOW-COST DEPLOYMENT:The first production deployment should use the minimum practical infrastructure:- one OCI Always Free Ampere A1 ARM64 VM for backend containers/database initially- static frontend hosting where appropriate- Docker Compose- Nginx/reverse proxy- HTTPS- GitHub ActionsDo not introduce Kubernetes, managed databases, message brokers or multiple servers unless there is a demonstrated requirement.AGENT BEHAVIOR:- Work incrementally.- Do not make huge uncontrolled changes.- Before modifying architecture, explain why.- Keep a CHANGELOG.- Keep an IMPLEMENTATION_STATUS.md file updated.- Keep a TODO/BACKLOG.md file updated.- After each major task, summarize changed files, tests run, known issues and next recommended task.- Use separate branches for significant features.- Never delete existing working code just to simplify implementation.- Preserve backward compatibility when changing APIs.- If a test fails, investigate the root cause rather than weakening/removing the test.DEFINITION OF DONE:A feature is Done only when:- Code implemented- Validation implemented- Tests added/passing- API documented- UI completed where applicable- Docker build succeeds- Critical browser flow verified- No known blocking error remains- Documentation/status updatedStart by producing the architecture, backlog, ERD/domain model, service boundaries and Sprint 0 implementation plan. Do not begin the entire application implementation in one uncontrolled operation.
23. Sprint-by-Sprint Antigravity Prompts
Sprint 0 – Foundation
Implement Sprint 0 only for RR Jaggery Traders.First inspect the approved requirements and current repository. Create the repository structure, service templates, React frontend shell, Docker Compose development environment, PostgreSQL setup, shared configuration conventions, GitHub Actions CI skeleton, health endpoints, README, architecture documentation and development standards.Do not implement full business features yet.Run the complete build and startup flow. Fix all setup errors. At the end, report files created, commands run, test results and remaining Sprint 0 tasks.
Sprint 1 – Authentication & Products
Implement Sprint 1 only.Build secure authentication and role-based authorization, then implement product/category management and customer-facing product browsing.Roles must include ADMIN, PRODUCTION_MANAGER, EMPLOYEE and CUSTOMER, with customer type RETAIL/REGISTERED_WHOLESALE where appropriate.Implement DTO validation, error handling, tests, Swagger documentation and responsive React screens.Use real PostgreSQL persistence. Do not use mock persistence.Verify login, role restrictions, product CRUD and public catalogue through browser testing.
Sprint 2 – Cart, Checkout & Orders
Implement Sprint 2.Build cart, address management, checkout, order creation, order items, order status workflow, order history and admin order management.Keep payment integration behind a clean interface so a real payment provider can be added later.Implement invoice data structures and make order creation transaction-safe.Test the complete retail flow from login → product → cart → checkout → order → admin processing.Do not mark complete until the browser flow and backend integration tests pass.
Sprint 3 – Wholesale, Offline Customers & Ledger
Implement Sprint 3, with special attention to offline/unregistered wholesalers.Create customer types Retail, Registered Wholesale and Offline Wholesale.Offline Wholesale must not require a login.Implement business profile, GSTIN, credit limit, credit days, wholesale pricing, minimum order quantity, admin-created offline orders, invoices, payments, partial payments and customer ledger.Make debit/credit balances transaction-safe and auditable.Create UI screens for customer profile, ledger, outstanding balance and payment entry.Test the exact offline-wholesale workflow end-to-end.
Sprint 4 – Inventory & Procurement
Implement Sprint 4.Build raw-material and finished-goods inventory, stock movements, suppliers, purchase orders, goods receipt, supplier ledger, minimum stock and alerts.Do not store stock as an unexplained mutable number. Stock must be derived/maintained from auditable movements.Connect sales and procurement to inventory updates safely.Add integration tests for purchase → receipt → stock increase and sale → stock decrease.Add reconciliation/adjustment workflow with mandatory reason.
Sprint 5 – Production
Implement Sprint 5.Build recipes/BOMs, production batches, material availability checks, production stages, material consumption, output, wastage, yield and finished-stock updates.Production must be batch-based and traceable.Implement configurable material requirements rather than hard-coded assumptions.Support statuses Planned, Materials Ready, In Production, Quality Check, Completed and Cancelled.Test a complete batch from raw material availability through finished stock.
Sprint 6 – Costing, Expenses & Payroll
Implement Sprint 6.Build expense categories/entries, production cost allocation, batch total cost and cost per KG.Build employee master, attendance, salary rules, advances, deductions, salary payments and employee ledger.Use Decimal/BigDecimal for money.Make salary rules configurable.Test production costing, employee salary calculation and payment balances.
Sprint 7 – Dashboard & Notifications
Implement Sprint 7.Build the owner/admin dashboard with sales, orders, stock, production, receivables, payables and expenses.Add low-stock and overdue-payment alerts.Build useful reports for sales, inventory, production, expenses and payroll.Keep dashboard queries efficient and avoid N+1 database queries.Use charts only where they improve decision-making.Verify dashboard calculations against known test data.
Sprint 8 – Hardening
Implement Sprint 8.Perform end-to-end testing, integration testing, authorization testing, validation testing, audit logging, health checks, backup/restore testing, security/dependency scanning and performance checks for critical APIs.Fix defects found during testing.Do not weaken tests to make the pipeline pass.Create production runbook, rollback procedure and troubleshooting guide.
Sprint 9 – Deployment
Implement Sprint 9.Prepare the application for low-cost production deployment.Build production Docker images, configure Nginx/reverse proxy, HTTPS, environment variables/secrets, PostgreSQL backup, health checks and GitHub Actions deployment.Deploy to the target VPS.Run smoke tests for customer registration/login, product browsing, order creation, offline wholesale order, ledger payment, inventory, production batch and dashboard.Document exact deployment and rollback commands.Do not expose credentials in the repository or logs.
24. Antigravity Verification Prompt
Use this prompt after each sprint or major release:
Act as an independent senior QA engineer and release reviewer for RR Jaggery Traders. Do not modify code initially. Inspect the implementation against the requirements document and current sprint acceptance criteria. Run the build, automated tests, Docker startup and critical browser flows. Check authorization boundaries, stock integrity, customer ledger integrity, supplier ledger integrity, production calculations, payroll calculations and error handling. Identify missing functionality, bugs, security risks, data-integrity risks, poor UX and technical debt. Produce a severity-ranked defect report. Only after the report is complete, fix the defects that are within the current sprint scope, rerun the tests, and provide a final release-readiness report with evidence.
25. Important Guidance for AI-Assisted Development
Do not give an AI agent unrestricted permission to deploy directly to production until the application has been reviewed.
Keep production secrets outside the repository.
Use Git commits/branches so every agent change is reversible.
Review financial, inventory, payroll and ledger code manually even when automated tests pass.
Require evidence for completion: tests, logs, screenshots/browser verification and build results.
Use separate agent tasks for architecture, implementation, QA and review rather than one enormous prompt.
Let the main Antigravity agent coordinate the project while specialized subagents work on isolated tasks.
Do not allow two agents to modify the same files simultaneously unless the work is explicitly coordinated.
Treat generated code as code that still requires testing and review.
26. Tool References
Official documentation used when preparing the AI-development-tool recommendations:
Google Antigravity: https://www.antigravity.google/
Antigravity Agent documentation: https://www.antigravity.google/docs/agent/
Antigravity pricing: https://www.antigravity.google/pricing
OpenAI Codex: https://openai.com/codex/
Cursor Cloud Agents documentation: https://cursor.com/docs
