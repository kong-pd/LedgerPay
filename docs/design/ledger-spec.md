# Ledger Core Specification (Phase 2)

> **Status: DRAFT.** Written 2026-07-04, before the owner's Phase 2 concept course — deliberately.
> **Do not implement from this document until it has been audited** at Phase 2 entry: owner takes the concept course (`docs/ai/curriculum.md` → Phase 2), then walks §12's audit checklist with an AI assistant, challenging every decision. Decisions confirmed there flip ADR-0004/0005 from *Proposed* to *Accepted*.
> Every design choice below carries its rationale so it can be interrogated by someone who did not write it.

---

## 1. Purpose, scope, glossary

Phase 2 delivers the **correctness kernel**: the double-entry ledger, the posting service, idempotency, and the payment state machine — driven by *simulated* triggers. Phase 3 swaps those triggers for Stripe events; the machine itself does not change.

**In scope (P2):** ledger schema · posting service · balances + trial balance · internal transfers with overdraft protection · generic idempotency mechanism · payment entity + state machine + `/simulate/*` trigger endpoints · capture/refund postings · reversal (correction) flow · invariant & concurrency test suite · demo seeder groundwork.

**Out of scope (P2):** Stripe (P3) · webhooks (P3) · reconciliation (P3) · auth (P4 — all endpoints are open in dev until then) · fees · partial refunds · multi-currency · idempotency-record TTL/cleanup (see §6.5).

**Glossary — three words that must never be confused:**

| Term | Meaning | Owned by |
|---|---|---|
| `Customer` | the person (KYC subject, P5) | Phase 1 |
| `Account` | the customer-facing product account ("wallet" colloquially); has a status; the thing KYC will gate | Phase 1 |
| `LedgerAccount` | the accounting object; where debits/credits land | Phase 2 |

Each `Account` is provisioned with exactly one `LedgerAccount` (1:1 in this project). APIs speak in `Account` ids; the ledger module maps them internally. `Customer`/`Account` tables may gain columns later; ledger tables are frozen shapes.

## 2. Chart of accounts & debit/credit conventions

Account types and their *normal side* (the side that increases the balance):

| Type | Normal side | P2 usage |
|---|---|---|
| ASSET | DEBIT | `PLATFORM_CASH` — money the platform holds |
| LIABILITY | CREDIT | `WALLET-{accountId}` — money owed to each customer |
| EQUITY / REVENUE / EXPENSE | CREDIT / CREDIT / DEBIT | none in P2 (fees → P3+, icebox) |

**Worked examples (amounts in sen, MYR):**

*Top-up of RM 100.00 into customer A's wallet* (platform receives money, and now owes it to A):

| Leg | Account | Direction | amount_minor |
|---|---|---|---|
| 1 | PLATFORM_CASH (ASSET) | DEBIT | 10000 |
| 2 | WALLET-A (LIABILITY) | CREDIT | 10000 |

