# Pre-Sprint 8 Configurable Business Form Architecture

**Status:** READY FOR OWNER ACCEPTANCE — PostgreSQL, Admin, Manager, and
Customer live-gateway verification passed.  
**Scope:** RR Jaggery Traders internal ERP forms only. This is not Sprint 8,
a general low-code system, or authorization for the later UI redesign.  
**Authority:** Owner's phase authorization and
[`ACCESS_MODEL_REVISION.md`](./ACCESS_MODEL_REVISION.md).  
**Database boundary:** The verified local baseline described in
[`PRE_SPRINT_8_DATABASE_RESET.md`](./PRE_SPRINT_8_DATABASE_RESET.md) must not be
reset or reseeded during this phase.

## 1. Architecture overview

Use a small hybrid model:

1. Existing domain entities and their relational columns remain authoritative
   for identity, references, financial/quantity calculations, status, and
   audit.
2. Admin-configurable form definitions describe additional questions and
   select choices.
3. A reusable JPA model/controller/service lives in `common-library`; only
   Finance, Customer-ledger, and Production opt in. Each service owns its form
   definitions, immutable revisions, submissions, and attachment metadata in
   that service's existing schema.
4. An ERP submission keeps a typed domain record as its source of truth. The
   associated form submission stores supplemental values and points to the
   domain record by local service/type and identifier. It does not replace the
   domain record with JSON. Typed select values that need historical display
   are also captured in the associated revision snapshot.
5. No service directly reads or writes another service's schema. No new
   microservice or Microsoft Forms dependency is introduced.

The reusable model is replicated by domain ownership, not by a central
cross-service database. Initial form ownership:

| Owner service/schema | Forms owned in this phase |
|---|---|
| Finance / `finance_schema` | EXPENSE, INVESTMENT, STAFF, ATTENDANCE, PAYROLL |
| Customer-ledger / `customer_schema` | WHOLESALE_ORDER |
| Production / `production_schema` | PRODUCTION |
| Commerce / `commerce_schema` | Product-form extension only when integrated; customer storefront forms remain out of scope |
| Procurement / `procurement_schema` | Supplier/procurement extensions remain a later application of the same model |
| Inventory / `inventory_schema` | Stock extensions remain a later application of the same model |

This boundary keeps each operational write and its custom values in the
database owned by the domain service. Commerce, procurement, and inventory do
not gain cross-schema access. Where one workflow spans services, existing REST
clients remain the integration mechanism; this form model does not create new
cross-service database foreign keys.

## 2. Definition and field model

The conceptual tables are created in the owning service's schema:

### `form_definitions`

- `id` (UUID primary key)
- `form_key` (stable, unique technical key; never edited after creation)
- `display_name`, `description`
- `active`
- `version` (current immutable revision number)
- `created_at`, `updated_at`, `created_by`, `updated_by`

### `form_fields`

- `id` (UUID primary key), `form_definition_id` (local FK)
- `field_key` (stable within a form; immutable after use)
- `label`, `field_type`, `required`, `active`, `display_order`
- `placeholder`, `help_text`
- bounded validation metadata (`min`, `max`, `maxLength`, and decimal `scale`;
  no executable expressions or arbitrary regular expressions)
- `created_at`, `updated_at`, `created_by`, `updated_by`

### `form_field_options`

- `id`, `field_id` (local FK)
- stable `value`, editable `label`, `display_order`, `active`
- creation/update actor and timestamp

Support only `TEXT`, `TEXTAREA`, `NUMBER`, `DECIMAL`, `DATE`, `DATETIME`,
`BOOLEAN`, `SINGLE_SELECT`, `MULTI_SELECT`, `FILE`, and `IMAGE`. Monetary
values use `DECIMAL` only for supplemental/non-accounting values. Important
financial amounts remain fixed typed `BigDecimal`/`NUMERIC` domain fields.

