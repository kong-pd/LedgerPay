# Personas & user stories

Epic-level only. **Acceptance criteria are written by the owner after each phase's concept course** — that is the point (see ROADMAP phase rituals). One worked example at the bottom shows the target format.

## Personas

- **Customer** — end user making payments; must pass KYC before transacting.
- **Operations admin** — watches money movement: transactions, ledger, balances, reconciliation.
- **Compliance analyst** — reviews KYC applications; works suspicious-activity cases.
- **Auditor** — verifies that records are complete and untampered.

## Epics

### Payments & ledger (Phases 2–3)
- As a **customer**, I can pay with a test card and see the payment succeed, fail, or be refunded. _(AC: TBD after P3 course)_
- As an **ops admin**, I can trust that every money movement produced a balanced ledger entry, even under retries and concurrent duplicates. _(AC: TBD after P2 course)_
- As an **ops admin**, I can see yesterday's reconciliation report and any mismatch between Stripe and our ledger. _(AC: TBD after P3 course)_

### Panel & access (Phase 4)
- As an **ops admin**, I can log in and browse transactions with status timelines, drilling into ledger entries and balances. _(AC: TBD after P4 course)_

### Compliance (Phases 5–6)
- As a **customer**, I must complete KYC before I can transact. _(AC: TBD after P5 course)_
- As a **compliance analyst**, I can work a KYC review queue and approve/reject with reason codes, fully audited. _(AC: TBD after P5 course)_
- As a **compliance analyst**, transactions that hit monitoring rules create alerts and cases I can investigate, annotate, assign, and close (false positive / simulated STR escalation). _(AC: TBD after P6 course)_
- As an **auditor**, I can verify the audit chain's integrity in one click and see the immutable action history of any case. _(AC: TBD after P6 course)_

## Worked example — target AC format (Phase 1 story, no new concepts needed)

**As an ops admin, I can create and search customer records so accounts can be opened.**

Acceptance criteria:
- [ ] `POST /api/v1/customers` validates email format and uniqueness → `400` (field errors) / `409` (duplicate); error body follows the global format `{code, message, fieldErrors[]}`
- [ ] `GET /api/v1/customers` supports `page`/`size` (defaults 0/20) and filtering by status
- [ ] Response DTOs never expose JPA entities directly
- [ ] All of the above proven by tests running against Testcontainers MySQL in CI
