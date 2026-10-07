# RR Jaggery Traders — Project Takeover Audit

**Audit basis:** repository working tree and read-only runtime observations  
**Audit date:** 2026-09-30  
**Disposition:** Takeover is possible only after the blocking integration, financial-integrity, and runtime discrepancies below are resolved and verified.  
**Sprint 8:** **NOT STARTED / NOT AUTHORIZED**

> This is a read-only audit. No source, configuration, database, deployment, or test state was changed. The repository already contained extensive modified and untracked files before this report was created. Source observations below therefore describe the inspected working tree, not a clean checkout or necessarily the currently deployed build. Runtime responses are reported separately and do not prove that the deployed binaries match the working tree.

## 1. Executive summary

RR Jaggery Traders is a small ERP/e-commerce platform implemented as a React/Vite frontend and eight Spring Boot services backed by PostgreSQL schemas. The current codebase contains commerce, customer/offline-ledger, inventory, procurement, production/recipe/costing, finance/payroll, authentication, and notification/reporting capabilities.

The key takeover risks are:

1. **Inventory integrations are role-incompatible in code:** commerce and procurement clients create internal JWTs with the legacy `EMPLOYEE` role, while inventory endpoints require `ADMIN` or `MANAGER`.
2. **Storefront reviews are unavailable at runtime:** GET `/api/v1/commerce/reviews` returned HTTP 403 through both gateway and direct commerce-service URLs. The storefront rendered, but its catalog showed “Cannot reach Commerce Service.” The exact client-side exception/body was not captured; the source appears to permit public reviews, so the mismatch between source and runtime needs investigation.
3. **Financial outputs can be misleading:** the manager P&L currently estimates production cost using a fixed `25.0` per output kilogram instead of the batch-cost results; production costing also reads another service’s schema directly and falls back to zero when some procurement-cost lookups fail.
4. **Configured business masters are not consistently authoritative:** payroll has hard-coded fallback rules, the UI contains fixed sample/default values, and documented costing baselines conflict with the configurable-rate requirement.
5. **Persistence and operational readiness are unproven:** main profiles use Hibernate `ddl-auto: update`; no service migration set was found; live data was not queried or reconciled. No build, tests, container inspection, database inspection, restart, or state-changing workflow was performed.
6. **Project-status documents conflict:** earlier documents variously describe Sprints 6 and 7 as ready, implemented, or not started. Relevant code exists, but full acceptance is not certified by this audit.

## 2. Objective and scope

The objective was to provide a factual takeover baseline across product requirements, sprint status, architecture, service ownership, roles, APIs, frontend/CSS, financial and data integrity, security, deployment, and evidence quality. This report does not certify business acceptance, production suitability, or the state of live data.

Included: requirements and project-document review; code inspection of the eight services, frontend, schema/bootstrap, deployment and CI; read-only HTTP GET probes and browser observation; a 40-item claim-verification matrix.

Excluded: source modifications other than this report; fixes; database reads; POST/PUT/PATCH/DELETE calls; builds, tests, migrations, Docker commands, restarts, authenticated workflows, and penetration testing.

## 3. Authority and source-of-truth order

For the amended portal-access model, [docs/ACCESS_MODEL_REVISION.md](./docs/ACCESS_MODEL_REVISION.md) is the controlling role amendment and supersedes older role descriptions. For product/sprint scope, the updated requirements document named `RR_Jaggery_Traders_Product_Requirements_and_Agile_Sprint_Plan_Updated_OCI_Runnable_Checkpoints.docx` is the intended authority; [extracted_doc.md](./extracted_doc.md) is the checked-in text extraction produced by [extract_docx.ps1](./extract_docx.ps1). The extraction script identifies the updated OCI checkpoint document as its input.

The source code establishes what is present in the inspected working tree, not whether a feature is accepted or deployed. Runtime checks establish only the observed response at the inspected local URLs and time. Older status, stabilization, and verification documents are treated as claims to compare—not as independent proof.