Form keys and field keys are technical identifiers. Admin may change display
labels and help text but not keys relied on by the server. Seed only the small
set of structural form definitions and fields that the runtime needs; do not
seed business categories, payment methods, investors, wage policy, or cost
rates.

## 3. Versions and historical data

Every definition edit that changes runtime behavior creates a monotonically
increasing immutable `form_revisions` row. It contains the definition ID,
version, full serialized snapshot of labels, field keys/types/validation,
active state, order, and select options, plus actor and timestamp. The current
definition/field/option rows are the editable Admin view; revision snapshots
are historical authority.

Each `form_submissions` row stores:

- submission UUID
- form definition ID and immutable revision ID/version
- a service-local domain record type and record ID (where attached to an
  existing core record)
- validated JSON values keyed by stable `field_key`
- submitter and submission timestamp

The implementation stores supplemental values and immutable revision
snapshots as serialized JSON text for PostgreSQL/H2 portability. These payloads
never contain or replace the required relational business record.
Decimal/date values are validated and serialized canonically; select values
are stable option values, not labels. Render historical records against their
stored revision snapshot, not current labels or active options.

For Finance Investments, creating the typed investment also records its
configured `investmentType` value against the new record in the same
transaction. That submission preserves the form revision and option label
needed for historical display; the relational investment remains authoritative.

Never physically delete a form, field, option, revision, or submission that
has historical references. Deactivate forms/fields/options instead. An unused
field/option may be physically removed only if the service can prove it has
never appeared in a submission or immutable revision. Reordering, relabeling,
deactivation, or adding a field creates a new revision; prior snapshots remain
unchanged.

## 4. Fixed core versus configurable questions

“Only the basic field hardcoded” means business-specific extra questions and
choice lists are configurable; it does not mean that accounting and inventory
keys become JSON. These are the minimum fixed semantics for the priority
forms:

| Form | Fixed relational fields and why | Configurable fields |
|---|---|---|
| Expense | Record ID, expense date, amount, currency/precision, category/payment method used by existing reports, creator/timestamps, status/workflow fields. Required for totals, P&L and audit. The current schema stores category and payment method as legacy strings, not master-table foreign keys. | Payee, description, reference, location, receipt and additional questions. Existing Finance category/payment master APIs remain the Admin-managed source of dropdowns; converting legacy expense strings to stable master IDs is a separately scoped schema/API migration. |
| Investment | Record ID, investor name/reference, investment type stable value, amount, date, creator/timestamps, attachment reference. Keeps capital events distinct and queryable; no P&L revenue/expense posting. | Investment type options and additional details/questions. |
| Wholesale order | Existing order/customer/product identities, quantities, prices, tax, totals, status, ledger and stock effects remain in their existing typed records. | Additional delivery/commercial questions only. |
| Production | Existing batch/product identities, dates, input/output/waste quantities, status, consumption/output relationships and costing inputs remain typed and relational. | Additional batch questions not used by stock/cost formulas. |
| Staff | Employee identity and active state; no portal account. Wage basis/rates remain typed domain values consumed by payroll. | Contact/profile details not required by calculations and additional questions. |
| Attendance | Employee reference, attendance date/status and any overtime/quantity values used for wage calculation remain typed. | Notes and additional operational questions. |
| Payroll | Employee reference, period, gross/deduction/net/payment amounts, rule reference, payment and ledger links remain typed and auditable. | Additional explanatory payroll questions that do not alter formulas. |

Supplier, Procurement, Stock, Product, and Customer/Wholesaler can use the same
definition/revision/supplemental-submission model in their owning services
later. This phase must not convert existing catalog, order, inventory,
procurement, ledger, payroll, or batch entities to JSON-only records.

## 5. Calculation semantics and business-value audit