*Internal transfer RM 25.00 from A to B* (platform's total debt unchanged, just re-owed):

| Leg | Account | Direction | amount_minor |
|---|---|---|---|
| 1 | WALLET-A (LIABILITY) | DEBIT | 2500 |
| 2 | WALLET-B (LIABILITY) | CREDIT | 2500 |

Balance formula: debit-normal accounts → `Σdebits − Σcredits`; credit-normal → `Σcredits − Σdebits`. A customer wallet balance is therefore `Σcredits − Σdebits` and must never go below zero (§5).

**Representation decision:** each posting row carries `direction ∈ {DEBIT, CREDIT}` and a strictly **positive** `amount_minor`.
*Alternative considered:* signed amounts (+ = debit, − = credit, entry sums to zero). Rejected: sign conventions hide errors from a learner; the textbook form makes invariant I1 read literally.

## 3. Data model (Flyway draft)

Migration plan: `V2__ledger_core.sql` → `V3__idempotency.sql` → `V4__payments.sql`. DDL below is a draft; the implementing card may adjust syntax, **not semantics**.

```sql
-- V2__ledger_core.sql
CREATE TABLE ledger_account (
  id               BIGINT PRIMARY KEY AUTO_INCREMENT,
  code             VARCHAR(64)  NOT NULL UNIQUE,   -- 'PLATFORM_CASH', 'WALLET-42'
  type             VARCHAR(16)  NOT NULL,          -- ASSET|LIABILITY|EQUITY|REVENUE|EXPENSE
  currency         CHAR(3)      NOT NULL,          -- always 'MYR' (validated in code)
  owner_account_id BIGINT       NULL,              -- FK -> account(id); NULL = platform account
  created_at       TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  CONSTRAINT chk_la_type CHECK (type IN ('ASSET','LIABILITY','EQUITY','REVENUE','EXPENSE'))
);
-- normal side is DERIVED from type in code (single source of truth), not stored.

CREATE TABLE journal_entry (
  id                 BIGINT PRIMARY KEY AUTO_INCREMENT,
  entry_ref          CHAR(36)     NOT NULL UNIQUE,   -- UUIDv4; external identifier
  entry_type         VARCHAR(32)  NOT NULL,          -- MANUAL_CREDIT|INTERNAL_TRANSFER|PAYMENT_CAPTURE|PAYMENT_REFUND|REVERSAL
  description        VARCHAR(255) NULL,
  reverses_entry_id  BIGINT       NULL UNIQUE,       -- FK -> journal_entry(id); UNIQUE = at most one reversal per entry (I8)
  payment_id         BIGINT       NULL,              -- FK -> payment(id), set on capture/refund entries
  posted_at          TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  created_by         VARCHAR(64)  NOT NULL DEFAULT 'system'
);
-- No updated_at on purpose: rows are never updated (I2).

CREATE TABLE posting (
  id                BIGINT PRIMARY KEY AUTO_INCREMENT,
  journal_entry_id  BIGINT      NOT NULL,            -- FK -> journal_entry(id)
  ledger_account_id BIGINT      NOT NULL,            -- FK -> ledger_account(id)
  direction         VARCHAR(6)  NOT NULL,
  amount_minor      BIGINT      NOT NULL,
  currency          CHAR(3)     NOT NULL,
  CONSTRAINT chk_p_dir    CHECK (direction IN ('DEBIT','CREDIT')),
  CONSTRAINT chk_p_amount CHECK (amount_minor > 0),
  INDEX idx_posting_account (ledger_account_id, id)  -- balance scans
);
```

```sql
-- V3__idempotency.sql
CREATE TABLE idempotency_record (
  id               BIGINT PRIMARY KEY AUTO_INCREMENT,
  endpoint         VARCHAR(128) NOT NULL,   -- logical operation, e.g. 'POST /api/v1/transfers'
  idem_key         VARCHAR(64)  NOT NULL,   -- client-supplied Idempotency-Key header
  request_hash     CHAR(64)     NOT NULL,   -- SHA-256 hex of the raw request body bytes
  status           VARCHAR(16)  NOT NULL,   -- IN_PROGRESS | SUCCEEDED | FAILED_BUSINESS
  response_status  SMALLINT     NULL,
  response_body    TEXT         NULL,       -- stored JSON, replayed verbatim
  journal_entry_id BIGINT       NULL,
  created_at       TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  completed_at     TIMESTAMP(6) NULL,
  CONSTRAINT uq_idem UNIQUE (endpoint, idem_key)     -- THE claiming mechanism (I4)
);
-- Forward-compat note: when auth lands (P4), the natural key becomes (principal, endpoint, idem_key) — planned migration.
```

```sql
-- V4__payments.sql
CREATE TABLE payment (
  id            BIGINT PRIMARY KEY AUTO_INCREMENT,
  payment_ref   CHAR(36)    NOT NULL UNIQUE,
  account_id    BIGINT      NOT NULL,        -- FK -> account(id): the wallet being topped up
  amount_minor  BIGINT      NOT NULL,
  currency      CHAR(3)     NOT NULL,
  status        VARCHAR(24) NOT NULL,        -- PENDING|AUTHORIZED|CAPTURED|FAILED|REFUNDED
  provider      VARCHAR(16) NOT NULL DEFAULT 'SIMULATED',   -- SIMULATED (P2) | STRIPE (P3)
  provider_ref  VARCHAR(128) NULL,           -- Stripe PaymentIntent id in P3
  created_at    TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at    TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  CONSTRAINT chk_pay_amount CHECK (amount_minor > 0)
);
-- payment is deliberately MUTABLE state (unlike ledger rows); its history lives in payment_transition.

CREATE TABLE payment_transition (
  id           BIGINT PRIMARY KEY AUTO_INCREMENT,
  payment_id   BIGINT       NOT NULL,        -- FK -> payment(id)
  from_status  VARCHAR(24)  NOT NULL,
  to_status    VARCHAR(24)  NOT NULL,
  trigger_name VARCHAR(64)  NOT NULL,        -- 'simulate.capture' (P2) / 'stripe.payment_intent.succeeded' (P3)
  occurred_at  TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  metadata     JSON         NULL
);
```

**JPA notes:** `JournalEntry` and `Posting` are annotated `@org.hibernate.annotations.Immutable`, expose no setters, and their repositories expose no update/delete methods (I2). Enums stored as `VARCHAR` via `@Enumerated(EnumType.STRING)` — never ordinal.

## 4. Posting service — the single gate to the ledger

All ledger writes, without exception, go through one method:

```
LedgerPostingService.post(entryType, description, legs[], links) -> JournalEntry
```

Algorithm (one `@Transactional` unit):
1. Validate: ≥ 2 legs; ≥ 1 DEBIT and ≥ 1 CREDIT; every `amount_minor > 0`; every currency == MYR; all referenced ledger accounts exist; Σdebit == Σcredit — else throw `UnbalancedEntryException` → nothing persists.
2. Insert `journal_entry`, then all `posting` rows.
3. Commit. Any exception anywhere → full rollback (T3 proves it).

**Honesty note (write it in interviews too):** MySQL cannot express "Σdebit == Σcredit per entry" as a declarative constraint. The guarantee is therefore layered: single code path (this service) + one transaction + `@Immutable` entities + the invariant test suite + the trial-balance check (§9). A DB trigger was considered and rejected: it hides logic from the debugger and the learner.

`MANUAL_CREDIT` (DR PLATFORM_CASH / CR wallet) exists **at service level only** — used by tests and the demo seeder to fund wallets. It has no HTTP endpoint.

## 5. Balances & concurrency — ADR-0005

**Balance = derived**, computed from postings:
`SELECT COALESCE(SUM(CASE WHEN direction='DEBIT' THEN amount_minor ELSE -amount_minor END),0) FROM posting WHERE ledger_account_id = ?` — then negate for credit-normal accounts. Correct by construction; nothing can drift. (Materialized balance column = rejected for P2; revisit-when in ADR-0005.)

**Overdraft rule:** any posting that **debits a customer wallet** (transfer out, refund) must pass a funds check; platform accounts are exempt. Enforced by a single guarded path: `postWithFundsCheck(...)`.

**Serialization of concurrent wallet debits:** `SELECT ... FOR UPDATE` on the *source* `ledger_account` row as a lock anchor → compute balance → check → post. Only the source row is locked (credits need no check), so A→B and B→A cannot deadlock. If any future operation ever locks two accounts: always lock in ascending `id` order.

> **⚠ The MVCC trap — read this twice.** Under MySQL's default `REPEATABLE READ`, a transaction's plain `SELECT`s read from a snapshot taken at its *first* consistent read. Sequence that loses money: T2 does an innocent early read (snapshot taken) → T1 commits a transfer out of wallet A → T2 acquires the `FOR UPDATE` lock on A (locking reads always see latest) → T2's balance `SUM` **still reads the old snapshot** → overdraft slips through. Fix: money-moving use-cases run with `@Transactional(isolation = READ_COMMITTED)` so every statement gets a fresh snapshot, and the post-lock `SUM` sees all committed postings. Test T7 exists to catch exactly this. Do not "simplify" this away.

## 6. Idempotency — the generic mechanism

Required on every money-moving/state-changing POST (`/transfers`, `/payments`, `/payments/{ref}/simulate/*`, `/journal-entries/{ref}/reverse`). Missing header → `400 IDEMPOTENCY_KEY_REQUIRED`.

**Flow (single transaction, insert-first):**
1. Hash raw request body bytes → SHA-256. (Deliberate simplification: byte-identical retries only. Two semantically equal bodies with different whitespace count as *different* requests → step 3b. Canonical-JSON hashing rejected as a rabbit hole.)
2. `INSERT idempotency_record (endpoint, key, hash, IN_PROGRESS)` — this **claims** the key via `uq_idem`.
   - **Concurrent duplicate:** its INSERT blocks on the unique-index lock until we commit/roll back, then gets a duplicate-key error → handler catches it → re-SELECT the record → replay (3a) or conflict (3b).
3. On existing record: (a) `request_hash` matches → return stored `response_status` + `response_body`, header `Idempotency-Replay: true`. (b) hash differs → `422 IDEMPOTENCY_KEY_REUSE`, touch nothing.
4. Execute business logic *in the same transaction*; `UPDATE` the record → `SUCCEEDED` (or `FAILED_BUSINESS` for 4xx business outcomes, which are also stored and replayed) + response snapshot; commit.

**Elegant property of the single-tx design:** a crash before commit rolls back the claim too → the retry re-executes cleanly → `IN_PROGRESS` is never visible after the fact. No TTL/reaper needed in P2. **P3 warning:** once a Stripe call (an external side effect that cannot roll back) sits in the middle, this collapses — P3 uses claim-then-execute in separate transactions and inherits the stuck-claim problem deliberately deferred here.

## 7. Payment state machine — ADR-0004

States: `PENDING, AUTHORIZED, CAPTURED, FAILED, REFUNDED`. Legal transitions (everything else → `409 ILLEGAL_TRANSITION`, no rows written):

| From | To | P2 trigger | P3 trigger (future) | Ledger effect (same tx) |
|---|---|---|---|---|
| PENDING | AUTHORIZED | `simulate.authorize` | `payment_intent.amount_capturable_updated` | — |
| PENDING | FAILED | `simulate.fail` | `payment_intent.payment_failed` | — |
| AUTHORIZED | CAPTURED | `simulate.capture` | `payment_intent.succeeded` | DR PLATFORM_CASH / CR wallet, `PAYMENT_CAPTURE` |
| AUTHORIZED | FAILED | `simulate.fail` | `payment_intent.canceled` | — |
| CAPTURED | REFUNDED | `simulate.refund` | `charge.refunded` | DR wallet / CR PLATFORM_CASH, `PAYMENT_REFUND` (full amount only; **goes through the funds-checked path** — if the customer already spent it, refund fails `INSUFFICIENT_FUNDS`; simplification, note in demo) |

Implementation: `PaymentStatus` enum + a `PaymentTransitionService` that (1) validates the transition against the table, (2) writes the `payment_transition` row, (3) updates `payment.status`, (4) posts the ledger effect if any — all in one transaction. **No other code may write `payment.status`** (CLAUDE.md §4.5).

> **Insight worth remembering: a state guard is not retry-idempotency.** Client captures, response times out, client retries: the guard now sees CAPTURED→CAPTURED = illegal → 409 — the client can't tell success from failure. That is why `/simulate/*` endpoints *also* require an `Idempotency-Key`: the retry replays the original 200 instead. The guard protects against *wrong* transitions; the idempotency layer protects against *repeated* ones.

## 8. Reversals (corrections)

`POST /api/v1/journal-entries/{ref}/reverse` creates a new entry of type `REVERSAL` whose legs mirror the original exactly (directions swapped, amounts identical), sets `reverses_entry_id`. Guards: cannot reverse a `REVERSAL`; cannot reverse twice (`UNIQUE reverses_entry_id` makes the race safe); a reversal that would drive a wallet negative fails the funds check. Original rows are never touched — this *is* the append-only correction story (curriculum P2 self-check #1).

## 9. API surface (P2)

| Endpoint | Notes |
|---|---|
| `POST /api/v1/transfers` | body: `{fromAccountId, toAccountId, amountMinor, currency, description}`; Idempotency-Key required → 201 |
| `POST /api/v1/payments` | create simulated top-up: `{accountId, amountMinor, currency}` → 201 PENDING |
| `POST /api/v1/payments/{ref}/simulate/{authorize\|capture\|fail\|refund}` | P2-only trigger endpoints; deleted in P3 |
| `GET /api/v1/payments/{ref}` | includes transition history |
| `GET /api/v1/ledger/accounts/{id}/balance` · `GET .../postings?page=` | balance + statement views |
| `GET /api/v1/journal-entries/{ref}` · `POST .../{ref}/reverse` | entry detail; correction |
| `GET /api/v1/ledger/trial-balance` | per-account debit/credit totals + global Σdebit == Σcredit flag — the "books are provably balanced" demo artifact |

Error codes (reuse P1 global format `{code, message, fieldErrors[]}`): `IDEMPOTENCY_KEY_REQUIRED`, `IDEMPOTENCY_KEY_REUSE`, `INSUFFICIENT_FUNDS`, `ILLEGAL_TRANSITION`, `VALIDATION_ERROR`, `NOT_FOUND`.

## 10. Invariants → enforcement → proof

| # | Invariant | Enforced by | Test |
|---|---|---|---|
| I1 | Every entry balances (Σdebit==Σcredit, ≥1 each side) | posting service + single tx | T1,T2,T11 |
| I2 | Ledger & transition rows append-only | `@Immutable`, no setters/update methods | T12 |
| I3 | Amounts > 0; currency == MYR everywhere | CHECKs + service validation | T2 |
| I4 | (endpoint,key) executes at most once | `uq_idem` + insert-first | T4,T5,T9 |
| I5 | Customer wallets never negative, even under races | funds-checked path + FOR UPDATE + READ_COMMITTED | T6,T7 |
| I6 | Status changes only via transition service; all logged | single writer + transition rows | T8 |
| I7 | CAPTURED ⇒ exactly one capture entry; REFUNDED ⇒ capture+refund | transition service posts in same tx | T9 |
| I8 | At most one reversal per entry; mirrored legs | `UNIQUE reverses_entry_id` + guards | T10 |
| I9 | Global trial balance holds at all times | consequence of I1 | T11 |

## 11. Test specifications (write these before the code — CLAUDE.md §2.3)

All integration tests run against Testcontainers MySQL 8. Fixture: `LedgerFixtures.fund(accountId, amountMinor)` posts a `MANUAL_CREDIT` through the real posting service.

- **T1 balanced happy path.** Fund wallet A 10000 → assert PLATFORM_CASH balance 10000 (debit-normal), WALLET-A 10000 (credit-normal), entry has exactly 2 postings.
- **T2 unbalanced/invalid rejected.** Attempt legs DR 10000 / CR 9900 → `UnbalancedEntryException`; also amount 0, negative, currency `USD`, single-leg → rejected; row counts unchanged in every case.
- **T3 mid-transaction atomicity.** Leg 2 references a nonexistent ledger account → exception → journal_entry AND posting counts both unchanged (no orphan header).
- **T4 concurrent idempotency.** Fund A 100000. 10 threads, same key `t4-key` + byte-identical body (transfer A→B 2500), released by a `CountDownLatch` → exactly 1 journal entry, exactly 1 idempotency record, A == 97500, B == 2500; all 10 responses identical; ≥9 carry `Idempotency-Replay: true`.
- **T5 key reuse, different body.** Same key, amount 2600 → `422 IDEMPOTENCY_KEY_REUSE`, no new rows.
- **T6 insufficient funds.** A has 5000; transfer 6000 → `422 INSUFFICIENT_FUNDS`, nothing persisted.
- **T7 concurrent overdraft race (the MVCC test).** A has 10000. Two threads, *different* keys, each transfer A→B 8000, barrier-released → exactly one succeeds, one gets `INSUFFICIENT_FUNDS`; A == 2000, B == 8000; trial balance holds. Must fail if isolation fix (§5) is removed — verify by temporarily reverting during review.
- **T8 illegal transition.** Payment in PENDING → `simulate/capture` → `409 ILLEGAL_TRANSITION`; no transition row, no entry, status still PENDING.
- **T9 capture exactly-once.** Authorize payment; two concurrent `simulate/capture` with the same key → one execution, one replay; exactly one PAYMENT_CAPTURE entry; wallet credited once. Then `simulate/refund` → REFUNDED, wallet debited back, trial balance holds.
- **T10 reversal.** Transfer A→B 2500, reverse it → balances restored exactly; reversal legs mirror original; second reverse → 409; reversing the reversal → 409.
- **T11 trial-balance property test.** Seeded RNG runs ~200 random valid ops (fund/transfer/authorize/capture/refund with random amounts 1..50000) → after each batch: global Σdebits == Σcredits, every wallet ≥ 0. Print seed on failure for reproduction.
- **T12 immutability.** Load a posting, mutate via reflection/detached copy and merge-flush → no UPDATE issued (assert via SQL log or unchanged row) ; repositories expose no delete/update for ledger types (compile-time check by API absence).

## 12. Audit checklist — run this at Phase 2 entry, after the concept course

Challenge each; changing an answer means editing this spec + the ADR before any code:
1. Re-derive the two worked examples in §2 yourself. Do the directions feel inevitable, not memorized?
2. Direction-enum vs signed amounts (§2): still convinced? What breaks in reporting if we switch later?
3. Derived balance + row lock (§5, ADR-0005): at what posting volume does `SUM` hurt, and what's the migration path? Is locking only the source row really deadlock-free — walk A→B ∥ B→A aloud.
4. Explain the MVCC trap (§5) to the assistant *from memory*. If you can't, do not implement P2-04 yet.
5. Single-tx idempotency (§6): why exactly does it break in P3? What replaces it?
6. Hash-raw-bytes (§6): acceptable, or do you want canonical JSON? What client behavior would force the upgrade?
7. State-guard ≠ retry-idempotency (§7): reproduce the timed-out-capture story. Should refunds allow negative wallets instead of failing? (We said no — defend or change.)
8. Refund of spent funds fails (§7): is that acceptable for the demo narrative?
9. Anything here contradicting CLAUDE.md §4? (There should be nothing.)

**Assumptions & simplifications register:** full-amount refunds only · no fees · no idempotency TTL · unauthenticated endpoints until P4 · `MANUAL_CREDIT` has no API · account ids (not opaque refs) in request bodies until further notice.