Relevant status/history documents include [IMPLEMENTATION_STATUS.md](./IMPLEMENTATION_STATUS.md), [PROJECT_TAKEOVER_ANALYSIS.md](./PROJECT_TAKEOVER_ANALYSIS.md), [FINAL_RUNTIME_VERIFICATION_REPORT.md](./FINAL_RUNTIME_VERIFICATION_REPORT.md), [PRE_SPRINT_8_FINAL_STABILIZATION_REPORT.md](./PRE_SPRINT_8_FINAL_STABILIZATION_REPORT.md), and [final_stabilization_report.md](./final_stabilization_report.md). They are not fully consistent with one another or with current code.

## 4. Evidence types and confidence

| Label | Meaning |
|---|---|
| **CODE VERIFIED ONLY** | Present in the inspected working-tree source/configuration. Not proof of successful runtime behavior. |
| **DOCUMENTATION VERIFIED** | Stated in a checked-in requirements or project document. Not proof that code or runtime conforms. |
| **RUNTIME VERIFIED** | Directly observed through a read-only GET or rendered browser page during this audit. |
| **BROKEN** | A required behavior is contradicted by inspected source or fails in the limited runtime checks. The evidence type and scope are stated; source-only findings do not imply runtime execution. |
| **NOT VERIFIED** | Not tested, queried, or established by available evidence. |
| **NOT APPLICABLE** | The claim does not apply to the observed system/scope. |

Confidence describes confidence in the finding as worded, not the severity or the certainty of root cause. “Source says permitted, runtime says 403” is a verified discrepancy; its root cause remains unverified.

## 5. Repository condition and change control

Before this report was added, `git status --short` showed numerous modified tracked files and numerous untracked reports, frontend files, service files, and a token-named file. Those changes predate this audit and have not been reverted, edited, or otherwise attributed to this work. The token-named file was not opened.

The report is the only file created by this audit. A final status check is recorded in the delivery summary. No claim is made that the pre-existing worktree is clean or safe to deploy.

## 6. Product and operating model

The requirements describe a jaggery producer/trader with both direct-to-customer commerce and business operations: customer/wholesale records, procurement, inventory, batch production, expenses, payroll, reporting, and operational notifications. The source contains implementations corresponding to those domains.

The existence of a UI screen, endpoint, service, or entity is not evidence that a complete business workflow works end-to-end. This audit did not validate sales-to-ledger posting, purchase-to-stock receipt, batch-to-stock posting, payroll settlement, reports against reconciled ledgers, or notification delivery.

## 7. Sprint 0–7 status

| Sprint | Evidence-based status |
|---|---|
| 0–5 | Prior status documents claim completed/stabilized work, and the present source contains the core application and service foundations. This audit did not rerun acceptance tests and does not certify every earlier story. |
| 6 | Costing and business-master code is present, including production cost masters, costing service, and finance masters. Some status documents describe Sprint 6 as “ready” or future work; therefore code presence is stronger evidence than the conflicting status labels, but acceptance remains unverified. |
| 7 | Reporting and notification source/UI exists. Other documents call Sprint 7 not started or incomplete. End-to-end data correctness, scheduling/delivery, and acceptance remain unverified. |

The project-status contradictions mean a takeover should use source plus reproducible acceptance tests, not a single status document. No sprint was advanced or marked accepted by this audit.

## 8. Sprint 8 boundary

**Sprint 8: NOT STARTED / NOT AUTHORIZED.** No Sprint 8 implementation, hardening change, remediation, test, backup/restore, security scan, or production-readiness change was performed as part of this audit. The existence of ordinary authentication/security/configuration code does not establish that Sprint 8 is underway.

The requirements list Sprint 8 work such as broader integration/authorization/validation testing, audit logging, health checks, backup/restore testing, security/dependency scanning, performance checks, and production runbooks. This audit did not perform those activities. The related requirement text is in [extracted_doc.md](./extracted_doc.md).

## 9. High-level architecture

The frontend is a React/TypeScript application built with Vite and Tailwind utilities. It calls Spring Boot services directly in some development/runtime paths and uses gateway-relative `/api/v1/...` paths in others. Deployment includes Nginx and PostgreSQL in [docker-compose.yml](./docker-compose.yml), with routes in [infrastructure/nginx/nginx.conf](./infrastructure/nginx/nginx.conf).

