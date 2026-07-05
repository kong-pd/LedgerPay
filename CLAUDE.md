# CLAUDE.md — AI Collaboration Context Pack

This repo is built by a single owner working with AI assistants across many **separate** chat sessions. Sessions share no memory — **this repository is the memory.** Paste this file (plus one task card) at the start of every session, or let tooling auto-read it.

## 1. Project snapshot

- **LedgerPay** = payments + compliance middle platform on Stripe **test mode**. Two layers:
  - **Ledger core:** double-entry ledger, payment state machine, idempotent APIs, webhook reconciliation, refunds.
  - **Compliance layer (the flagship depth):** KYC onboarding pipeline, transaction-monitoring rules → alerts → suspicious-activity cases with workflow, append-only hash-chained audit log.
- **Stack:** Java 21 · Spring Boot 3.x · Maven · Spring Data JPA · MySQL 8 · Flyway · JUnit 5 + Testcontainers · React + Vite + TS + Ant Design (Phase 4+) · Docker Compose · AWS (form TBD).
- **Architecture:** modular monolith with four modules — `payments`, `ledger`, `compliance`, `common` (ADR-0001).
- Work is defined by **task cards** in `docs/ai/tasks/`. The plan lives in `ROADMAP.md`.

## 2. Session protocol

1. Load this file + the current task card + everything the card lists under "Context to load".
2. Work **only** within the card's scope. If a needed decision is not covered by an existing ADR: **stop, present options with a recommendation as a draft ADR. Never decide silently.**
3. For correctness-critical code (ledger posting, idempotency, state transitions, webhooks, audit chain): **write the tests from the card's test specs first**, get owner approval, then implement.
4. The owner runs all code and tests locally. "It should work" is not done — ticking the card's acceptance criteria is.
5. End of session: update §6 *Current state* (or tell the owner exactly what to write), tick progress in `ROADMAP.md`.

## 3. Working with the owner

- Conversation in **Chinese**; all artifacts (code, comments, commits, docs) in **English**.
- The owner is deliberately learning this domain. Before using any concept the card marks as *new*: explain it in plain words → owner restates it → then code. Syllabus and self-checks: `docs/ai/curriculum.md`. Unexplained magic is a bug.
- Prefer boring, readable solutions. No new libraries beyond ADR-0003 without an ADR.
- Push back when the owner (or a previous session) proposes something violating §4. Agreeableness about invariant violations is the worst possible failure mode.

## 4. Hard invariants — violating any of these is wrong, regardless of who suggested it

1. **Money is never floating point.** Amounts are `long` minor units end-to-end; field/column names end in `_minor`; single configured currency (**MYR**, minor unit = sen) stored and validated on every money row (ADR-0002).
2. **The ledger is append-only.** Never `UPDATE`/`DELETE` posted ledger rows. Corrections are reversal entries.
3. **Every journal entry balances** — Σdebits == Σcredits — and all legs commit **atomically in one DB transaction**. An unbalanced write must be *impossible* (constraint/invariant level), not merely avoided.
4. **Every money-moving or state-changing API is idempotent**: `Idempotency-Key` header + unique constraint + stored response. Concurrent duplicates produce exactly one effect.
5. **State transitions go through the transition service only.** No direct status-column writes anywhere; illegal transitions throw and are logged.
6. **Webhook handlers verify signatures and are idempotent + order-tolerant.** Assume duplicate and out-of-order delivery.
7. **Audit tables are append-only**; every KYC/case/rule action emits an audit event into the hash chain.
8. **No real money, no real PII, no card data on our servers** (Stripe-hosted fields only), test-mode keys only, secrets never committed.

## 5. Decision index (ADRs are binding)

| ADR | Decision |
|---|---|
| [0001](docs/adr/0001-modular-monolith.md) | Modular monolith, not microservices |
| [0002](docs/adr/0002-money-as-integer-minor-units.md) | Money as `long` minor units, single currency |
| [0003](docs/adr/0003-backend-stack.md) | Java 21 / Spring Boot 3 / Maven / JPA / MySQL 8 / Flyway / Testcontainers; no Lombok for now |
| [0004](docs/adr/0004-hand-rolled-state-machine.md) | **Proposed** — hand-rolled payment state machine (accept at P2 entry after spec audit) |
| [0005](docs/adr/0005-derived-balances-with-row-locks.md) | **Proposed** — derived balances + `FOR UPDATE` row locks + `READ_COMMITTED` (accept at P2 entry) |
| — | Expected future ADRs: P3 integration decisions (0006+, from `docs/design/p3-stripe-pitfalls-and-tests.md` §1 at P3 entry), frontend framework confirmation (P4), P5/P6 compliance decisions (rule engine, evaluation timing, audit chain — from `docs/design/p5-p6-compliance-sketch.md` at phase entry), AWS deployment form (P7) |

## 6. Current state — update every session

- **Phase:** 0 — not started (documentation skeleton only)
- **Done:** docs skeleton; default currency decided → **MYR** (ADR-0002); **P2 design spec drafted** — `docs/design/ledger-spec.md`, status DRAFT, must be audited at P2 entry (§12) before implementation; P2 task cards P2-01…P2-06 drafted; **P3 pitfall register + test specs drafted** — `docs/design/p3-stripe-pitfalls-and-tests.md`, DRAFT, audited at P3 entry (§6); **P5–P6 compliance sketch drafted** — `docs/design/p5-p6-compliance-sketch.md`, DRAFT, audited at P5/P6 entry (§7); system blueprint (`docs/blueprint.md`); session prompt kit incl. literal first-message (`docs/ai/SESSION_PROMPT.md`) (2026-07-04)
- **Next card:** `docs/ai/tasks/P0-01-project-bootstrap.md`
- **Open questions:** none
