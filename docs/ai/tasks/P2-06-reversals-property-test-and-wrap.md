## P2-06 — Reversals, trial-balance property test, demo seeder & phase wrap

**Phase:** 2 · **Prereq cards:** P2-05 · **New concepts (teach first):** append-only corrections · property-style testing with a seeded RNG
**Context to load:** `CLAUDE.md`, `docs/design/ledger-spec.md` §8, §10–12

### Goal
Corrections work the append-only way, the whole kernel survives randomized abuse, demo data exists, and Phase 2 closes cleanly.

### In scope
- `POST /api/v1/journal-entries/{ref}/reverse` per spec §8 (mirrored legs, `reverses_entry_id`, guards; funds-checked; idempotency-wrapped)
- **T11** property test: seeded RNG, ~200 random valid ops → after each batch, global Σdebit == Σcredit and every wallet ≥ 0; print seed on failure
- Finish **T12** (immutability coverage across ledger + transition types)
- Demo seeder (dev profile): a handful of customers/accounts, funded wallets, transfers, one full payment lifecycle, one reversal — so the P4 panel and P6 rules have something to show
- Phase-exit ritual (ROADMAP): owner writes `docs/architecture.md` P2 TBW sections (transaction boundaries, ERD, idempotency, state machine) **in their own words**; owner back-fills Phase 2 DoD in `docs/scope.md` and ticks it; tag `phase-2`; retro paragraph in `docs/notes/`

### Out of scope
Anything Stripe · anything UI · new rule ideas (→ Icebox).

### Test specs
- [ ] **T10** reverse a transfer → balances restored exactly, mirrored legs; second reverse → 409; reversing a reversal → 409
- [ ] **T11** as above, three different seeds in CI
- [ ] Concurrent double-reverse race → `UNIQUE reverses_entry_id` lets exactly one through

### Acceptance criteria
- [ ] `docker compose up` + seeder → trial balance shows a living, balanced ledger
- [ ] All P2 tests green in CI; Phase 2 DoD ticked; tag pushed

### Handback checklist
- [ ] `CLAUDE.md` §6 → Phase 2 complete, next = P3 concept course · adversarial self-review vs §4 (all items)