Eight service modules are present under `services/`. They share one PostgreSQL database deployment but use domain-specific schemas. Service-to-service HTTP clients exist; this is not a message-driven architecture. Redis is configured in deployment, but its operational use and runtime health were not assessed.

## 10. Service inventory

| Service | Default port / schema | Primary domain and observed source surface | Access / dependency notes |
|---|---|---|---|
| Auth | 8081 / `auth_schema` | Login, registration, current-user API, admin users/manager administration, user and role entities | Login/register are public; `/me` requires authentication; admin controller is ADMIN-only. JWT issuance/validation is shared across services. |
| Commerce | 8082 / `commerce_schema` | Products, categories, carts, online orders, reviews, image upload | Public catalogue is intended; admin catalogue/order operations allow ADMIN/MANAGER. Runtime review GET returned 403. Online order flow calls inventory. |
| Customer ledger | 8083 / `customer_schema` | Customer records, offline orders/items, ledger entries, payments | Operational customer/ledger operations have ADMIN/MANAGER guards in inspected controllers. Owns the offline/customer balance data. |
| Inventory | 8084 / `inventory_schema` | Stock items, movement ledger, receipts, sales, adjustments/reconciliation | Inspected controller operations require ADMIN/MANAGER. Called by commerce, procurement, and production. Commerce/procurement service-token roles do not satisfy this guard. |
| Procurement | 8085 / `procurement_schema` | Suppliers, purchase orders/lines, receiving | Controller operations require ADMIN/MANAGER. Receipt flow calls inventory; production costing directly queries this service’s schema (boundary violation). |
| Production | 8086 / `production_schema` | Recipes/BOM, batches/stages, cost masters and batch-cost calculations | Batch/recipe/cost-master controller operations require ADMIN/MANAGER. Calls inventory; creation path catches an inventory-sync exception and may still return the saved batch. |
| Finance | 8087 / `finance_schema` | Employees, attendance, payroll, salary advances/payments, expenses, business masters | Inspected controller surfaces allow ADMIN/MANAGER, including business-master configuration; that conflicts with the amended model’s ADMIN reservation for at least master/rule configuration. |
| Notification | 8088 / `notification_schema` | Notification/alert records and APIs | Operational controller endpoints are guarded ADMIN/MANAGER. Frontend initiates synchronization; delivery and end-to-end alert correctness are unverified. |

Ports and schema names are from main service configuration/bootstrap. Per-service test files exist (20 backend Java test source files were inventoried); their pass status is **NOT VERIFIED** because tests were not run.

## 11. Domain boundaries and integrations

The broad service split is coherent, but ownership is not consistently respected:

- `ProductionCostingService` executes SQL against `procurement_schema.purchase_order_lines` rather than consuming a procurement-owned API. It chooses the highest unit cost rather than demonstrably selecting the most recent received cost; lookup errors can be caught and represented as zero cost. See [ProductionCostingService.java](./services/production-service/src/main/java/com/rrjaggery/production/service/ProductionCostingService.java).
- Commerce and procurement service-token builders claim `EMPLOYEE`; inventory authorization accepts `ADMIN`/`MANAGER`. This makes their receive/sale stock calls an integration blocker in the inspected code. See [InventoryStockClient.java](./services/commerce-service/src/main/java/com/rrjaggery/commerce/service/InventoryStockClient.java), [InventoryStockClient.java](./services/procurement-service/src/main/java/com/rrjaggery/procurement/service/InventoryStockClient.java), and [InventoryController.java](./services/inventory-service/src/main/java/com/rrjaggery/inventory/controller/InventoryController.java).
- Production’s inventory client uses manager/admin claims, so its authorization shape is different; the real target behavior was not exercised.
- Cross-service side effects and transaction boundaries were not verified. A local database transaction cannot by itself establish atomicity across HTTP service calls.

## 12. Data ownership and persistence

