## P2-02 — Posting service, balances & trial balance

**Phase:** 2 · **Prereq cards:** P2-01 · **New concepts (teach first):** `@Transactional` & rollback semantics · why the DB can't enforce "entry balances" declaratively
**Context to load:** `CLAUDE.md`, `docs/design/ledger-spec.md` §2, §4–5 (read the MVCC box even though locking lands in P2-04), §9–11

### Goal
`LedgerPostingService` is the single gate to the ledger; balances and the trial balance are queryable; the invariant test suite starts existing.

### In scope
- `LedgerPostingService.post(...)` per spec §4 (validation → atomic insert → rollback on any failure)
- `MANUAL_CREDIT` service-level funding + `LedgerFixtures.fund(accountId, amountMinor)` test fixture
- Balance query (derived, spec §5 formula, normal-side aware)
- `GET /api/v1/ledger/accounts/{id}/balance`, `GET .../postings?page=`, `GET /api/v1/ledger/trial-balance` (spec §9)
- Write tests FIRST from specs, get owner approval, then implement (CLAUDE.md §2.3)

### Out of scope
Funds check / locking (P2-04) · idempotency behavior (P2-03) · transfers · payments.

### Test specs
- [ ] **T1** balanced happy path (fund 10000 → both balances correct, exactly 2 postings)
- [ ] **T2** unbalanced / zero / negative / wrong-currency / single-leg all rejected, row counts unchanged
- [ ] **T3** mid-transaction failure → no orphan `journal_entry` header, no postings

### Acceptance criteria
- [ ] Trial-balance endpoint returns per-account totals + global `balanced: true` on seeded data
- [ ] All tests green locally and in CI; T2/T3 demonstrably fail if validation or `@Transactional` is removed (try it once, revert)

### Handback checklist
- [ ] `CLAUDE.md` §6 updated · deviations → Icebox/ADR draft · adversarial self-review vs §4 (esp. #2–#3)