Calculations use fixed core columns, stable technical field keys, or explicit
mapping to a typed domain field. They never inspect the display label.
For example, the stable `investmentType` field key maps to the typed
`investments.investment_type` value and validates against that form field's
active option values. A label change cannot change the mapping. Existing
Expense report grouping still uses its legacy category string; this phase does
not pretend it has a relational category-ID mapping. No administrator-authored
formula or arbitrary expression is allowed.

Targeted source/documentation audit:

| Classification | Existing value/pattern | Treatment |
|---|---|---|
| A — fixed | Expense, payroll, attendance, batch, order, tax, ledger and stock identifiers/quantities/amounts used by calculations. | Keep typed/relational; do not make form labels or JSON authoritative. |
| B — configurable | Existing Finance expense categories, payment methods, advance/deduction types, payroll rules, and Production cost masters. | Preserve existing rows. Finance master reads remain available to managers; master creation/update/deactivation endpoints are now ADMIN-only. Do not reseed or silently replace them. |
| D — owner decision | Payroll fallbacks of 26 working days, 8 hours/day, 1.50 overtime multiplier, 0.50 half-day factor, 100 kg per attended day when quantity is absent; DAILY default and fixed wage-type list. | Flag explicitly. Do not silently redefine in this architecture phase. Calculation paths must fail/validate rather than imply that an unapproved fallback is an Admin-configurable question. |
| D — owner decision | Finance `CASH` payment defaults and UI fallback methods; manager expense default “Diesel / Fuel”; supplier/stock “Raw Materials”; customer-ledger hardcoded payment/customer/status choices. | Follow-up needed: existing UI defaults/fallbacks are not changed by this backend phase. Use active service-owned master/API values where an existing master exists. Preserve required fixed workflow states; do not invent replacements. |
| D — owner decision | Production costing groups masters by matching type/name text; missing cost masters cause display fallbacks of ₹12.50/kg labour and ₹4/kg packaging. | Do not infer semantic cost category from editable labels. Map stable cost-master keys/types explicitly. Remove UI presentation fallbacks as part of the relevant integration only; never use them as rates. |
| D — owner decision | Documented costing baselines for cane, lime, oil, bagasse, labour, fuel, packaging, and other costs. | The rules document is not proof of approved live policy. Existing master rows remain authoritative until the owner approves changes. |
| A — defect, separate follow-up | Manager P&L estimates production cost as output KG × 25.0 rather than actual batch costing. | Do not enshrine this as a form default. Correct report integration only if directly required by the form work; otherwise retain as a clearly reported existing defect for separate authorization. |
| C — technical | Current date initialization, UUID generation, decimal rounding/precision and standard validation limits. | Safe only as technical behavior; never use as a business rate/category. |

This classification records findings, not approval of any D value. Hardcoded
sample prices in historical seed templates are not promoted into new business
masters.

## 6. Roles and API contract

Backend authorization is required on every endpoint. Authorities are exactly
`ROLE_ADMIN`, `ROLE_MANAGER`, and `ROLE_CUSTOMER`; wholesale customers and
employees do not receive portal accounts.

| Operation | ADMIN | MANAGER | CUSTOMER |
|---|---:|---:|---:|
| List/view internal definitions and active fields | Allow | Allow only forms usable operationally | Deny |
| Create/update/deactivate/reorder definition, fields, options | Allow | Deny | Deny |
| Submit/view own permitted internal operational records | Allow | Allow by form policy | Deny |
| Investment management | Allow | Deny | Deny |
| Submit/view permitted Expense, Wholesale Order, Production, Attendance | Allow | Allow | Deny |
| Staff master and payroll rule/definition management | Allow | Deny | Deny |
| Payroll computation/entry | Allow | As existing role policy permits | Deny |

Implemented service-local REST routes:

- Finance base: `/api/v1/finance/forms`; Customer-ledger base:
  `/api/v1/customers/forms`; Production base: `/api/v1/production/forms`.
- `GET /definitions` and `GET /definitions/{formKey}` for authorized internal
  users. Managers see only active forms permitted to their operational role.
