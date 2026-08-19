# P1-01 — Customer CRUD foundation

**Phase:** 1 · **Prereq cards:** P0-01 · **New concepts:** explained just in time; architecture-first workflow
**Context to load:** `CLAUDE.md`, `docs/scope.md`, `docs/stories.md`, `docs/architecture.md`, ADR-0001, ADR-0003, ADR-0006

### Goal

Deliver a complete Customer REST vertical slice in the `payments` module with persistence, DTO separation, validation, pagination, and a consistent error contract.

### In scope

- Flyway migration for `customer`
- Customer entity, status, repository, service, request/response DTOs, and controller
- `POST`, `GET by id`, paginated/filterable `GET collection`, `PUT`, and `DELETE`
- Normalized unique email handling
- Global error format `{code, message, fieldErrors[]}`
- MySQL Testcontainers integration tests through MockMvc

### Out of scope

- Account CRUD, ledger-account provisioning, KYC, authentication, OpenAPI, and Postman
- New modules or libraries

### Test specs

- [x] Valid create returns `201`, a `Location` header, and a response DTO
- [x] Invalid email returns `400` with a field error
- [x] Duplicate normalized email returns `409`
- [x] Missing customer returns `404`
- [x] Collection endpoint paginates and filters by status
- [x] Update changes allowed fields; delete returns `204` and subsequent read returns `404`
- [x] Tests run against Testcontainers MySQL, never H2

### Acceptance criteria

- [x] Customer endpoints are rooted at `/api/v1/customers`
- [x] Controllers never expose JPA entities
- [x] Schema is created only through Flyway and `ddl-auto=validate` remains enabled
- [x] `mvn -B verify` is green locally
- [ ] GitHub Actions is green for the P1-01 commit

### Handback checklist

- [x] `CLAUDE.md` §6 updated
- [x] Next card set to P1-02 after P1-01 local verification
- [x] No `CLAUDE.md` §4 invariant is weakened

