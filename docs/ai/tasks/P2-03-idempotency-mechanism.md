## P2-03 — Generic idempotency mechanism

**Phase:** 2 · **Prereq cards:** P2-02 · **New concepts (teach first):** claiming via unique constraint · duplicate-key lock waiting · replay semantics
**Context to load:** `CLAUDE.md`, `docs/design/ledger-spec.md` §6, §9–11

### Goal
A reusable idempotency component any money-moving endpoint can wrap around its use-case, implementing spec §6 exactly (single-transaction, insert-first).

### In scope
- Component (interceptor/service — implementer's choice, justify in one paragraph) implementing: required-header check → raw-body SHA-256 → insert-first claim → replay / reuse-conflict / execute paths → response snapshot on `SUCCEEDED` / `FAILED_BUSINESS`
- `Idempotency-Replay: true` header on replays
- Error codes: `IDEMPOTENCY_KEY_REQUIRED` (400), `IDEMPOTENCY_KEY_REUSE` (422)
- A trivial internal test endpoint (or direct service harness) to exercise it before real endpoints exist
- Tests FIRST (CLAUDE.md §2.3)

### Out of scope
TTL/cleanup (registered simplification, spec §12) · principal-scoped keys (P4 migration note) · P3 claim-then-execute variant.

### Test specs
- [ ] **T4** 10 concurrent threads, same key + byte-identical body → exactly 1 execution, 1 record, identical responses, ≥9 replays flagged
- [ ] **T5** same key, different body → 422, no side effects
- [ ] Business failure (4xx) is stored as `FAILED_BUSINESS` and replayed identically on retry

### Acceptance criteria
- [ ] All tests green locally and in CI
- [ ] Owner can explain (recorded in one paragraph in `docs/notes/`) why a crashed request leaves no `IN_PROGRESS` residue in this design — and why P3 breaks that

### Handback checklist
- [ ] `CLAUDE.md` §6 updated · deviations → Icebox/ADR draft · adversarial self-review vs §4 (esp. #4)
