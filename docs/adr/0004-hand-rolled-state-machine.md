# ADR-0004 — Hand-rolled payment state machine (enum + transition service), not Spring Statemachine

- **Date:** 2026-07-04
- **Status:** Proposed — to be Accepted at Phase 2 entry after the concept-course audit (ledger-spec §12)

## Context

Payments move through a small, fully-known lifecycle (PENDING → AUTHORIZED → CAPTURED / FAILED → REFUNDED). The machine must: reject illegal transitions, log every transition, and atomically post ledger effects on certain transitions. P3 will drive the same machine from Stripe webhook events instead of `/simulate/*` endpoints.

## Options considered

1. **Spring Statemachine** — a full framework: builders, listeners, persisters. Heavyweight for 5 states, hard to debug through, and hides the exact thing the owner is trying to learn.
2. **Status column + scattered `if` checks** — how it goes wrong in real codebases: transitions leak everywhere, no single log, no single guard.
3. **Hand-rolled: `PaymentStatus` enum + a transition table + one `PaymentTransitionService`** — ~100 lines, fully visible, trivially testable, single writer of `payment.status`, transition log and ledger effects in the same transaction.

## Decision

Option 3. The legal-transition table lives in code next to the enum (ledger-spec §7 is the source of truth). The service is the only component allowed to modify `payment.status` (CLAUDE.md §4.5). Trigger names are strings so P2 (`simulate.capture`) and P3 (`stripe.payment_intent.succeeded`) share the mechanism.

## Consequences

- ✅ The state machine is a teachable, interview-explainable artifact rather than framework configuration.
- ✅ P3 becomes "swap the callers", not "rebuild the machine".
- ⚠️ We own the correctness: guarded by tests T8/T9 and the invariant that no other code writes the status column.
- ⚠️ If states ever multiply (disputes, partial captures…), revisit — that is future scope anyway.

## Revisit when

The lifecycle grows beyond ~8 states or needs hierarchical/parallel states — the point where a framework starts paying rent.