Main service profiles use PostgreSQL and Hibernate `ddl-auto: update`; test profiles commonly use H2 with `create-drop`. The root initializer creates all eight schemas and selected tables in [init-schemas.sql](./infrastructure/docker/postgres/init-schemas.sql). No per-service migration set was found during inspection.

The result is a mixed schema-bootstrap/ORM-update strategy rather than a verified, versioned migration path. A fresh database, an upgrade from an older schema, and rollback behavior were not tested. The existing database was not queried; the audit cannot report current counts, balances, role records, seeds actually applied, or referential consistency.

Several domains retain both current balances/quantities and movement/ledger history. Their reconciliation is a takeover prerequisite; no live balance-to-ledger comparison was performed.

## 13. API surface and observed availability

The service controllers expose CRUD and workflow endpoints beneath `/api/v1/...`; principal surfaces are listed in Section 10. Direct service endpoints and gateway routes are present. Only unauthenticated GET probes were used.

Observed HTTP 200: `http://localhost/`, gateway commerce health, each of the eight service health endpoints on ports 8081–8088, and commerce category/product reads through the gateway. Observed HTTP 403: gateway and direct-service GET `/api/v1/commerce/reviews`. Other methods, authenticated routes, payload validation, and error handling are unverified.

## 14. Portal roles and access model

The amended portal model defines **ADMIN, MANAGER, CUSTOMER**. Wholesale buyers and employees are business records and do not receive portal logins under the amendment. This amendment supersedes older role lists in historical documents.

Most inspected operational service controllers use ADMIN/MANAGER guards, and the admin user controller is ADMIN-only. However, finance business-master endpoints allow both ADMIN and MANAGER. Confirm each master/rule’s intended authority against the amendment before relying on that endpoint.

Customer isolation and ownership checks exist in customer-facing paths, but the full set of object-level authorization paths was not exhaustively executed. The browser/API customer boundary is not runtime-certified by this audit.

## 15. Legacy role drift and service identities

`Role.java` still declares `PRODUCTION_MANAGER` and `EMPLOYEE`, while the amended model has three portal roles. The auth initializer contains migration/disable logic for those legacy roles. The live database was not queried, so the current role population and whether the migration has run are **NOT VERIFIED**.

The more urgent code issue is internal service JWT construction: commerce/procurement assert `EMPLOYEE`, but inventory endpoints require ADMIN/MANAGER. An internal service identity should not be conflated with a human portal role. The actual requests were not triggered, so the integration failure is code-verified, not runtime-observed.

## 16. Commerce, customers, and ledger

Online commerce includes a public catalogue, cart/order controllers and inventory deduction calls. Customer-ledger includes customer records, offline orders, payments, and ledger APIs. Source also includes demo initializer data: seeded customer credit limits of 100,000, 50,000, and 75,000 currency units, and an offline item price of 550.00; these are seed inputs, not verified live balances or approved commercial values. See [DataInitializer.java](./services/customer-ledger-service/src/main/java/com/rrjaggery/customerledger/config/DataInitializer.java).

Commerce sample catalogue prices are also seeded in source: retail/wholesale 550/420 for a 10 kg product; 85/62 for 1 kg; 65/48 for 0.5 kg; and 380/290 for 5 kg. They may be inserted only under initializer conditions; database state and actual use are unverified. See [CommerceDataInitializer.java](./services/commerce-service/src/main/java/com/rrjaggery/commerce/config/CommerceDataInitializer.java).

These values must be treated as sample defaults, not evidence of current market pricing. Online-order posting, customer-ledger impacts, and consistency with stock movement were not tested.

## 17. Inventory and stock integrity

Inventory source models stock items and movements, with controller operations for item management, receiving, sale deductions, adjustments, and reconciliation. Procurement/commerce calls use separate service clients; production has another inventory integration.

There is no runtime evidence in this audit that current on-hand stock equals the sum of movements or that receiving/sales workflows complete. The incompatible commerce/procurement role claims are an immediate integration blocker. Legacy status/changelog quantities and payables, where present, are historical claims only and are not current verified balances.

## 18. Production lifecycle

