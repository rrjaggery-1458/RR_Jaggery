# Pre-Sprint 8 Local Development Database Reset

**Date:** 2026-09-30  
**Status:** Database reset and post-restart baseline verification completed. Valid ADMIN/MANAGER logins and all eight gateway health checks pass. The original HTTP 500 was traced to malformed JSON in the probe and is corrected to return HTTP 400.  
**Boundary:** Local development database reset plus narrowly scoped auth/seeder repair. No schema design, Git history, or Sprint 8 implementation was changed.

## 1. Target identification and backup

The active database was identified from the running Compose project at `E:\RR Jaggery`:

| Property | Verified value |
|---|---|
| Compose project/service | `rrjaggery` / `postgres` |
| PostgreSQL container | `rr-postgres` |
| PostgreSQL version | 16.15 |
| Database | `rr_jaggery_db` |
| Database role used for the local backup/reset | `postgres` |
| Persistent database volume | `rrjaggery_postgres_data`, mounted at PostgreSQL's data directory |
| Schemas in scope | `auth_schema`, `commerce_schema`, `customer_schema`, `inventory_schema`, `procurement_schema`, `production_schema`, `finance_schema`, `notification_schema` |

Before changing database records, the local application containers were stopped while PostgreSQL remained running. A compressed custom-format `pg_dump` was created and its archive listing was validated with `pg_restore --list`. The verified backup is:

```text
C:\Users\rohan\.copilot\session-state\ae48c205-dfe7-4921-8325-65aeff329329\files\rr_jaggery_db_pre_reset_20260930.dump
```

File size: **128,109 bytes**. The backup contains the pre-reset database contents and should be handled as sensitive local data. No passwords, tokens, or secret values are recorded here. The database container, database, schemas, and persistent volume were not deleted.

## 2. Classification and reset policy

| Class | Treatment |
|---|---|
| System structure | Preserved. All eight schemas, tables, constraints, extensions, and PostgreSQL itself remain. |
| Authentication baseline | Preserved the existing enabled ADMIN and MANAGER accounts, with one matching role assignment each. Removed all other portal accounts and role assignments. No account was recreated by the reset transaction. |
| Business masters/configuration | Preserved existing Finance and Production master rows. Their provenance was not established from the database alone; retaining them avoids destroying potentially owner-configured values. No master values were inserted by this reset. |
| Business catalog and transactions | Cleared the Commerce catalog and all existing transactional, customer, stock, procurement, production, finance, and notification records listed below. |
| Recipes/BOM | Cleared the tables; both were already empty before the reset. |

Finance configuration retained: 4 advance types, 4 deduction types, 11 expense categories, 10 payment methods, and 1 payroll rule. Production configuration retained: 9 production cost master rows. These are configuration/business-master records, not transaction evidence. Their correctness and owner approval were not assessed.

The reset used one PostgreSQL transaction and explicit `TRUNCATE ... RESTART IDENTITY` statements for the selected data tables, without `CASCADE`. It did not drop schemas or tables. Auth cleanup retained the existing development ADMIN/MANAGER baseline and removed the other user accounts.

## 3. Before/after table counts

Counts are exact PostgreSQL `count(*)` results. “Preserved” identifies business-master/configuration tables intentionally left intact.

