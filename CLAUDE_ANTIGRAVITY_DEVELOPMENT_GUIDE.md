# RR Jaggery Traders — Antigravity Development Guide

## 1. Mission

Build RR Jaggery Traders as a production-capable business platform according to the approved requirements document.

The objective is not merely to generate source code.

The objective is to produce a working, tested, maintainable and deployable application.

## 2. Development Strategy

Work incrementally.

Never attempt to implement all business modules in a single operation.

Follow:

Sprint 0 → Sprint 1 → Sprint 2 → Sprint 3 → Sprint 4 → Sprint 5 → Sprint 6 → Sprint 7 → Sprint 8 → Sprint 9

Do not skip a sprint checkpoint.

## 3. Before Coding

Before implementing a sprint:

* Read the relevant requirements.
* Inspect the existing repository.
* Inspect the current implementation status.
* Inspect existing architecture decisions.
* Inspect database migrations.
* Inspect tests.
* Identify dependencies.
* Identify ambiguity.

If ambiguity affects business correctness, ask the user.

## 4. During Coding

Prefer small, understandable changes.

Do not create unnecessary abstractions.

Do not introduce infrastructure without justification.

Do not duplicate business logic between frontend and backend when the backend should be authoritative.

Keep API contracts explicit.

Keep database ownership clear.

## 5. Database

PostgreSQL is authoritative.

Use migrations.

Never casually modify production data.

Never delete tables or important data merely to make tests pass.

If a destructive migration is required, stop and request approval.

## 6. Frontend

The frontend must consume the actual backend APIs.

Do not create fake API responses for completed features.

Mock data is permitted only for explicitly identified development/test purposes.

## 7. Backend

Use clear layering such as:

Controller
→ Service
→ Repository
→ Database

Keep business rules in appropriate backend services.

Validate incoming requests.

Enforce authorization.

Return meaningful API errors.

## 8. Financial Integrity

Treat the following as high-risk domains:

* customer ledger
* supplier ledger
* payments
* credit
* expenses
* payroll
* production costing
* inventory value

Never guess business behavior in these areas.

Use decimal-safe calculations.

Test edge cases.

## 9. Inventory Integrity

Stock changes must be traceable.

Avoid directly modifying stock balances without an appropriate stock movement or controlled adjustment mechanism.

Inventory operations must be transactional.

## 10. Production Integrity

Production must maintain traceability between:

Raw materials
→ Production batch
→ Consumption
→ Output
→ Wastage
→ Finished stock

Do not simplify production calculations without approval.

## 11. Git

Use meaningful commits.

Prefer:

feat:
fix:
test:
refactor:
docs:
chore:

Do not commit secrets.

Do not commit generated build artifacts unless explicitly required.

## 12. Branching

Use feature branches for meaningful work.

Use pull requests where appropriate.

Do not make large unrelated changes in one commit.

## 13. CI/CD

CI must validate the project before production deployment.

At minimum verify:

* build
* tests
* frontend build
* backend build
* Docker build
* ARM64 compatibility where applicable

## 14. Docker

The complete development environment must be reproducible.

Avoid:

* machine-specific paths
* hard-coded hostnames
* hard-coded credentials
* assumptions that only work on Windows

The application must ultimately run on Linux ARM64.

## 15. Production

Production deployment target:

OCI Always Free Ampere A1 ARM64 VM.

Initial topology:

Cloudflare
→ Nginx
→ React/Spring Boot containers
→ PostgreSQL

Use Docker Compose.

Do not introduce Kubernetes for the initial deployment.

## 16. Secrets

Never request that the user paste production secrets into source files.

Use:

.env
environment variables
GitHub secrets
OCI configuration
other appropriate secret mechanisms

Never commit real credentials.

## 17. When to Stop

Stop and ask the user if:

* business logic is ambiguous
* financial behavior is ambiguous
* stock behavior is ambiguous
* production behavior is ambiguous
* architecture must change
* a destructive migration is needed
* production data may be affected
* a major new technology is required
* deployment could cause irreversible impact

Do not silently choose a business rule.

## 18. Sprint Completion

A sprint is complete only after:

* implementation
* automated tests
* integration verification
* browser verification where applicable
* Docker verification
* Runnable Checkpoint
* Demo
* Acceptance
* documentation update

## 19. Status Files

Maintain:

IMPLEMENTATION_STATUS.md
DECISIONS.md
CHANGELOG.md

These files must remain accurate.

## 20. Final Principle

Prefer:

simple
→ correct
→ tested
→ maintainable
→ deployable

over:

complex
→ impressive
→ unnecessary

Do not add complexity merely because it is technically possible.
