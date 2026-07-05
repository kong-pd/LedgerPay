# Phase 3 Pitfall Register & Test Specifications — Stripe, Webhooks, Reconciliation

> **Status: DRAFT.** Written 2026-07-04, audited at Phase 3 entry (§6). P3 inherits its design from P2 (the state machine already exists); the danger here is not architecture but a minefield of integration gotchas. This document enumerates the mines. Format: **Trap → Rule → Proof (test)**. Key decisions in §1 are *Proposed*; the audit promotes the big ones to ADR-0006+.

## 1. What changes from P2 — design deltas (Proposed)

**D1 — `capture_method=manual` on PaymentIntents**, so Stripe's lifecycle maps 1:1 onto the P2 machine (`requires_capture` ⇔ AUTHORIZED). Cost: uncaptured authorizations expire (~7 days) and Stripe cancels them — handled by the `canceled` event (W11). *Alternative rejected:* automatic capture collapses PENDING→CAPTURED and makes the machine provider-dependent.

| Stripe event | Transition | Ledger effect (same tx) |
|---|---|---|
| — (our `POST /payments` creates + confirms the PI) | → PENDING | — |
| `payment_intent.amount_capturable_updated` | PENDING → AUTHORIZED | — |
| `payment_intent.payment_failed` | PENDING → FAILED | — |
| `payment_intent.canceled` (incl. auth expiry) | AUTHORIZED → FAILED | — |
| `payment_intent.succeeded` | AUTHORIZED → CAPTURED (forward-jump allowed, D5) | DR STRIPE_CLEARING / CR wallet |
| `charge.refunded` (full only) | CAPTURED → REFUNDED | DR wallet / CR STRIPE_CLEARING |

**D2 — New ledger account `STRIPE_CLEARING` (ASSET):** money Stripe holds for us. All provider=STRIPE effects post here; `PLATFORM_CASH` remains for `MANUAL_CREDIT`/simulated flows. Payouts (clearing → bank) are out of scope → icebox.

**D3 — `webhook_event` table; pipeline = verify → claim → process.** Verify signature first (unverified requests are **never persisted**), claim by `event_id` (UNIQUE), process synchronously, mark `PROCESSED | NEEDS_ATTENTION | FAILED`. Columns: `event_id UNIQUE, type, payload JSON, status, error, received_at, processed_at`.

**D4 — Claim-then-execute idempotency for endpoints that call Stripe.** P2's single-transaction design collapses once an un-rollbackable external call sits inside it. New shape: tx1 claim the key (commit) → call Stripe, **passing our key as Stripe's own `Idempotency-Key`** (`lp-{key}`; Stripe retains keys ~24h) → tx2 finalize + response snapshot. Retry while claimed → `409 IDEMPOTENCY_IN_PROGRESS`. Stuck claims are now possible — deliberately deferred (visible via admin query; TTL reaper → icebox).

**D5 — Endpoints request, webhooks decide.** Our `capture`/`refund` endpoints call Stripe and return **`202 Accepted`** without touching state; state changes happen only when the webhook arrives, through `PaymentTransitionService` (single-writer preserved). Webhook callers get two extra tolerances: *benign-stale* (payment already at or past the target in lifecycle order PENDING < AUTHORIZED < CAPTURED < REFUNDED, FAILED terminal → 200 no-op) and *forward-jump* (PENDING → CAPTURED legal for webhook triggers, logged — collapses a missed intermediate). User/API callers keep strict 409 guards.

**D6 — Stripe SDK behind our own `StripeGateway` interface.** CI and integration tests use a deterministic stub (which is also how recon tests seed divergence). A small manual smoke checklist runs against real test mode. **CI never talks to Stripe.**

**D7 — Reconciliation v1 is read-only.** Nightly job lists PaymentIntents + refunds created in a UTC window (edges overlapped, deduped by id), compares against local payments + ledger, records discrepancies (`MISSING_LOCAL | MISSING_REMOTE | AMOUNT_MISMATCH | STATUS_MISMATCH`) + report endpoint. It never posts entries. Balance-transaction-based recon = documented upgrade path.

## 2. Pitfalls — webhooks & Stripe API

- **W1 · No signature verification.** Anyone can POST "payment succeeded" = free money. Rule: verify `Stripe-Signature` (SDK `constructEvent`) before anything else; invalid/missing → 400, nothing persisted. *Proof: T13.*
- **W2 · Verifying a parsed body.** Signatures cover the **exact raw bytes**; parse-then-reserialize breaks it. In Spring, capture the raw body before Jackson touches it. *Proof: T14.*
- **W3 · Duplicate delivery.** Stripe is at-least-once. Rule: claim by `event_id` UNIQUE — the P2 claiming pattern, reused. Second delivery → 200, zero effect. *Proof: T15.*
- **W4 · Out-of-order delivery.** Never assume order. Benign-stale + forward-jump (D5); a stale event must never crash or double-post. *Proof: T16.*
- **W5 · Two writers racing.** If the API-response path *also* wrote state, it would race the webhook path. Rule: D5's 202 pattern — endpoints request, webhooks decide. *Proof: T24.*
- **W6 · Wrong acknowledgment semantics.** 2xx = "recorded", non-2xx = "please retry". Benign dup/stale → 200. Transient failure after claim → 500 so Stripe redelivers. *Proof: T17.*
- **W7 · External call inside single-tx idempotency.** Never wrap a Stripe call in the P2 mechanism — use D4. *Proof: T20.*
- **W8 · Not propagating idempotency to Stripe.** Crash between the Stripe call and finalize + retry = duplicate PaymentIntent. Our key rides as Stripe's key. *Proof: T20 (stub asserts the same key on both attempts).*
- **W9 · Trusting amounts.** Capture/refund endpoints accept **no amount field** (full-only; the amount lives in our payment row — kills parameter tampering). On `succeeded`, compare event amount + currency against our row; mismatch → `NEEDS_ATTENTION`, **no posting** (this flag is a seed for a P6 rule). *Proof: T18.*
- **W10 · Partial refund from the Stripe dashboard.** `charge.refunded` with `amount_refunded < amount`: policy is full-only → `NEEDS_ATTENTION`, no posting, status stays CAPTURED. *Proof: T19.*
- **W11 · Forgotten authorizations.** Uncaptured auths expire (~7 days); Stripe cancels → `canceled` event drives AUTHORIZED→FAILED (already legal). Demo tip: don't expect week-old demo auths to still be alive. *Proof: transition-mapping unit tests.*
- **W12 · Local-dev signature hell + version drift.** `stripe listen --forward-to` uses a **different signing secret** than dashboard endpoints — configure per profile or signatures "mysteriously" fail locally. Pin the Stripe API version and SDK version. *Setup checklist, not a test.*