| Schema.table | Before | After | Treatment |
|---|---:|---:|---|
| `auth_schema.user_roles` | 32 | 2 | Retained ADMIN and MANAGER assignments only |
| `auth_schema.users` | 32 | 2 | Retained existing enabled ADMIN and MANAGER accounts only |
| `commerce_schema.cart_items` | 0 | 0 | Cleared |
| `commerce_schema.categories` | 4 | 0 | Cleared per request |
| `commerce_schema.order_items` | 1 | 0 | Cleared |
| `commerce_schema.orders` | 1 | 0 | Cleared |
| `commerce_schema.product_reviews` | 4 | 0 | Cleared |
| `commerce_schema.products` | 10 | 0 | Cleared per request |
| `customer_schema.customer_ledger` | 1 | 0 | Cleared |
| `customer_schema.customer_payments` | 0 | 0 | Cleared |
| `customer_schema.customers` | 11 | 0 | Cleared |
| `customer_schema.offline_order_items` | 1 | 0 | Cleared |
| `customer_schema.offline_orders` | 1 | 0 | Cleared |
| `inventory_schema.inventory_items` | 19 | 0 | Cleared; no old stock remains |
| `inventory_schema.stock_movements` | 35 | 0 | Cleared |
| `procurement_schema.goods_receipts` | 9 | 0 | Cleared |
| `procurement_schema.purchase_order_lines` | 9 | 0 | Cleared |
| `procurement_schema.purchase_orders` | 9 | 0 | Cleared |
| `procurement_schema.supplier_ledger_entries` | 9 | 0 | Cleared |
| `procurement_schema.suppliers` | 9 | 0 | Cleared |
| `production_schema.batch_consumptions` | 3 | 0 | Cleared |
| `production_schema.batch_outputs` | 3 | 0 | Cleared |
| `production_schema.batch_wastages` | 3 | 0 | Cleared |
| `production_schema.production_batches` | 14 | 0 | Cleared; no old production remains |
| `production_schema.production_cost_masters` | 9 | 9 | Preserved configuration |
| `production_schema.recipe_items` | 0 | 0 | Cleared; already empty |
| `production_schema.recipes` | 0 | 0 | Cleared; already empty |
| `finance_schema.advance_types` | 4 | 4 | Preserved business master |
| `finance_schema.attendance` | 19 | 0 | Cleared |
| `finance_schema.deduction_types` | 4 | 4 | Preserved business master |
| `finance_schema.employee_ledger` | 12 | 0 | Cleared |
| `finance_schema.employees` | 11 | 0 | Cleared |
| `finance_schema.expense_categories` | 11 | 11 | Preserved business master |
| `finance_schema.operating_expenses` | 7 | 0 | Cleared; no old expenses remain |
| `finance_schema.payment_methods` | 10 | 10 | Preserved business master |
| `finance_schema.payroll_records` | 0 | 0 | Cleared; already empty |
| `finance_schema.payroll_rules` | 1 | 1 | Preserved configurable rule |
| `finance_schema.salary_advances` | 5 | 0 | Cleared |
| `finance_schema.salary_deductions` | 3 | 0 | Cleared |
| `finance_schema.salary_payments` | 4 | 0 | Cleared |
| `notification_schema.notifications` | 9 | 0 | Cleared |

All listed Commerce orders/reviews/products/categories, customer/supplier balances and records, inventory, procurement transactions, production batches, expenses, payroll/employee transactions, and notifications now have zero rows. The old historical figures cited in prior project documents were not independently matched to specific pre-reset database rows; their relevant tables were cleared regardless.

## 4. Authentication baseline

Before application startup/reset, PostgreSQL showed 32 users: 1 ADMIN, 16 MANAGER, and 15 CUSTOMER role assignments. Of those, 1 ADMIN, 5 MANAGER, and 14 CUSTOMER accounts were enabled. These are aggregate counts only; account identifiers and credentials are intentionally omitted.

The existing ADMIN and MANAGER baseline accounts were enabled and had their expected single roles before reset. After reset:

- `auth_schema.users`: **2** rows.
- `auth_schema.user_roles`: **2** rows, exactly `ADMIN:1` and `MANAGER:1`.
- Enabled ADMIN/MANAGER baseline accounts: **2**.
- CUSTOMER role assignments: **0**. Customer registration was not exercised because it would create an account in the clean baseline.
- Assignments for `PRODUCTION_MANAGER`, `EMPLOYEE`, or `REGISTERED_WHOLESALE`: **0**.
- Active wholesale portal accounts after auth-service startup: **0**.

