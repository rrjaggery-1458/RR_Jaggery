# Pre-Sprint 8 Docker and Git Integration Report

## Final status

**BLOCKED — live reset and integration verification passed, but the Git
commit/push gate remains intentionally incomplete.** The worktree contains
many pre-existing or otherwise unclassified changes, so staging or committing
the integration state could include unrelated work. No commit or push was
attempted.

## Database reset and persistence

- The development Compose stack was reset by removing only the volumes
  previously identified as belonging to this Compose project: PostgreSQL,
  Redis, and uploads. They were recreated by the fresh startup.
- PostgreSQL database: `rr_jaggery_db`.
- Service schemas verified: `auth_schema`, `commerce_schema`,
  `customer_schema`, `inventory_schema`, `procurement_schema`,
  `production_schema`, `finance_schema`, and `notification_schema`.
- A pre-reset database backup/export location is not present in the retained
  execution evidence; this report therefore does not claim that a backup was
  performed.
- The stack was stopped and started again without removing volumes. All
  service health endpoints and the gateway returned HTTP 200 after the
  restart. PostgreSQL data persisted across that restart.

## Post-restart database evidence

Counts below are exact PostgreSQL `COUNT(*)` results after restart.

### Authentication

| Role | Enabled | Rows |
|---|---:|---:|
| ADMIN | yes | 1 |
| MANAGER | no | 1 |
| CUSTOMER | no | 1 |

The single Admin logged in through the gateway (HTTP 200), and the
authenticated `/api/v1/auth/me` request returned HTTP 200 with role `ADMIN`.
The temporary Manager and Customer authorization-test accounts were disabled
through the Admin API; the service has no account-delete route. No credentials
or tokens are recorded here.

### Business, transaction, and structural-form tables

Every service-owned non-form table in the exact-count query had zero rows,
including:

- Commerce: carts, categories, products, orders, order items, and reviews.
- Customer/ledger: customers, offline orders/items, ledger entries, and
  payments.
- Inventory: items and stock movements.
- Procurement: suppliers, purchase orders/lines, goods receipts, and supplier
  ledger entries.
- Production: recipes/items, batches, consumptions, outputs, wastage, and cost
  masters.
- Finance: expenses, investments, employees, attendance, payroll records,
  rules, advances, deductions, payments, and employee ledger entries.
- Notifications: notifications.
- Configurable-form submissions and attachments: zero in customer, finance,
  and production schemas.

Structural form data intentionally remained:

| Schema | Definitions | Fields | Options | Revisions | Submissions |
|---|---:|---:|---:|---:|---:|
| customer_schema | 1 | 0 | 0 | 1 | 0 |
| finance_schema | 5 | 1 | 0 | 8 | 0 |
| production_schema | 1 | 0 | 0 | 1 | 0 |

These are application form definitions/revision history, not business
transactions. Business master/configuration tables were not populated with
sample values.

## Live role verification

The checks below were performed through the Docker gateway. Authorization
denials were HTTP 403.

| Role | Check | Result |
|---|---|---|
| Admin | Login and `/auth/me` | HTTP 200; role `ADMIN` |
| Admin | Existing form-definition and field read/update checks | HTTP 200 |
| Manager | Permitted finance definitions and Expense access | Allowed |
| Manager | Expense create, read, then delete verification workflow | HTTP 201, 200, 200; temporary record removed |
| Manager | Ten form-builder mutation attempts | HTTP 403 |
| Manager | Investment management | HTTP 403 |
| Customer | Finance definitions and form-builder mutation | HTTP 403 |
| Customer | Internal customer-ledger and production definitions | HTTP 403 |
| Customer | Investment management and internal Expense submission | HTTP 403 |

No unauthorized form-definition mutation or Investment row was created. No
Expense verification record remained after cleanup. No other business
transaction was created. The final database query confirmed zero rows in
`finance_schema.operating_expenses`, `finance_schema.investments`, and all
form-submission tables.

## Application and test verification

- All eight service health endpoints plus the gateway: HTTP 200 after restart.
- Admin login and authenticated identity after restart: HTTP 200 / HTTP 200.
- Backend Maven reactor test suite: 75 tests, 0 failures, 0 errors, 0 skipped.
- `mvn -f services/pom.xml package -DskipTests`: succeeded.
- `docker compose build`: succeeded for the eight backend images.
- Frontend production build: succeeded.
- Frontend lint: exited successfully with existing/nonfatal warnings; Vite
  reported a large JavaScript chunk.
- Browser verification at the gateway root: page/assets loaded; API calls used
  same-origin gateway routes; no console errors or failed/4xx/5xx requests.
- Compose configuration validation succeeded with the existing warning that
  the Compose `version` attribute is obsolete.

The gateway-routing changes in `frontend/src/App.tsx` and
`frontend/src/CustomerLedger.tsx` were necessary for production pages served
through the Docker gateway. No changes to role authorization rules were made
for the live role checks.

## Git and safety record

- Branch: `main`; remote: `origin` (`rrjaggery-1458/RR_Jaggery`).
- The worktree was not clean and contained numerous pre-existing or
  unclassified modifications. The last status count observed was 99 entries;
  the unstaged diff spanned 41 files. Nothing was staged.
- No commit or push was attempted. The changes were not safely separable from
  the existing worktree state.
- Bootstrap configuration values and authentication tokens were not written
  to this report or repository files. The supplied token file was not read.
- No database reset occurred after the fresh reset described above; the
  post-reset stop/start verification retained the PostgreSQL volume.
- No volumes were removed after their fresh recreation.

## Remaining action

The database and live integration gates are verified. Before any commit or
push, the owner must classify the existing worktree changes and decide how to
isolate the intended changes. A pre-reset backup should also be confirmed
from an independent record if one was created.