- `POST /definitions`, `PUT /definitions/{formKey}`, and
  `PATCH /definitions/{formKey}/status` for ADMIN only.
- `POST /definitions/{formKey}/fields`, `PUT /fields/{fieldId}`,
  `PATCH /fields/{fieldId}/status`, and
  `PATCH /definitions/{formKey}/fields/reorder` for ADMIN only.
- `POST /fields/{fieldId}/options`, `PUT /field-options/{optionId}`, and
  `PATCH /field-options/{optionId}/status` for ADMIN only.
- `POST /submissions` saves validated supplemental values. Core form keys
  require an existing service-owned record ID; a domain repository adapter
  verifies that record. Admin-created non-core forms receive a generated
  submission ID. `GET /submissions?recordType=...&recordId=...` returns only
  internal records permitted by role.
- Finance investment creation writes its typed record and configured type
  snapshot atomically; later supplemental answers use the regular submission
  endpoint.
- `GET /definitions/{formKey}/revisions/{version}` returns the immutable
  historical snapshot.

Use UUIDs for entity identifiers and Bean Validation at boundaries. Validate
required active fields, exact value type, decimal bounds/scale, length,
date-time syntax, and that every select value belongs to an active option in
the current revision. Unknown/inactive keys are rejected. FILE/IMAGE answers
are explicitly rejected with HTTP 501 until secure upload is implemented.
Core business fields are still validated by their existing typed request/
service paths. The custom-answer POST is a separate request after the
service-owned typed record is created, not yet an atomic combined form
submission. Customer-facing security configurations deny internal form paths.

Investment records are stored separately from expenses and operating revenue.
They must not be included in P&L, COGM, factory OPEX, payroll, sales, or ledger
totals unless a separately approved accounting workflow posts them.

## 7. Attachments

`FILE` and `IMAGE` are supported field-definition types and an attachment
metadata entity exists in each opted-in schema. Binary upload and download
endpoints are not implemented. Submitting a value for either type returns
HTTP 501 rather than accepting a path or arbitrary file name. A future upload
path must use opaque storage keys, enforce role access, cap size, allowlist
MIME and extensions, inspect file signatures, generate server-side names,
and reject executable/active content. The Commerce product-image path is
product-specific and is not reused for private ERP attachments.

## 8. Audit and service ownership

There is no common Spring Data auditing base in the inspected repository.
Definitions, fields, and options capture `created_by`, `updated_by`, and
timestamps. Every edit appends an immutable `form_revisions` snapshot with
actor and time; the prior version is the before-state. The actor comes from
the authenticated security context, never a caller-supplied identity.

Each service accesses only its own schema. `common-library` contains the
reusable JPA mappings and REST/controller logic, but each opted-in service
scans and persists those entities against only its own configured
`default_schema`. Domain-record existence is checked by a small
service-specific adapter using that service's own repositories. No
cross-schema joins or table access are introduced.

## 9. Migration and rollout strategy

Current services use Hibernate `ddl-auto: update`; the repository has no
established Flyway/Liquibase history. Additive tables are materialized by the
existing local profile. The versioned PostgreSQL DDL at
[`V20260930_01__configurable_forms.sql`](../services/db/migrations/V20260930_01__configurable_forms.sql)
was applied once to the local development database after a verified backup.
It is not wired to an automatic migration runner. The script's foreign-key
`ALTER TABLE ... ADD CONSTRAINT` statements are not rerun-safe; do not rerun
it without checking each constraint. Before production rollout, use
backup/rollback planning and switch the production profile away from
unconstrained schema auto-update. No baseline table was dropped, truncated,
or reset for this integration gate.

Existing preserved master rows in `finance_schema` and
`production_schema.production_cost_masters` remain untouched. Seed only
structural definitions required to expose the supported forms; do not create
expense categories, payment methods, investors, payroll rules, wages, or
costing rates automatically. Migration of existing values to new master
representations requires an explicit mapping and must preserve their IDs and
historical meaning.