Production source includes recipe/BOM and multi-stage batch lifecycle paths as well as a simpler manager UI operation that submits a completed daily batch. Both are present; they should not be confused as a single verified workflow. `ProductionService` catches inventory synchronization failure after a batch is persisted and can still return the saved batch (source observation around the inventory-sync handling in `ProductionService.java`). This creates a possible divergence between production records and inventory movements.

No batch lifecycle, approval transition, stock consumption/output, or failure-retry path was executed. Production acceptance is therefore **NOT VERIFIED**.

## 19. Production costing

The costing service has cost-master and batch-cost logic, but the manager P&L does not consume its calculated output: the UI estimates production cost as `outputKg * 25.0` ([ManagerPortal.tsx](./frontend/src/ManagerPortal.tsx), around line 4892). This is a source-verified financial-reporting defect, not a runtime-calculated value.

The costing service’s direct procurement-schema query violates service ownership. Its source ordering/fallback behavior can select a high cost rather than the newest receipt and can treat lookup failure as zero, understating cost. No real batch cost was calculated or reconciled in this audit.

`docs/SPRINT6_COSTING_RULES.md` documents fixed rupee baselines for cane, lime, oil, bagasse, labour, fuel, packaging, and other expenses. This conflicts with the requirement to configure business-specific rates and with the presence of cost-master configuration. No claim is made that those document values are currently applied in live calculations.

The UI also displays fallback labels of ₹12.50 per kg output for labour and ₹4.00 per kg output for packaging when a matching master is absent ([ManagerPortal.tsx](./frontend/src/ManagerPortal.tsx), around lines 7716 and 7732). These are hard-coded display fallbacks, not verified configured rates.

## 20. Finance, payroll, and business masters

Payroll service defaults include 26 standard working days, 8 hours/day, a 1.50 overtime multiplier, a 0.50 half-day factor, and a fallback of 100 kg per attended day when an employee’s production quantity is absent ([PayrollService.java](./services/finance-service/src/main/java/com/rrjaggery/finance/service/PayrollService.java), around lines 199–247). `PayrollRule` repeats the first four as entity defaults. Such values may be legitimate configurable starting points but must not be silently treated as approved business policy.

The UI initializes payroll-rule fields to the same 26/8/1.5/0.5 defaults and initializes payment method to `CASH`; finance services/entities also default missing payment methods to `CASH`. Expense/payment categories and supported methods need to be verified against the active business masters.

The business-master controller allows ADMIN and MANAGER. The amended access model reserves ADMIN-only authority for at least some business master/rule configuration; endpoint-by-endpoint authorization needs an explicit mapping.

## 21. Frontend architecture and API routing

`frontend/src/main.tsx` imports `index.css`; Tailwind is configured and used. `App.css` is not the active entry stylesheet via `main.tsx`. Storefront components combine utility classes with custom CSS helpers. The storefront has About, Contact, Reviews, navigation, and footer UI.

The manager ERP is styled predominantly slate/navy with amber/orange utility classes, unlike the white/green storefront palette. This is a visible product inconsistency, not a runtime defect by itself.

At the port-80 runtime, the manager API helper selects relative gateway URLs. Other frontend paths, including `App.tsx` and `CustomerLedger.tsx`, retain direct localhost service URLs. The routing strategy is inconsistent and can behave differently across origins/deployment modes. [nginx.conf](./infrastructure/nginx/nginx.conf) configures gateway routing; `frontend/dist` exists and is mounted by Compose, but parity with current frontend source is **NOT VERIFIED**.

## 22. Frontend theme and styling

The storefront’s active styling is primarily white/emerald/cream with supporting custom CSS and Tailwind; the manager portal uses slate/navy and amber/orange. The existing UI establishes navigation and core portal surfaces but visual consistency across the storefront and ERP is incomplete.

The styling assessment is based on source plus a limited browser view. No responsive, accessibility, cross-browser, or full-page visual regression suite was run.

## 23. Storefront runtime observation

