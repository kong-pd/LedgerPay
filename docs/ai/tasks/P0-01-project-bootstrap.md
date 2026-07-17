## P0-01 — Project bootstrap: runnable skeleton

**Phase:** 0 · **Prereq cards:** none · **New concepts (teach first):** Spring Initializr & starters · `application.yml` + profiles · Actuator health · Flyway baseline · what Testcontainers does
**Context to load:** `CLAUDE.md`, ADR-0001/0002/0003, `ROADMAP.md` Phase 0

### Goal
A cloneable repo where `docker compose up` yields app + MySQL with a passing health check, Flyway baseline applied, and CI green.

### In scope
- Spring Initializr project (Maven, Java 21, Boot 3.x): `web`, `validation`, `data-jpa`, `actuator`, `flyway`, `mysql` driver
- Package layout `com.ledgerpay.{payments,ledger,compliance,common}`, each with `package-info.java` carrying its one-line responsibility from `docs/architecture.md`
- `docker-compose.yml`: `mysql:8` + app service; also document running the app from the IDE against compose MySQL as the everyday dev loop
- Config via env vars; `.env.example` committed; `.env` gitignored
- Flyway `V1__baseline.sql` (may create a trivial `schema_marker` table)
- `/actuator/health` exposed
- Tests (Testcontainers MySQL via `@ServiceConnection`): context-loads + health endpoint
- GitHub Actions: `mvn -B verify` on push/PR

### Out of scope
Any business entity/endpoint · Spring Security · Lombok · React · deployment.

### Test specs
- [x] `contextLoads`: application context starts against a Testcontainers `mysql:8` container
- [x] `GET /actuator/health` → 200 with status `UP` (MockMvc/WebTestClient)

### Steps sketch
1. Concept mini-course (list above) → owner restates each
2. Generate project, commit the raw skeleton
3. Compose + env wiring → health UP locally
4. Flyway baseline migration
5. Testcontainers tests
6. GitHub Actions workflow
7. README quickstart section (clone → env → compose → curl health)

### Acceptance criteria
- [x] Fresh clone + `.env` from example + `docker compose up` → health returns UP (exact curl command shown in README)
- [x] `mvn -B verify` green locally (with Docker running) and in CI
- [x] No secrets anywhere in git history; GitHub push protection enabled (owner action)
- [x] Four module packages exist with responsibilities

### Handback checklist
- [x] `CLAUDE.md` §6 → Phase 0 done, next card P1-01
- [x] `ROADMAP.md` Phase 0 ticked
- [x] Deviations → Icebox or draft ADR (none requiring either)
- [x] Adversarial self-review vs `CLAUDE.md` §4 (especially #8: secrets)
