# P1-03 — OpenAPI, Postman, and Phase 1 wrap

**Phase:** 1 · **Prereq cards:** P1-02 · **New concepts:** API contracts explained just in time
**Context to load:** `CLAUDE.md`, `docs/scope.md`, `ROADMAP.md`, ADR-0003

### Goal

Make the Phase 1 REST API discoverable and reproducible, then close the milestone against its Definition of Done.

### In scope

- springdoc OpenAPI UI and JSON
- Committed Postman collection for Customer and Account workflows
- Full Phase 1 integration-suite review
- README usage notes and Phase 1 documentation handback

### Out of scope

- Authentication, frontend work, ledger behavior, and Phase 2 implementation

### Acceptance criteria

- [x] OpenAPI is served and describes all Customer/Account endpoints
- [x] Postman collection runs the happy paths and representative errors
- [ ] Phase 1 Definition of Done is fully verified
- [ ] `mvn -B verify` is green locally and in CI

### Verification evidence

- Local `mvn -B verify`: 18 tests, 0 failures, 0 errors, 0 skipped; executable Spring Boot JAR produced.
- Runtime OpenAPI check: OpenAPI 3.0.1, 4 paths, and all 10 Customer/Account operations; `/swagger-ui.html` redirects to `/swagger-ui/index.html`.
- Newman collection run: 19 requests, 19 test scripts, and 21 assertions with 0 failures.
- GitHub Actions verification and Phase 1 exit handback remain pending until this branch is pushed.
