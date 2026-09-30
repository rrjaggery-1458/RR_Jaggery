# Pre-Sprint 8 Git Worktree Classification

## Scope and safety

This is a read-only classification. No files were staged, committed, pushed,
reset, discarded, stashed, or overwritten. `auth_token.txt` was not opened or
included in any candidate list. The root `.gitignore` excludes that path.

At inventory time on branch `main`, Git reported 41 changed tracked files and
134 expanded untracked files (175 entries with
`--untracked-files=all`; the ordinary collapsed status showed 100 entries).
The index was empty. The collapsed count was 99 before the integration report
was added. This classification document itself adds one more untracked file.

The classifications distinguish identifiable project work from provenance
that cannot be established from status and paths alone. Classification is not
approval that a feature is correct or ready to ship.

## 1. Required/current project implementation

These paths contain active application implementation, tests, or schema
support rather than disposable build output. They appear to represent current
RR Jaggery features, but remain uncommitted and need scope/ownership approval
before staging:

- Configurable forms and persistence:
  - `services/common-library/src/main/java/com/rrjaggery/common/forms/**`
  - `services/common-library/src/main/java/com/rrjaggery/common/exception/GlobalExceptionHandler.java`
  - `services/common-library/pom.xml`
  - `services/db/migrations/V20260930_01__configurable_forms.sql`
  - `services/customer-ledger-service/src/main/java/com/rrjaggery/customerledger/CustomerLedgerServiceApplication.java`
  - `services/customer-ledger-service/src/main/java/com/rrjaggery/customerledger/config/DataInitializer.java`
  - `services/customer-ledger-service/src/main/java/com/rrjaggery/customerledger/service/CustomerFormRecordValidator.java`
  - customer-ledger main/test `application.yml` files
  - `services/finance-service/src/**`, `services/finance-service/pom.xml`
  - `services/production-service/src/main/java/com/rrjaggery/production/service/ProductionFormRecordValidator.java`
- Commerce reviews and image handling:
  - modified commerce initializer, security config, admin-order and public-catalogue controllers, and main/test configuration
  - `services/commerce-service/src/main/java/com/rrjaggery/commerce/controller/ImageUploadController.java`
  - `services/commerce-service/src/main/java/com/rrjaggery/commerce/dto/CreateReviewRequest.java`
  - `services/commerce-service/src/main/java/com/rrjaggery/commerce/dto/ProductReviewDto.java`
  - `services/commerce-service/src/main/java/com/rrjaggery/commerce/entity/ProductReview.java`
  - `services/commerce-service/src/main/java/com/rrjaggery/commerce/repository/ProductReviewRepository.java`
  - `services/commerce-service/src/main/java/com/rrjaggery/commerce/service/ImageStorageService.java`
  - `services/commerce-service/src/main/java/com/rrjaggery/commerce/service/ReviewService.java`
- Other service features:
  - `services/inventory-service/src/main/java/com/rrjaggery/inventory/service/InventoryService.java`
  - modified procurement DTO, entity, and service
  - modified production application, controller, batch DTO, repository, service, and main/test configuration
  - `services/production-service/src/main/java/com/rrjaggery/production/controller/ProductionCostMasterController.java`
  - `services/production-service/src/main/java/com/rrjaggery/production/dto/BatchCostingDto.java`
  - `services/production-service/src/main/java/com/rrjaggery/production/entity/ProductionCostMaster.java`
  - `services/production-service/src/main/java/com/rrjaggery/production/repository/ProductionCostMasterRepository.java`
  - `services/production-service/src/main/java/com/rrjaggery/production/service/ProductionCostingService.java`
  - modified notification test plus `services/notification-service/pom.xml` and `src/main/resources/application.yml`
  - notification controller, DTOs, model, repository, and service under `services/notification-service/src/main/java/com/rrjaggery/notification/**`
- `infrastructure/docker/postgres/init-schemas.sql`

## 2. Current-task changes

The following changes are directly attributable to the accepted Docker
integration verification and this worktree review:

- `.gitignore` — one root ignore rule for `auth_token.txt`; the token file
  itself is not a candidate.
- `frontend/src/CustomerLedger.tsx` — gateway-aware API base selection.
- `PRE_SPRINT_8_DOCKER_GIT_INTEGRATION_REPORT.md` — verification report.
- `PRE_SPRINT_8_GIT_WORKTREE_CLASSIFICATION.md` — this classification.

`frontend/src/App.tsx` includes gateway-routing changes, but its complete
diff is much broader (919 insertions and 224 deletions) and contains unrelated
UI/product changes. Only the gateway-routing hunks can be attributed to this
task; the whole file is not safe to stage as a single path.

## 3. Legitimate pre-existing project changes

These are identifiable as earlier project work from the preceding task
history, not changes made by this Git review:

