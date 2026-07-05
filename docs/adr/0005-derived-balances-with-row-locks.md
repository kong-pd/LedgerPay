# ADR-0005 — Balances derived from postings + `FOR UPDATE` row-lock serialization

- **Date:** 2026-07-04
- **Status:** Proposed — to be Accepted at Phase 2 entry after the concept-course audit (ledger-spec §12)

## Context

Reads need account balances; wallet debits need an overdraft check that holds under concurrency. Two classic designs exist, and the choice shapes both correctness and the demo story.

## Options considered

1. **Materialized `balance_minor` column**, updated in the same transaction as each posting — O(1) reads; but creates a second source of truth that can drift from the postings, and drift in a ledger project is a credibility hole.
2. **Derived balance** — `SUM` over postings at read time. Correct by construction; nothing to drift; O(n) per read, which is irrelevant at demo scale with `idx_posting_account`.

## Decision

Option 2, with concurrency handled as follows (details: ledger-spec §5):
- Any posting that debits a customer wallet goes through a funds-checked path.
- That path takes `SELECT … FOR UPDATE` on the **source** `ledger_account` row as a lock anchor, then computes the `SUM`, then posts — serializing concurrent debits of the same wallet. Only the source row is locked, so opposing transfers cannot deadlock; if two locks are ever needed, lock in ascending id order.
- Money-moving transactions run at **`READ_COMMITTED`** isolation. Under default `REPEATABLE READ`, a stale MVCC snapshot taken before the lock can make the post-lock `SUM` miss committed postings and let an overdraft through — test T7 exists to catch exactly this regression.

## Consequences

- ✅ "The balance *is* the postings" — the strongest possible integrity story, and reconciliation-friendly for P3.
- ✅ Trial balance and per-account statements come for free.
- ⚠️ Requires the isolation-level discipline above; this is the one place P2 code is allowed to be subtle.
- ⚠️ Reads are O(postings-per-account). Migration path if it ever matters: add a materialized balance as a *cache* verified against the derived value by a reconciliation job — never as the source of truth.

## Revisit when

Per-account posting counts reach a scale where `SUM` latency is observable in the demo (won't happen here), or P7 load-testing curiosity makes the cached-balance exercise worth doing.
