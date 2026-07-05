## P2-04 — Internal transfer API with concurrency-safe overdraft protection

**Phase:** 2 · **Prereq cards:** P2-02, P2-03 · **New concepts (teach first):** pessimistic locking (`SELECT … FOR UPDATE`) · MVCC snapshots & isolation levels — **do not start coding until the owner can retell the spec §5 trap from memory** (audit checklist item 4)
**Context to load:** `CLAUDE.md`, `docs/design/ledger-spec.md` §5, §6, §9–11, ADR-0005

### Goal
`POST /api/v1/transfers` moves money between wallets, idempotently, and provably cannot overdraft — even under concurrent attack.

### In scope
- `postWithFundsCheck(...)` guarded path: lock source `ledger_account` row → derived balance → check → post (spec §5)
- `@Transactional(isolation = READ_COMMITTED)` on the transfer use-case, with a code comment linking to spec §5's MVCC box
- Transfer endpoint wrapped in the P2-03 idempotency component; validation (amount > 0, MYR, from ≠ to, accounts exist & wallet-type)
- `INSUFFICIENT_FUNDS` (422) error path
- Tests FIRST (CLAUDE.md §2.3)

### Out of scope
Payments/state machine (P2-05) · destination-account locking (not needed — credits are uncapped; note the ascending-id rule if that ever changes) · fees.

### Test specs
- [ ] **T6** balance 5000, transfer 6000 → 422 `INSUFFICIENT_FUNDS`, nothing persisted
- [ ] **T7** balance 10000; two concurrent 8000 transfers, different keys, barrier-released → exactly one succeeds; final A=2000, B=8000; trial balance holds
- [ ] **T7-regression drill:** temporarily revert to default isolation → demonstrate T7 can fail (may need repetition loop) → restore. Owner watches this happen once; write one sentence about it in `docs/notes/`

### Acceptance criteria
- [ ] End-to-end via Postman: fund (fixture) → transfer → balances + trial balance verify by hand
- [ ] All tests green locally and in CI

### Handback checklist
- [ ] `CLAUDE.md` §6 updated · deviations → Icebox/ADR draft · adversarial self-review vs §4 (esp. #1, #3, #4)