## 10. Minimal UI boundary

If a UI is needed to exercise the APIs, add only a functional configuration
view for authorized Admins and a runtime submission view for permitted
Managers. Keep it within existing navigation and styling conventions. Do not
redesign ManagerPortal, Customer pages, storefront, navigation, global theme,
or layout; do not add animation or visual polish.

## 11. Verification gates

The implementation must test definition CRUD/permissions, option management,
revision snapshots, required/type/select validation, custom-value persistence,
historical reads after relabel/deactivation/reorder, immutable used fields,
investment separation, service-local ownership, attachment validation if
binary upload ships, and role isolation for Admin/Manager/Customer. Re-run
existing finance, production, customer-ledger, commerce, and auth tests as
affected. The current integration-gate test transactions are explicitly
identified below; do not create other demo business transactions.

**Stop condition:** after this authorized architecture phase, report the
changes and evidence and wait for owner review. Do not begin broad UI redesign,
deployment, Sprint 8, or unrelated stabilization.

## 12. Implemented scope and remaining boundaries

The backend foundation is implemented in `common-library`, with Finance,
Customer-ledger, and Production opted in using their own schemas and
service-owned record validators. Structural definitions cover Expense,
Investment, Staff, Attendance, Payroll, Wholesale Order, and Production.
Investment has a separate typed API/table; it is not counted as an operating
expense or revenue. Existing Finance business-master mutation endpoints are
Admin-only while reads remain available to Managers.

The PostgreSQL DDL file has been applied once to the development database
after the pre-migration backup recorded below. It is a single-run
transactional script; table and index creation use `IF NOT EXISTS`, but
foreign-key additions do not, so the complete script must not be rerun against
a partially or fully applied schema without first checking the constraints.

There is no new form UI in this phase. Core typed record creation remains on
existing service endpoints; custom-answer submission is separate for forms
other than the atomic Investment type snapshot. Binary FILE/IMAGE handling
remains unavailable and is rejected with HTTP 501. These boundaries are
intentional; broad UI work, deployment, database reset, and Sprint 8 remain
out of scope.

## 13. Final PostgreSQL integration gate

### Database and migration

- Database: `rr_jaggery_db`; live server: PostgreSQL **16.15**.
- Before migration, a compressed custom-format backup was created at
  `C:\Users\rohan\.copilot\session-state\ae48c205-dfe7-4921-8325-65aeff329329\files\rr_jaggery_db_pre_forms_20260930.dump`
  (**105,075 bytes**). `pg_restore --list` successfully read its archive
  table of contents. No password, token, or credential is recorded here.
- `V20260930_01__configurable_forms.sql` was applied successfully as one
  transaction. `finance_schema`, `customer_schema`, and `production_schema`
  each contain the six expected `form_*` tables. Finance additionally has
  the typed `investments` table.
- PostgreSQL catalog inspection confirmed the copied `LIKE ... INCLUDING ALL`
  indexes are attached to tables in the destination schema. Every form
  foreign key in Finance, Customer-ledger, and Production references a table
  in that same schema; no copied reference points back to `finance_schema`.
- The pre-existing Finance masters remain at 11 expense categories, 10
  payment methods, and 1 payroll rule; Production retains 9 cost masters.
  These match their pre-migration counts. Existing catalog/order/customer/
  production transaction tables were not reset. The two explicitly marked
  integration-test records below are the only business rows created by this
  gate.

### Service startup and health

The Finance, Customer-ledger, and Production images were packaged from the
current source and rebuilt/restarted. Their PostgreSQL startup completed.
All eight service `/actuator/health` endpoints returned HTTP 200; the gateway
root returned HTTP 200, and PostgreSQL `pg_isready` reported accepting
connections. The existing gateway and PostgreSQL containers remained up.