- First-Admin bootstrap implementation:
  - `services/auth-service/src/main/java/com/rrjaggery/auth/config/DataInitializer.java`
  - `services/auth-service/src/main/java/com/rrjaggery/auth/repository/UserRepository.java`
  - `services/auth-service/src/main/java/com/rrjaggery/auth/config/BootstrapAdminLock.java`
  - `services/auth-service/src/main/java/com/rrjaggery/auth/config/FirstAdminBootstrapRunner.java`
  - `services/auth-service/src/main/java/com/rrjaggery/auth/service/FirstAdminBootstrapService.java`
  - `services/auth-service/src/test/java/com/rrjaggery/auth/controller/AuthControllerTest.java`
  - `services/auth-service/src/test/java/com/rrjaggery/auth/config/DataInitializerTest.java`
  - `services/auth-service/src/test/java/com/rrjaggery/auth/service/FirstAdminBootstrapServiceTest.java`
  - `services/auth-service/src/test/resources/application.yml`
  - the bootstrap environment-variable additions in `docker-compose.yml`
  - `PRE_SPRINT_8_FIRST_ADMIN_BOOTSTRAP.md`
- Earlier accepted/pre-Sprint documentation:
  - `docs/PRE_SPRINT_8_CONFIGURABLE_FORM_ARCHITECTURE.md`
  - `docs/PRE_SPRINT_8_DATABASE_RESET.md`
  - `docs/SPRINT6_COSTING_RULES.md`
  - `docs/SPRINT7_REPORTING_ARCHITECTURE.md`

These files are not part of the current integration-report commit candidate
set. Their presence and prior validation do not imply approval to include them
in a future commit.

## 4. Generated/local or verification artifacts

These paths appear to be local scratch, backup, or one-off verification
material and are not safe product-commit candidates without an explicit
retention decision:

- `frontend/src/ManagerPortal.tsx.backup`
- `frontend/verify.js`
- `scratch/pre_sprint6_full_acceptance_audit.ps1`
- `scratch/pre_sprint6_verify.ps1`
- `scratch/sprint7_integrated_acceptance.ps1`
- `scratch/test_image_persistence.ps1`
- `scratch/test_jaggery.png`
- `scratch/verify_backend_stabilization.ps1`
- `scratch/verify_sprint6_final_gate.ps1`

No build output or compiled artifacts were included in the expanded status
inventory.

## 5. Sensitive files

- `auth_token.txt` is sensitive by name and is ignored by the root
  `.gitignore`. Its contents were not inspected. It is not staged and must
  never be added to a commit.

No other credential-bearing path was positively identified from the
worktree inventory. This statement is not a content scan of files.

## 6. Uncertain files and changes

The following are not safe candidates until their origin, intended retention,
and contents are confirmed by the owner:

- `frontend/src/App.tsx` as a whole: mixed gateway work and much larger
  unrelated UI/product changes.
- `frontend/src/ManagerPortal.tsx`: application source, but its provenance and
  scope are uncertain.
- `CHANGELOG.md`, `frontend/src/index.css`, `infrastructure/nginx/nginx.conf`,
  and the application changes listed under category 1: active project
  implementation is apparent, but the complete intended commit scope is not.
- `RR Jaggery Logo.png`: image asset; no ownership or usage confirmation was
  established during this review.
- One-off/root-level status, audit, or verification documents:
  - `FINAL_ACCURATE_STATUS.md`
  - `FINAL_RUNTIME_VERIFICATION_REPORT.md`
  - `FINAL_STATUS_SUMMARY.txt`
  - `PRE_SPRINT_8_FINAL_STABILIZATION_REPORT.md`
  - `PROJECT_TAKEOVER_ANALYSIS.md`
  - `PROJECT_TAKEOVER_AUDIT.md`
  - `STATUS_SUMMARY.txt`
  - `VERIFICATION_GAP_ANALYSIS.md`
  - `final_stabilization_report.md`
  - `verification_summary.txt`

These files were classified by path/name rather than content and should not
be included based on this review alone. The known integration report and
bootstrap report are separately classified above.

## Safe candidates for a future, separately approved commit

Only these complete paths are currently safe candidates for the narrow
integration-verification change:

1. `.gitignore` — only the root ignore-rule addition; do not include
   `auth_token.txt`.
2. `frontend/src/CustomerLedger.tsx` — its complete diff is the gateway-aware
   API-base change.
3. `PRE_SPRINT_8_DOCKER_GIT_INTEGRATION_REPORT.md`.
4. `PRE_SPRINT_8_GIT_WORKTREE_CLASSIFICATION.md`.

The gateway-routing changes in `frontend/src/App.tsx` are a candidate only as
individually isolated hunks after an owner-approved patch review; the entire
file is explicitly excluded. No other path is certified as safe for this
narrow commit scope.

## Readiness

There are no staged changes. No Git mutation was performed. The identifiable
current-task candidate set is isolated, while all other work remains
unstaged and untouched.

**FINAL STATUS: READY FOR GIT STAGING REVIEW**