The auth service was started while the old baseline data was still present; startup completed before the reset, so its startup initializer did not need to create the retained ADMIN/MANAGER accounts. The existing authentication initializer can create a demo CUSTOMER account if it is absent on a future auth-service startup.

## 5. Verification queries and results

The database identity and schema check returned `rr_jaggery_db` and **8** expected service schemas. `pg_isready -U postgres -d rr_jaggery_db` returned `/var/run/postgresql:5432 - accepting connections`.

The per-table count snapshot was generated and executed against PostgreSQL using:

```sql
SELECT format(
    'SELECT %L, count(*) FROM %I.%I;',
    table_schema || '.' || table_name,
    table_schema,
    table_name
)
FROM information_schema.tables
WHERE table_type = 'BASE TABLE'
  AND table_schema IN (
      'auth_schema', 'commerce_schema', 'customer_schema', 'inventory_schema',
      'procurement_schema', 'production_schema', 'finance_schema', 'notification_schema'
  )
ORDER BY table_schema, table_name;
```

The generated `count(*)` statements returned the post-reset counts in Section 3. A separate PostgreSQL aggregate over representative transaction tables returned **0** total remaining transaction rows. Auth verification returned 2 users, `ADMIN:1,MANAGER:1`, zero legacy-role assignments, and zero CUSTOMER assignments. No database data was inferred from frontend rendering.

## 6. Application verification

Application containers were started without rebuilding images. After reset, GET requests through the Nginx gateway returned HTTP 200 for each health route:

| Service | Gateway health route | Result |
|---|---|---:|
| Auth | `/api/v1/auth/health` | 200 |
| Commerce | `/api/v1/commerce/health` | 200 |
| Customer ledger | `/api/v1/customers/health` | 200 |
| Inventory | `/api/v1/inventory/health` | 200 |
| Procurement | `/api/v1/procurement/health` | 200 |
| Production | `/api/v1/production/health` | 200 |
| Finance | `/api/v1/finance/health` | 200 |
| Notification | `/api/v1/notifications/health` | 200 |

The local Compose containers were running at the final status check. Direct host-port GET probes did not respond consistently for seven service ports, although the gateway health routes all returned 200 and gateway-container GET probes to each service succeeded. Treat the gateway as the verified path; direct host-port connectivity remains a deployment/runtime discrepancy.

The initial login probes recorded HTTP 500 for ADMIN and MANAGER. Subsequent diagnosis showed that those requests contained malformed JSON; the corrected gateway login results are documented in Section 9. Customer registration/login was not exercised.

No business transaction was created during verification. No order, payment, stock movement, purchase, production batch, expense, or payroll record was submitted.

## 7. Known clean-baseline limitation

At the time of the original reset, source inspection identified startup seeders:

- Commerce inserted demo categories/products when its category table was empty.
- Customer ledger inserted sample customers and a demo offline order when its customer table was empty.
- Auth inserted a demo CUSTOMER account when that account was absent, as well as default internal accounts.

Those seeders were controlled in the follow-up baseline repair documented in Section 9. Normal service startup no longer inserts demo business data; the commerce/customer demo seeders require explicit opt-in. Their H2 test configuration opts in explicitly.

Existing Finance/Production master rows were preserved, but their provenance and business approval remain unknown. No prior payable/stock/production figures were reinserted.

## 8. Original reset change and phase boundary

During the original reset phase, only this reset report was added; no application source, tests, schema definitions, or Git history were changed. The follow-up repair in Section 9 made only the narrowly scoped auth/seeder/error-response and test changes. The PostgreSQL records were reset only in the verified local Compose development database. No production database/server was targeted or modified.

**Sprint 8 remains NOT STARTED / NOT AUTHORIZED.**

## 9. Post-Reset Baseline Repair

### Login diagnosis and root cause