The homepage, storefront shell, hero/navigation, and footer rendered in the browser. The catalogue moved from loading to “Cannot reach Commerce Service.” The category/product GETs returned HTTP 200, while reviews GET returned HTTP 403 via both gateway and direct commerce service.

The frontend source requests catalogue categories, products, and reviews as part of the same loading path and has a generic failure state. The observed 403 correlates with the catalog failure, but the exact browser exception and response body were not captured. Importantly, current source security configuration appears to permit public review reads; therefore the runtime/source discrepancy may involve a stale or different deployed artifact/configuration or a routing/security mismatch. Do not infer the precise root cause without inspecting deployed versions and response details.

## 24. Reporting and notifications

Manager reporting UI includes monthly/yearly selection and production-period requests. The source has a statically initialized reporting period of September 2026. The P&L calculation uses the fixed output-cost estimate described in Section 19 rather than verified batch-cost results.

Notification/alert synchronization is initiated from frontend-fetched data. No scheduled job, delivery channel, actual alert lifecycle, retry, or report-to-ledger accuracy was tested. [docs/SPRINT7_REPORTING_ARCHITECTURE.md](./docs/SPRINT7_REPORTING_ARCHITECTURE.md) and current code/status documents should be reconciled before declaring Sprint 7 complete.

## 25. Security and access-control observations

- JWT signing configuration references a checked-in fallback secret in source. The value is intentionally not reproduced here. Deployment must be shown to provide a strong external secret and reject insecure defaults.
- Auth initialization contains default account/password seed logic. Values are intentionally omitted. Determine whether it runs in the target deployment, rotate any applicable credentials, and verify they are not usable outside local development.
- Legacy human roles remain in the enum, and internal service tokens use a legacy role that does not match the inventory authorization model.
- Finance master configuration allows MANAGER where the amended model reserves at least some configuration for ADMIN.
- The observed review GET returned 403 despite source permitting public reads; a public-catalogue reliability issue and a source/runtime discrepancy.
- Some controllers use permissive cross-origin configuration. CORS is not authorization; production origin policy was not assessed.
- No penetration test, dependency scan, secret scan, authenticated role matrix, brute-force/rate-limit check, or backup/restore exercise was run. No claim of exploitability is made beyond the concrete source/runtime mismatches stated above.

## 26. Build, tests, CI, and deployment evidence

The backend contains 20 Java test source files. Many tests use H2 `create-drop` and some mock inter-service clients, so a green unit suite would still not establish PostgreSQL migration correctness or cross-service integration. Tests were not run. `frontend/package.json` provides build/lint/dev scripts but no test script. E2E/verification scripts exist but were not run.

GitHub Actions CI defines frontend build and Maven test jobs in [.github/workflows/ci.yml](./.github/workflows/ci.yml); neither job was run during this audit. No local build/type-check result is asserted.

Compose/Nginx wiring and a pre-existing `frontend/dist` were inspected, but Docker/container state was not queried. The running localhost endpoints do not prove which image, source revision, or built frontend is serving. No HTTPS, external firewall, backup, restore, monitoring, or deployment rollback readiness was tested.

## 27. Priority takeover blockers

1. **P1 — Review API/runtime mismatch and broken storefront catalogue path.** Runtime GET 403 conflicts with source permit rules; identify the deployed code/config and make the public read path coherent.
2. **P1 — Inventory internal identity mismatch.** Commerce/procurement use `EMPLOYEE`, while inventory requires ADMIN/MANAGER; test and correct service-to-service identity without granting broad human privileges.
3. **P1 — Cost/report correctness.** Replace or formally validate the manager P&L’s fixed ₹25/kg estimate; make calculation failures explicit rather than allowing zero-cost fallbacks to masquerade as valid.
4. **P1 — Cross-service ownership.** Remove/replace production’s direct procurement-schema query and define the unit-cost selection rule and failure semantics.
5. **P1 — Production/stock reconciliation.** Batch persistence can succeed despite inventory-sync failure; establish retry/reconciliation behavior and prove it with integration tests.
6. **P1 — Access model reconciliation.** Resolve finance master roles and legacy role migration against the amended ADMIN/MANAGER/CUSTOMER model.
7. **P2 — Configuration and seed hygiene.** Separate sample financial defaults from approved operational data; verify startup seed behavior against the target database.
8. **P2 — Controlled schema evolution.** Establish migrations and test fresh install, upgrade, and rollback against PostgreSQL.
9. **P2 — Frontend routing/build parity.** Unify gateway/direct API selection and confirm mounted `dist` corresponds to the intended source.
10. **P2 — Evidence and status baseline.** Reconcile Sprint 6/7 project documents and establish repeatable CI, role, integration, and financial acceptance evidence before declaring readiness.

