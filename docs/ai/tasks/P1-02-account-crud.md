# P1-02 — Account CRUD and customer association

**Phase:** 1 · **Prereq cards:** P1-01 · **New concepts:** explained just in time
**Context to load:** `CLAUDE.md`, `docs/scope.md`, `docs/architecture.md`, ADR-0002, ADR-0006, ADR-0007

### Goal

Add Account CRUD owned by `payments`, associated with Customer, while keeping ledger provisioning out of Phase 1.

### In scope

- Flyway migration and JPA model for Account
- Customer-to-Account association by identifier
- Account DTOs, validation, controller/service/repository layers
- Paginated account listing and consistent global errors
- MySQL Testcontainers integration tests

### Out of scope

- Balances, money fields, transfers, `LedgerAccount`, and KYC gating

### Test specs

- [x] Valid create returns `201`, a `Location` header, ACTIVE status, and MYR currency
- [x] Missing Customer returns `404 CUSTOMER_NOT_FOUND`
- [x] Duplicate Account name within one Customer returns `409 ACCOUNT_NAME_CONFLICT`
- [x] Blank Account name returns `400 VALIDATION_FAILED`
- [x] Collection endpoint paginates and filters by Customer and status
- [x] Update changes allowed fields; delete returns `204` and subsequent read returns `404`
- [x] A Customer with Accounts cannot be deleted and returns `409 CUSTOMER_HAS_ACCOUNTS`
- [x] Tests run against Testcontainers MySQL, never H2

### Acceptance criteria

- [x] CRUD endpoints are rooted at `/api/v1/accounts`
- [x] An Account cannot reference a missing Customer
- [x] DTOs do not expose JPA entities
- [x] Schema is created through Flyway V3 and `ddl-auto=validate` remains enabled
- [x] `mvn -B verify` is green locally: 16 tests, 0 failures, 0 errors
- [ ] GitHub Actions is green for the P1-02 commit

### Verification evidence

- Manual Customer/Account CRUD and representative error-contract checks passed
- Flyway history contains successful V1, V2, and V3 migrations
- Local `mvn -B verify` produced the executable Spring Boot JAR on 2026-08-19

### Handback checklist

- [x] ADR-0007 records the balance-free Account decision
- [x] `CLAUDE.md` §5 and §6 updated with the local completion state
- [x] No balance, money movement, LedgerAccount provisioning, or KYC gating was added
- [ ] Commit and push the P1-02 branch; verify GitHub Actions before selecting P1-03
