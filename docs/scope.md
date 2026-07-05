# Scope

## One-line story

> "I built a payments + compliance middle platform: every money movement is backed by an auditable double-entry ledger, and suspicious activity is automatically detected and case-managed."

## Goals — why this project exists

1. Land HK fintech backend interviews: concrete proof of having built a *payment system* and *compliance tooling* — the two archetypes HK JDs name.
2. Learn transferable backend engineering: ledger correctness, idempotency, state machines, webhooks, auditability.
3. Reusable asset for KL internship applications (Oct 2026), HK master's applications, and grad-role interviews.

**Non-goal:** this is not a product/startup. Production traffic, licensing, and commercial concerns are out.

## In scope

- **Ledger core:** Stripe test-mode payments; double-entry ledger; payment state machine; idempotent APIs; webhook reconciliation; refunds; daily reconciliation job.
- **Compliance layer:** KYC onboarding pipeline (fake documents only) with human review queue; DB-configurable monitoring rules → alerts → suspicious-activity cases with workflow (open → investigating → closed as false-positive / escalated as simulated STR); append-only hash-chained audit log + verification endpoint.
- **React admin panel:** transactions, ledger, compliance cases; JWT auth.
- Demo data seeder / transaction simulator; Docker Compose local env; one AWS deployment; CI running tests.

## Out of scope — binding

Real money or live Stripe keys · card data touching our servers · bank/PSP licensing work · building card acquiring/processing internals · multi-currency & FX · production-grade fraud ML · real IDV/KYC vendor integration · microservices split · Kubernetes · mobile apps · multi-tenancy · i18n · HA/scale targets.

## Scope-change rule

New ideas are **never implemented on the spot**. They go into the Icebox with a date. Icebox items may be promoted **only at a phase boundary**, and **never before Checkpoint A**. Rationale: the compliance domain is bottomless; this rule is the project's survival mechanism.

### Icebox

| Date | Idea | Notes |
|---|---|---|
| 2026-07-04 | maker-checker approval for rule changes | candidate stretch goal at Phase 6 |
| 2026-07-04 | external anchoring of audit-chain root hash | discuss in Phase 6 ADR |
| 2026-07-04 | ArchUnit tests to enforce module boundaries | nice-to-have, revisit at Phase 2 |

## Definition of Done per phase

A phase closes only when every box ticks. P0/P1 are pre-filled. **Later phases are blank on purpose** — the owner writes each DoD right after that phase's concept course, as the learning check (see ROADMAP phase rituals).

### Phase 0 — Skeleton
- [ ] Fresh clone + `.env` from example + `docker compose up` → health endpoint returns UP
- [ ] Flyway baseline migration applied automatically
- [ ] GitHub Actions runs `mvn -B verify` on every push — green
- [ ] `.env` gitignored, `.env.example` committed, GitHub push protection enabled
- [ ] Four module packages exist, each with a one-line responsibility in `package-info.java`

### Phase 1 — REST foundations
- [ ] Customer & Account CRUD with bean validation, pagination, and a consistent global error format
- [ ] OpenAPI served via springdoc; Postman collection committed
- [ ] Controller/service tests run against Testcontainers MySQL in CI
- [ ] DTOs fully separated from JPA entities

### Phase 2 — Ledger core
_DoD to be written by owner at phase entry, after the concept course._

### Phase 3 — Stripe + reconciliation
_DoD to be written at phase entry._

### Phase 4 — Panel + auth
_DoD to be written at phase entry._

### Phase 5 — KYC
_DoD to be written at phase entry._

### Phase 6 — Monitoring + cases + audit chain
_DoD to be written at phase entry._

### Phase 7 — Deployment + polish
_DoD to be written at phase entry._
