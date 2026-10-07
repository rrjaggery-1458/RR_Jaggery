# Pre-Sprint 8 First-Admin Bootstrap

## Purpose

A fresh database has no administrator, and public registration intentionally creates customers only. The auth service now supports securely provisioning the first administrator during startup without adding a public Admin-registration endpoint or seeding business data.

## Startup behavior

The auth service runs the bootstrap after the existing legacy-account migration:

- It counts enabled users with the `ADMIN` role.
- If at least one enabled Admin exists, it skips bootstrap without changing any account or password. Credentials may be omitted in this case.
- If none exists, it requires both configured credentials, validates the username as an email address, and requires a password of at least 12 characters. Missing or invalid configuration fails startup with a configuration-only error.
- On success, it creates exactly one enabled user with `ADMIN` / `ROLE_ADMIN` authority and `INTERNAL` customer type. The password is encoded by the existing Spring Security `PasswordEncoder` before persistence.
- Disabled historical Admins are neither enabled nor modified. If the configured email conflicts with a disabled account, persistence fails safely rather than reactivating that account.
- No Manager, Customer, or business records are created.

Bootstrap is transactional. PostgreSQL transaction-scoped advisory locking serializes concurrent bootstrap attempts across service instances; non-PostgreSQL tests use a process-local lock.

## Configuration and Docker

Set these environment variables only in a protected deployment environment or a local, untracked `.env` file:

- `BOOTSTRAP_ADMIN_USERNAME`
- `BOOTSTRAP_ADMIN_PASSWORD`

Compose passes these variables only to `auth-service`. The repository's `.gitignore` excludes `.env` and related local environment files. Do not add real values to Compose, source, documentation, or version control. Use your deployment platform's secret/environment-variable facility outside local development.

For a fresh Docker database, supply both values through that protected environment before starting the Compose stack. The auth service then provisions the initial Admin during startup. If no enabled Admin exists and either value is absent, auth-service startup fails closed. Once an enabled Admin exists, the startup bootstrap is skipped and the configured values are not used to alter that account.

## Verification

Validation completed:

- Full auth-service suite, including all focused bootstrap and controller tests: **19 passed**, 0 failures/errors.
- Covered first Admin creation, repeat-start idempotency, pre-existing Admin preservation, disabled Admin handling, missing-credential failure, encoded password, `ROLE_ADMIN` token authority, concurrent bootstrap attempts, no Manager/Customer bootstrap accounts, Customer-only public registration, and Admin-only Manager creation.
- `docker-compose.yml` contains variable references only; it does not contain bootstrap credentials.

No live PostgreSQL/Docker startup was performed for this blocker-resolution task.

## Change and safety record

Implementation files:

- `services/auth-service/src/main/java/com/rrjaggery/auth/config/BootstrapAdminLock.java`
- `services/auth-service/src/main/java/com/rrjaggery/auth/config/FirstAdminBootstrapRunner.java`
- `services/auth-service/src/main/java/com/rrjaggery/auth/config/DataInitializer.java`
- `services/auth-service/src/main/java/com/rrjaggery/auth/repository/UserRepository.java`
- `services/auth-service/src/main/java/com/rrjaggery/auth/service/FirstAdminBootstrapService.java`
- `services/auth-service/src/test/java/com/rrjaggery/auth/controller/AuthControllerTest.java`
- `services/auth-service/src/test/java/com/rrjaggery/auth/service/FirstAdminBootstrapServiceTest.java`
- `services/auth-service/src/test/resources/application.yml` (test-only bootstrap fixture)
- `docker-compose.yml`

No database reset was performed. No Docker volumes were deleted. No commit or push was made. No real credentials were added; the test configuration contains only a test-only fixture.

The worktree contained unrelated pre-existing changes, which were left untouched. A pre-existing untracked `auth_token.txt` was not inspected, modified, or included in any change; do not commit it as part of this task.

## Next step

After owner review, execute the previously supplied destructive Docker/database reset prompt. This implementation task does not perform that reset.

## Final status

**READY FOR RESET** — the first-Admin bootstrap implementation and Auth service tests passed. The destructive reset remains a separate next step and was not performed here.