These are prioritization recommendations only; no fixes were made.

## 28. Forty-item prior-claim verification matrix

| # | Claim checked | Classification | Evidence / limitation |
|---:|---|---|---|
| 1 | The repository contains eight application services. | CODE VERIFIED ONLY | Eight service modules are present under `services/`. |
| 2 | The services have distinct domain schemas. | CODE VERIFIED ONLY | Main configs and root schema initializer name eight schemas. |
| 3 | Services are running and healthy at the time of audit. | RUNTIME VERIFIED | GET health probes to ports 8081–8088 returned HTTP 200. Does not prove sustained or dependency health. |
| 4 | The public homepage responds. | RUNTIME VERIFIED | GET `http://localhost/` returned HTTP 200. |
| 5 | The gateway commerce health route responds. | RUNTIME VERIFIED | GET returned HTTP 200. |
| 6 | Commerce category reads respond through the gateway. | RUNTIME VERIFIED | GET returned HTTP 200. |
| 7 | Commerce product reads respond through the gateway. | RUNTIME VERIFIED | GET returned HTTP 200. |
| 8 | Public review reads work. | BROKEN | Direct-service and gateway GET both returned HTTP 403. |
| 9 | The storefront shell renders in the browser. | RUNTIME VERIFIED | Hero, navigation, shell, and footer rendered. |
| 10 | The storefront catalogue loads successfully in the browser. | BROKEN | UI changed from loading to “Cannot reach Commerce Service.” Exact exception/body not captured. |
| 11 | Source security configuration permits the public review read path. | CODE VERIFIED ONLY | Public review route appears in the commerce permit configuration; this conflicts with observed 403. |
| 12 | The running frontend exactly matches current frontend source. | NOT VERIFIED | `frontend/dist` exists and is mounted; source/build parity was not established. |
| 13 | The amended portal model has ADMIN, MANAGER, CUSTOMER roles. | DOCUMENTATION VERIFIED | Formal amendment document establishes this model. |
| 14 | Wholesale customers and employees are portal-login roles under the amendment. | NOT APPLICABLE | The amendment treats them as internal business records, not portal roles. |
| 15 | The role enum contains legacy roles beyond the amendment. | CODE VERIFIED ONLY | `PRODUCTION_MANAGER` and `EMPLOYEE` remain in `Role.java`. |
| 16 | Auth startup contains logic to migrate/disable legacy roles. | CODE VERIFIED ONLY | Initializer contains migration and disable logic; live execution/state not queried. |
| 17 | Commerce and procurement inventory clients use `EMPLOYEE` claims. | CODE VERIFIED ONLY | Both token-building clients claim `EMPLOYEE`. |
| 18 | Inventory controller operations accept the same `EMPLOYEE` identity. | BROKEN | Source guards require ADMIN/MANAGER; service requests were not executed. |
| 19 | Production uses inventory integration with a manager/admin-compatible claim. | CODE VERIFIED ONLY | Source configuration observed; actual request not run. |
| 20 | Finance business-master endpoints are ADMIN-only. | BROKEN | Controller allows ADMIN and MANAGER, conflicting with the amended ADMIN reservation for at least some master configuration. |
| 21 | All customer/object-level isolation cases have been verified. | NOT VERIFIED | Source inspected selectively; no authenticated matrix or cross-customer runtime tests. |
| 22 | Production has recipe/BOM and staged batch lifecycle code. | CODE VERIFIED ONLY | Controller/service/source surfaces exist. |
| 23 | Manager UI also has a simplified completed-batch entry. | CODE VERIFIED ONLY | Daily production UI path exists; not executed. |
| 24 | Batch creation always guarantees inventory synchronization before success. | BROKEN | Source catches inventory-sync failure and can retain/return the batch despite failed sync. |
| 25 | Production costing is fully isolated behind service APIs. | BROKEN | Costing service directly queries procurement schema. |
| 26 | Raw-material lookup failure cannot understate batch cost. | BROKEN | Source fallback can yield zero after errors; actual cost path not run. |
| 27 | Manager P&L uses computed batch-cost output. | BROKEN | Source calculates output kilograms times fixed 25.0 instead. |
| 28 | Payroll defaults are fully externalized/configuration-only. | BROKEN | Service/entity/UI contain 26 days, 8 hours, 1.5 overtime, 0.5 half-day and 100 kg/day fallback. |
| 29 | The UI’s labour/packaging fallback values are configurable-only. | BROKEN | Source displays fixed ₹12.50/kg and ₹4.00/kg when masters are missing. |
| 30 | Seeded commercial/customer amounts are verified current business values. | NOT VERIFIED | Source contains sample seeds; database contents and approved pricing were not queried. |
| 31 | Financial baseline documentation is consistent with configurable costs. | BROKEN | Sprint 6 rules document fixed rupee baselines while source includes cost-master configuration and requirements call for configurability. |
| 32 | All current stock and customer/supplier balances reconcile to ledgers. | NOT VERIFIED | No database query or reconciliation was performed. |
| 33 | Services use versioned migrations for schema evolution. | BROKEN | No service migration set was found; main profiles use `ddl-auto: update` and root SQL bootstrap. |
| 34 | PostgreSQL fresh install and upgrade paths are verified. | NOT VERIFIED | No migration, bootstrap, or restore test was run. |
| 35 | Backend tests currently pass. | NOT VERIFIED | Twenty test source files inventoried; none run. |
| 36 | Frontend build/lint currently passes. | NOT VERIFIED | Scripts exist; neither was run. |
| 37 | CI currently passes on the inspected worktree. | NOT VERIFIED | Workflow definitions exist; no CI run was initiated/inspected. |
| 38 | Frontend routing consistently uses the gateway. | BROKEN | Manager helper uses relative routes in one mode; App/customer ledger retain direct service URLs. |
| 39 | Storefront and ERP use a consistent visual theme. | BROKEN | Storefront is white/emerald/cream; manager ERP is slate/navy with amber/orange accents. |
| 40 | Sprint 8 has started or is authorized by this audit. | NOT APPLICABLE | Explicitly **NOT STARTED / NOT AUTHORIZED**; no Sprint 8 work was performed. |