The original HTTP 500 responses were reproduced with the earlier PowerShell `curl.exe --data-binary` probe. Sanitized auth-service logs showed `HttpMessageNotReadableException` caused by Jackson `JsonParseException`: the request parser encountered an unquoted JSON field name before `AuthController` or `AuthService` was invoked. PowerShell's native argument handling had stripped the JSON key quotes in that probe. Therefore the original 500 was not caused by the retained account records, BCrypt verification, role resolution, or JWT creation.

The generic exception handler had mapped this malformed-body exception to HTTP 500. A targeted handler now returns HTTP 400 with a generic invalid-request response for unreadable/malformed request bodies. A valid JSON request to the same Nginx gateway login route authenticated both retained accounts successfully.

### Authentication verification

Post-restart PostgreSQL checks established:

- ADMIN account exists and is enabled: **YES**; exactly one `ADMIN` role.
- MANAGER account exists and is enabled: **YES**; exactly one `MANAGER` role.
- Both stored password hashes match the configured BCrypt hash format: **YES** (2 of 2; hash values were not displayed).
- User-to-role foreign-key orphans: **0**.
- Duplicate user/role pairs: **0** (the table key also enforces pair uniqueness).
- CUSTOMER, `PRODUCTION_MANAGER`, `EMPLOYEE`, and `REGISTERED_WHOLESALE` role assignments: **0**.
- The existing accounts were retained; their passwords were not reset.

Valid gateway login verification after rebuilding/restarting:

| Check | Result |
|---|---|
| ADMIN login | **PASS — HTTP 200** |
| ADMIN JWT role claim | **PASS — exactly `ROLE_ADMIN`** |
| MANAGER login | **PASS — HTTP 200** |
| MANAGER JWT role claim | **PASS — exactly `ROLE_MANAGER`** |
| Token signature/authentication | **PASS** — both tokens authorized read-only protected requests |
| ADMIN operational inventory authorization | **PASS — HTTP 200** |
| ADMIN-only user-management authorization | **PASS — HTTP 200** |
| MANAGER operational inventory authorization | **PASS — HTTP 200** |
| MANAGER denied ADMIN-only user-management route | **PASS — HTTP 403** |
| Malformed JSON login request | **PASS — HTTP 400** |

JWTs and response bodies were held only for local validation and were not recorded. Customer registration was not exercised because it would create a new portal account.

### Startup seeder audit and control

A repository-wide search for Java `CommandLineRunner`/`ApplicationRunner` and lifecycle hooks, SQL inserts and seed scripts found three actual service startup runners. `init-schemas.sql` creates extensions/schemas/structural tables and contains no data inserts. `generate_sprint1.js` contains historical generated-source templates but is not wired into Compose/service startup and was not executed.

| Runner | Prior behavior / class | Post-repair behavior |
|---|---|---|
| Auth `DataInitializer` | Mixed **A (system initialization)** with **B (demo/default account seeds)**: created default ADMIN/MANAGER and demo CUSTOMER accounts, then migrated/disabled legacy users. | Account creation removed; runner retains only legacy-account migration/disable behavior (**A**). No default password is embedded in the runtime initializer, and it does not create or reset accounts. |
| Commerce `CommerceDataInitializer` | **B/C**: inserted sample catalogue categories and products, i.e. demo data in business catalogue/master tables, when empty. | Bean exists only when `app.demo-data.seed-enabled=true`; unset/default behavior is disabled. |
| Customer-ledger `DataInitializer` | **B/D**: inserted sample customer records and created a demo offline order/ledger transaction when empty. | Bean exists only when `app.demo-data.seed-enabled=true`; unset/default behavior is disabled. |

Normal local/Compose startup had no `APP_DEMO_DATA_SEED_ENABLED` override; both conditional seeders were absent from the default runtime. The property can be explicitly opted into with `app.demo-data.seed-enabled=true` (or the equivalent relaxed-binding environment variable) for a disposable development dataset. The commerce and customer-ledger H2 test configurations opt in explicitly so their existing fixtures remain available. Auth does not seed a demo CUSTOMER or bootstrap accounts at startup.