### Gateway API and role evidence

- A valid pre-existing local Admin bearer token was accepted by
  `/api/v1/auth/me`; the returned role was `ADMIN`. It was not printed or
  stored in this report. Admin API requests through the gateway successfully
  read Finance definitions, manage fields/options/order/status, read revision
  history, create an Investment, create an Expense, and submit/read associated
  form data.
- Anonymous requests to Finance, Customer-ledger, Production form
  definitions and Finance Investments returned HTTP 403. Authenticated Admin
  requests to the three definitions endpoints and Investment endpoint
  returned HTTP 200.
- The preserved enabled ADMIN and MANAGER account-role counts remain one each.
  A Manager credential/token was not available for this gate, so live Manager
  login, Manager submission/read, Manager denial of builder/Investment
  mutations, and Customer-role denial were **not** verified against the live
  gateway. Existing H2 API tests do verify Manager operational access and
  Admin-only restrictions, and Customer denial, but this does not substitute
  for the requested live role tests.

### PostgreSQL business workflow evidence

- **Investment:** The actual Finance API created investment
  `e176975e-6bae-4049-a8f1-a7f5d46886cb`, value `0.01`, type
  `GATE_TEST_ONLY`. A linked submission stored the typed option value and the
  then-current label `Temporary Integration Gate Type` in revision 3.
  The option was later relabeled and deactivated; the old submission and
  original active label/state remain in its immutable revision snapshot.
  Investment remains a row in its own `investments` table, not
  `operating_expenses`. No investment consumer was found in the Finance,
  Production, or Commerce calculation/report source. Commerce orders/order
  items and Production batches/outputs remained at zero.
- **Expense:** The actual typed Expense API created
  `a11f9563-3ad5-49d8-93f3-78ea104988b5`, dated 2026-09-30, amount `0.10`,
  category `Gate Verification`. A separate form API request saved
  `gateTestField` and `gateTestChoice` against that Expense. Core date,
  amount, category, and identity remain in `operating_expenses`; supplemental
  values are in `form_submissions`. Changing the field/option labels and
  deactivating the test field/option did not rewrite the old answer or
  revision-7 labels.
- An explicit historical revision API read returned the original
  `Test Business Field` label. A FILE answer containing a path-like string
  returned HTTP 501; no form submission was added by the failed request and
  `form_attachments` remains empty.
- The test-only Expense and Investment, their two submissions, and immutable
  revisions have been retained rather than deleting records referenced by
  history. All `gateTest*` Expense fields are inactive, and the temporary
  Investment option is inactive. No other test business rows were created.

### Workflow integration and atomicity classification

| Priority form/workflow | Integration state | Atomicity assessment |
|---|---|---|
| Investment | **A** — actual Admin API workflow and PostgreSQL persistence/history verified | **SAFE** — typed Investment and required configured type submission are written in one service transaction |
| Expense | **B** — typed create plus separate supplemental submit verified with Admin; requested Manager workflow not live-verified | **PARTIAL / POSSIBLE ORPHANED FORM DATA** — the typed Expense can persist if the follow-up configured submission is omitted or fails |
| Wholesale Order | **B** — form definition/record adapter exists; existing business workflow not exercised against PostgreSQL | **PARTIAL / POSSIBLE ORPHANED FORM DATA** — supplemental submit is separate |
| Production | **B** — form definition/record adapter exists; existing business workflow not exercised against PostgreSQL | **PARTIAL / POSSIBLE ORPHANED FORM DATA** — supplemental submit is separate |
| Staff | **B** — form definition/record adapter exists; existing business workflow not exercised against PostgreSQL | **PARTIAL / POSSIBLE ORPHANED FORM DATA** — supplemental submit is separate |
| Attendance | **B** — form definition/record adapter exists; existing business workflow not exercised against PostgreSQL | **PARTIAL / POSSIBLE ORPHANED FORM DATA** — supplemental submit is separate |
| Payroll | **B** — form definition/record adapter exists; existing business workflow not exercised against PostgreSQL | **PARTIAL / POSSIBLE ORPHANED FORM DATA** — supplemental submit is separate |

