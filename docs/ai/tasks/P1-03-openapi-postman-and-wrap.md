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

- [ ] OpenAPI is served and describes all Customer/Account endpoints
- [ ] Postman collection runs the happy paths and representative errors
- [ ] Phase 1 Definition of Done is fully verified
- [ ] `mvn -B verify` is green locally and in CI