### Source files changed

- `services/common-library/src/main/java/com/rrjaggery/common/exception/GlobalExceptionHandler.java` — map unreadable/malformed request bodies to HTTP 400 instead of generic HTTP 500.
- `services/auth-service/src/main/java/com/rrjaggery/auth/config/DataInitializer.java` — remove implicit default Admin/Manager/demo-Customer creation; retain only role migration and disabling of legacy/wholesale logins.
- `services/commerce-service/src/main/java/com/rrjaggery/commerce/config/CommerceDataInitializer.java` — gate demo catalogue seeding behind explicit opt-in.
- `services/customer-ledger-service/src/main/java/com/rrjaggery/customerledger/config/DataInitializer.java` — gate demo customer/order seeding behind explicit opt-in.
- `services/auth-service/src/test/java/com/rrjaggery/auth/controller/AuthControllerTest.java` — create isolated test-only encoded accounts and cover malformed JSON.
- `services/auth-service/src/test/java/com/rrjaggery/auth/config/DataInitializerTest.java` — assert an empty auth repository is not populated by startup initialization.
- `services/commerce-service/src/test/resources/application.yml` and `services/customer-ledger-service/src/test/resources/application.yml` — opt into sample fixtures for H2 tests only.
- `docs/PRE_SPRINT_8_DATABASE_RESET.md` — append this repair record while retaining the original reset evidence.

### Restart and post-restart database verification

The auth, commerce, and customer-ledger services were stopped, rebuilt from the updated working tree, and restarted; PostgreSQL was left running and its data was not cleared again. The other five services remained running. Spring startup-completion markers were present for all three rebuilt services; no demo-seed log markers appeared.

All eight Nginx gateway health endpoints returned HTTP 200 after the restart: auth, commerce, customer ledger, inventory, procurement, production, finance, and notification.

After restart, PostgreSQL again returned:

- `auth_schema.users = 2`; `auth_schema.user_roles = 2` (`ADMIN:1`, `MANAGER:1`).
- `commerce_schema.categories`, `products`, `cart_items`, `orders`, `order_items`, and `product_reviews` = **0**.
- `customer_schema.customers`, `offline_orders`, `offline_order_items`, `customer_ledger`, and `customer_payments` = **0**.
- `inventory_schema.inventory_items` and `stock_movements` = **0**.
- All procurement suppliers, purchase orders/lines, goods receipts, and supplier ledger entries = **0**.
- All production recipes, batches, consumptions, outputs, and wastage = **0**.
- Finance employees, attendance, expenses, salary advances/deductions/payments, employee ledger, and payroll records = **0**.
- Notifications = **0**.
- Preserved Finance configuration remains: 4 advance types, 4 deduction types, 11 expense categories, 10 payment methods, and 1 payroll rule. Preserved Production costing masters remain 9. These were not seeded by this repair.
- PostgreSQL still reports the same eight schemas. No demo CUSTOMER or legacy-role assignment reappeared.

### Tests and remaining limitations

Command executed:

```text
mvn -f services/pom.xml -pl auth-service,commerce-service,customer-ledger-service -am test
```

**31 tests passed; 0 failures, 0 errors, 0 skipped** across common-library prerequisites, auth (10), commerce (10), and customer-ledger (11). The updated auth/commerce/customer-ledger artifacts were then packaged and their three Compose images rebuilt successfully.

The retained ADMIN/MANAGER credentials were not changed. This repair removes implicit auth account creation; account recovery/provisioning must use an authorized process rather than a source-coded default. Existing Finance/Production business-master rows remain and their business approval is still unknown. Other previously documented product/security/runtime findings were not addressed.

**Sprint 8 remains NOT STARTED / NOT AUTHORIZED.** No forms, UI redesign, Investment, Expense redesign, or new business transactions were implemented or created.
