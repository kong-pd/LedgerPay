## P2-01 — Ledger schema, immutable entities, chart bootstrap & wallet provisioning

**Phase:** 2 · **Prereq cards:** P1 complete + P2 concept course + spec audit done (ledger-spec §12) · **New concepts (teach first):** chart of accounts · normal side · JPA `@Immutable` · enums as `STRING`
**Context to load:** `CLAUDE.md`, `docs/design/ledger-spec.md` §1–3, ADR-0002/0005

### Goal
The ledger's data layer exists: migrations V2–V4 applied, immutable entities mapped, `PLATFORM_CASH` seeded, and every product `Account` (existing and future) owns exactly one `WALLET-{id}` ledger account.

### In scope
- Flyway `V2__ledger_core.sql`, `V3__idempotency.sql`, `V4__payments.sql` per spec §3 (syntax may adjust; **semantics may not**)
- Entities: `LedgerAccount`, `JournalEntry` + `Posting` (`@Immutable`, no setters), `IdempotencyRecord`, `Payment`, `PaymentTransition`; repositories expose no update/delete for immutable types
- Seed `PLATFORM_CASH` (ASSET, MYR) idempotently on startup or via migration
- Provisioning: creating an `Account` (P1 flow) also creates its `LedgerAccount` in the same transaction; one-off backfill for pre-existing dev accounts

### Out of scope
Posting logic · balances · any new HTTP endpoint · idempotency behavior (table only).

### Test specs
- [ ] Migrations apply cleanly on a fresh Testcontainers MySQL; `PLATFORM_CASH` exists exactly once even after restart
- [ ] Creating an `Account` yields a `WALLET-{id}` LIABILITY ledger account (MYR); backfill covers old rows
- [ ] T12 (part): mutate-and-flush on a loaded `Posting` issues no UPDATE; ledger repositories have no delete/update methods

### Acceptance criteria
- [ ] `mvn -B verify` green locally and in CI
- [ ] `docs/architecture.md` ERD stub updated by owner (tables + relations, hand-drawn Mermaid is fine)

### Handback checklist
- [ ] `CLAUDE.md` §6 updated · deviations → Icebox/ADR draft · adversarial self-review vs §4 (esp. #1–#3)
