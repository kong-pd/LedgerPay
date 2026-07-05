## P2-05 — Payment state machine with simulated triggers

**Phase:** 2 · **Prereq cards:** P2-04 · **New concepts (teach first):** guarded transitions & transition log · "state guard ≠ retry-idempotency" (spec §7 insight box)
**Context to load:** `CLAUDE.md`, `docs/design/ledger-spec.md` §7, §9–11, ADR-0004

### Goal
Payments move PENDING → AUTHORIZED → CAPTURED/FAILED → REFUNDED strictly through `PaymentTransitionService`, with ledger effects posted atomically — driven by `/simulate/*` endpoints that P3 will replace with Stripe events.

### In scope
- `PaymentStatus` enum + legal-transition table in code (spec §7 is the source of truth)
- `PaymentTransitionService`: validate → transition row → status update → ledger effect (capture: DR PLATFORM_CASH / CR wallet; refund: funds-checked DR wallet / CR PLATFORM_CASH) — one transaction; sole writer of `payment.status`
- `POST /api/v1/payments` (create, PENDING) · `POST /api/v1/payments/{ref}/simulate/{authorize|capture|fail|refund}` · `GET /api/v1/payments/{ref}` with transition history — all mutating endpoints idempotency-wrapped
- `ILLEGAL_TRANSITION` (409) error path
- Tests FIRST (CLAUDE.md §2.3)

### Out of scope
Stripe anything · partial refunds · webhook thinking (P3) · UI.

### Test specs
- [ ] **T8** capture a PENDING payment → 409, no transition row, no entry, status unchanged
- [ ] **T9** authorize → two concurrent captures, same key → one execution + one replay, exactly one `PAYMENT_CAPTURE` entry, wallet credited once; then refund → REFUNDED, wallet debited, trial balance holds
- [ ] Refund when wallet already spent → 422 `INSUFFICIENT_FUNDS`, status stays CAPTURED (documented simplification)
- [ ] Every status change has exactly one matching `payment_transition` row (I6)

### Acceptance criteria
- [ ] Full lifecycle runnable in Postman: create → authorize → capture → refund, ledger visible at each step via trial balance
- [ ] All tests green locally and in CI

### Handback checklist
- [ ] `CLAUDE.md` §6 updated · deviations → Icebox/ADR draft · adversarial self-review vs §4 (esp. #4–#5, #7)