## 29. Readiness conclusion and handover sequence

**Overall readiness: NOT CERTIFIED for production takeover.** This is not a claim that the system is wholly nonfunctional: the home page, service health routes, and core commerce catalogue reads returned responses, and substantial source functionality exists. It is a statement that critical cross-service, financial, access-model, deployment-parity, and data-integrity assertions remain unresolved or unverified.

Recommended sequence for an authorized follow-on:

1. Preserve this working tree and establish the intended deployable commit/artifact; compare it to the running frontend and service builds.
2. Resolve the review route 403 and internal service identity mismatch; add explicit, least-privilege integration tests.
3. Validate production stock synchronization and error/retry behavior; reconcile batch, inventory, procurement, and ledger records.
4. Establish a single authoritative cost calculation and trace it into reporting; define and test missing-cost/error semantics.
5. Confirm business-approved payroll, price, credit, and cost defaults; remove sample seeds from production paths or gate them explicitly.
6. Map every endpoint/master to the amended role model and verify both allowed and denied access, including object ownership.
7. Introduce and test controlled PostgreSQL schema migrations; capture a read-only baseline of live balances before any data changes.
8. Reconcile Sprint 6/7 documents against implemented acceptance criteria. Only begin Sprint 8 after explicit authorization.

No fixes, builds, tests, database reads, container commands, or state-changing calls were performed for this report.