No generic transaction framework or unrelated workflow rewrite was added.
Domain-specific atomic commands for non-Investment workflows remain an owner
decision if supplemental form data becomes mandatory.

### Automated tests and final readiness

- `mvn -f services\pom.xml test` — **BUILD SUCCESS** across the parent,
  common library, and all eight services. Aggregated Surefire XML results:
  **66 tests, 0 failures, 0 errors, 0 skipped**. These service tests use
  isolated H2 databases; separate live PostgreSQL checks are documented above.
- Focused Finance form API run —
  `mvn -f services\pom.xml -pl finance-service -am "-Dtest=FormRegistryApiTests" "-Dsurefire.failIfNoSpecifiedTests=false" test`
  — **6 tests, 0 failures, 0 errors, 0 skipped**.
- Phase-touched tracked files passed `git diff --check`. The whole-worktree
  check reported whitespace in unrelated pre-existing changes; those were
  left untouched.
- The live Admin investment/Expense and historical revision workflows passed.
  This document is updated below with the final live-role-only verification.
  No form UI, broad redesign, deployment, or Sprint 8 work was started.

## 14. Final live role verification

- **Temporary Manager account:** Created once through the existing
  Admin-protected `/api/v1/auth/admin/managers` endpoint with a temporary
  generated password; normal `/api/v1/auth/login` and `/api/v1/auth/me`
  returned HTTP 200 and confirmed `MANAGER`. No existing account or password
  was changed.
- **Manager permitted access:** Finance form definitions, Expense fields,
  configured category/payment choices, Expense create, supplemental form
  submission, and reads of the created Expense and submission succeeded:
  HTTP 200 for reads and HTTP 201 for creates/submission.
- **Manager builder/Investment denials:** All tested create/update/deactivate
  form operations; add/edit/deactivate field/option; reorder fields; change
  Investment form configuration; Investment list; and Investment create
  returned HTTP 403.
- **Temporary Customer account:** Created once through the existing
  `/api/v1/auth/register` endpoint with the ordinary `CUSTOMER` role. Normal
  login and `/api/v1/auth/me` returned HTTP 200 and confirmed `CUSTOMER`.
- **Customer internal API denials:** Finance, Customer-ledger, and Production
  form-definition reads; Finance and service-local form-definition
  create/update attempts; Finance field add/edit; Investment list/create;
  Expense list/read; and internal Expense form submission/read all returned
  HTTP 403 with the authenticated Customer token.
- **Customer-facing access:** The public Commerce catalogue endpoint
  `/api/v1/commerce/products` returned HTTP 200. It is intentionally public;
  Customer authorization is not required for this catalogue read.
- **Temporary account cleanup:** Both temporary accounts were disabled
  through the existing Admin user-management endpoint. They remain as
  disabled audit records because no safe deletion endpoint is provided.
  Enabled role counts returned to ADMIN: 1 and MANAGER: 1; there is no enabled
  Customer.
- **Database side effects:** The explicitly authorized minimal Manager
  Expense `d4da850e-1831-4bc2-af31-e187053cad76` is tagged
  `PRE_SPRINT8_ROLE_GATE_TEST`; one associated form submission was created
  and read by Manager. Manager-denied operations created no definitions,
  fields, options, revisions, or Investments. Customer-denied operations
  created no business or form data. The earlier integration-gate Investment
  and Expense were unchanged.
- **Source changes:** None required or made. The suite was not rerun because
  this gate changed no source; the prior backend results remain 66 tests and
  6 focused Finance tests passed.
- Temporary passwords and JWTs were kept only in process memory; none were
  printed or written to files.

**READY FOR OWNER ACCEPTANCE**
