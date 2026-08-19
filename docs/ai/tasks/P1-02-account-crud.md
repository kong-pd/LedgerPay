# P1-02 — Account CRUD and customer association

**Phase:** 1 · **Prereq cards:** P1-01 · **New concepts:** explained just in time
**Context to load:** `CLAUDE.md`, `docs/scope.md`, `docs/architecture.md`, ADR-0002, ADR-0006

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

### Acceptance criteria

- [ ] CRUD endpoints are rooted at `/api/v1/accounts`
- [ ] An account cannot reference a missing customer
- [ ] DTOs do not expose JPA entities
- [ ] `mvn -B verify` is green locally and in CI

