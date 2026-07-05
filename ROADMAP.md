# ROADMAP

Milestone-based. Week numbers assume ~10 h/week and are estimates (±50%) — trust milestones, not the calendar. **No new scope before Checkpoint A** (see `docs/scope.md`).

| Phase | Weeks* | Focus | New skills | Exit deliverables | Status |
|---|---|---|---|---|---|
| 0 | 1 | Runnable skeleton | Spring Initializr, Compose, CI | `docker compose up` → app + MySQL healthy; Flyway baseline; CI green; secrets hygiene | ☐ |
| 1 | 1–3 | Spring Boot + REST foundations | MVC layering, JPA, DTO/validation, error handling, springdoc, Postman | Customer/Account CRUD + OpenAPI + Postman collection + first tests | ☐ |
| 2 | 3–6 | **Ledger core** | double-entry, ACID/transactions, state machines, idempotency | internal transfer API + invariant & concurrency test suite proving the books always balance | ☐ |
| 3 | 6–9 | Stripe + webhooks + reconciliation | Stripe Checkout, webhook signatures/ordering, scheduled jobs | test-mode payments + refunds + daily reconciliation report | ☐ |
| **A** | ~9 | **Checkpoint A (~end Sep): complete demoable payments + ledger system; soft-deploy on a cheap VM; README GIF + live link ready for internship applications** | — | — | ☐ |
| 4 | 9–11 | React panel v1 + auth | React/TS/AntD, Spring Security + JWT, CORS | transaction list/detail with ledger view + login | ☐ |
| 5 | 11–13 | Compliance I: KYC | KYC/CDD, onboarding state machine | KYC pipeline + review queue + trading gate + audit events | ☐ |
| 6 | 13–16 | Compliance II: monitoring, cases, audit chain | AML typologies, rules, hash chains | configurable rules → alerts → cases; case workflow; verify-chain endpoint; demo seeder | ☐ |
| 7 | 16–18 | AWS + polish | multi-stage Docker, AWS deploy, observability | live deployment + final README/architecture/demo script | ☐ |

\* counted from project start.

## Phase rituals

**Entry:** ① concept course for the phase's new skills (syllabus: `docs/ai/curriculum.md`) → ② owner writes the phase's Definition of Done in `docs/scope.md` — this doubles as the learning check → ③ generate task cards → ④ execute cards, one per session.

**Exit:** DoD all ticked → tag `phase-N` → update `CLAUDE.md` §6 → one paragraph of retro notes in `docs/notes/` (what surprised you — future interview material).