## 3. Pitfalls — reconciliation

- **R1 · Auto-healing recon.** Letting recon post "fixes" creates an unreviewed money-creation path that bypasses the posting discipline. Recon records; humans resolve (P6 turns `MISSING_LOCAL` into auto-cases). *Proof: T21/T22 assert the ledger is untouched.*
- **R2 · Window/timezone bugs.** "Yesterday" in UTC, edges overlapped, dedupe by id; re-running a window is idempotent. *Proof: T23.*
- **R3 · Pagination truncation.** Stripe lists paginate (`has_more`); forgetting = silently reconciling half the data. *Proof: T23.*
- **R4 · CI depending on live Stripe.** Flaky, slow, secret-coupled. D6: stub in CI; manual smoke for real wiring. *Proof: T13–T24 all run stubbed.*

## 4. Test specifications (T13–T24, continuing from ledger-spec)

- **T13** Invalid/absent signature → 400; no `webhook_event` row; no side effects.
- **T14** Valid signature computed over exact raw bytes (payload with unusual whitespace) → accepted and parsed.
- **T15** Same `event_id` delivered twice → one PROCESSED row, one transition/entry; second delivery → 200 no-op.
- **T16** Deliver `succeeded` while PENDING → forward-jump to CAPTURED + exactly one capture entry; then a late `amount_capturable_updated` → 200 benign, nothing changes.
- **T17** Injected transient failure on first processing → 500; redelivery of the same event → success; exactly one ledger effect; event ends PROCESSED.
- **T18** `succeeded` whose amount ≠ `payment.amount_minor` → no transition, no posting; event `NEEDS_ATTENTION`.
- **T19** Partial-refund event → `NEEDS_ATTENTION`, no posting, status stays CAPTURED.
- **T20** Injected crash between the Stripe call and finalize → retry with the same key completes; stub saw the **same** Stripe idempotency key twice; exactly one local payment + one stub PI; a request during the claim window gets `409 IDEMPOTENCY_IN_PROGRESS`.
- **T21** Stub lists 3 succeeded PIs; local has 2 CAPTURED → exactly one `MISSING_LOCAL` with the right refs.
- **T22** Local CAPTURED payment absent remotely → `MISSING_REMOTE`; stub amount differs → `AMOUNT_MISMATCH`; both recorded; ledger untouched.
- **T23** Stub paginates (2 pages); recon runs twice over the same window → all items covered, identical discrepancy set, no duplicates.
- **T24** Two concurrent capture requests, same idempotency key → exactly one `StripeGateway.capture` invocation (stub counts), both callers get the replayed 202; the subsequent `succeeded` webhook → exactly one transition + one entry.

## 5. Suggested card cut (draft the actual cards at P3 entry, from this doc)

P3-01 spike: SDK + Stripe CLI wiring + **verify a MYR test-mode PaymentIntent on day 1** · P3-02 webhook endpoint: signature, event store, dedupe (T13–T15, T17) · P3-03 claim-then-execute retrofit + create/confirm PI (T20) · P3-04 capture/refund + lifecycle mapping + races (T16, T18, T19, T24, W11) · P3-05 recon job + report (T21–T23) · P3-06 retire `/simulate/*` from the public API (service-level simulation stays for seeder/tests), Checkpoint A polish: soft deploy + README GIF + docs homework.

## 6. Audit checklist — Phase 3 entry

1. Before anything else: does a **MYR** test-mode PaymentIntent actually work in your Stripe account? (If not, this doc gets a currency amendment — find out on day 1, not week 8.)
2. Retell from memory why P2's single-tx idempotency dies here, and how claim-then-execute + key propagation fixes it.
3. Challenge D1: is manual capture worth the auth-expiry handling? What would the machine look like with automatic capture?
4. Challenge D5's forward-jump: comfortable with PENDING→CAPTURED? The alternative is fetch-the-PI-and-reconcile-to-state on every event (simpler mental model, one extra API call per event). Choose consciously.
5. When, if ever, would auto-healing recon be acceptable? Defend R1 or amend it.
6. Confirm the P6 hooks: `NEEDS_ATTENTION` events and `MISSING_LOCAL` discrepancies should become cases — where exactly?
7. Any contradiction with CLAUDE.md §4 or ledger-spec invariants I1–I9?

**Invariants extension:** **I10** every external event is processed at most once (`event_id` UNIQUE) · **I11** webhook handlers change state/ledger only via the transition + posting services — no side channels · **I12** reconciliation never writes to the ledger.

**Assumptions register:** full refunds only (inherited) · non-3DS test payment methods only (`requires_action` flows → icebox) · payouts & balance-transaction recon → icebox · stuck-claim TTL deferred · live smoke tests are manual.
