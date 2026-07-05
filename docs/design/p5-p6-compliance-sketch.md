# Phase 5–6 Compliance Layer — Design Sketch (KYC · Monitoring · Cases · Audit Chain)

> **Status: DRAFT sketch.** Written 2026-07-04. Less exhaustive than `ledger-spec.md` on purpose — P5/P6 reuse patterns P2 already proved (state machines, claiming via unique constraints, single-writer services). What this document pins down is the part cheap-model sessions would most plausibly get wrong: the **rule representation**, the **evaluation timing**, and the **audit hash chain's two traps**. Audit §7 at P5/P6 entry; promote D-items to ADRs then. Task cards are drafted at phase entry from §6.

## 1. Phase 5 — KYC (brief: it is P2 patterns wearing a compliance hat)

- **KYC application state machine:** `PENDING_REVIEW → IN_REVIEW → APPROVED | REJECTED`; `REJECTED → PENDING_REVIEW` (resubmission). Implemented as a **copy of the P2 pattern** (enum + transition table + single-writer transition service + transition log) — deliberately *not* a generic framework; two concrete copies beat one premature abstraction (revisit if a third machine appears).
- **The gate lives in the backend:** transfer/payment creation checks the customer's KYC state and rejects with `403 KYC_REQUIRED` before touching money paths. UI hides buttons; the service enforces.
- **Documents:** fake, watermarked samples only (CLAUDE.md §4.8). Stored as `kyc_document` rows (`LONGBLOB` + mime + filename) — DB blobs are the right call at demo scale; S3 is a noted upgrade path, not a P5 task.
- **Decisions carry reason codes** (enum: `DOC_BLURRY`, `DOC_EXPIRED`, `NAME_MISMATCH`, `WATCHLIST_HIT`, `OTHER`) — free-text-only rejections are an anti-pattern auditors flag.
- **Audit ordering subtlety:** the hash chain arrives in P6, but P5 actions must already be audited. Resolution: P5 writes **plain rows** into the (P6-shaped) `audit_event` table with hash columns NULL; **P6's first card runs a chain-ify migration** that computes the chain over existing rows in id order, then flips on live chaining. One table, no rework.

## 2. Phase 6 — Rule engine (D1, D2)

**D1 — Rule *types* live in code; rule *instances* live in data.** A `MonitoringRule` DB row = `{code UNIQUE, type, name, params JSON, direction OUTFLOW|INFLOW|ANY, severity LOW|MEDIUM|HIGH, enabled}`. The catalog of types (each a small, unit-testable Java class):

| Type | Fires when (per customer account) | params |
|---|---|---|
| `THRESHOLD` | single qualifying entry amount ≥ X | `threshold_minor` |
| `VELOCITY_COUNT` | ≥ N qualifying entries within window | `count`, `window_hours` |
| `VELOCITY_SUM` | Σ amounts within window ≥ X | `sum_minor`, `window_hours` |
| `STRUCTURING` | ≥ N entries each in `[X·(1−m), X)` within window — "just under the line" | `threshold_minor`, `margin_pct`, `count`, `window_hours` |
| `WATCHLIST` | counterparty on the mock watchlist | `list_code` |

*Rejected alternatives:* a stored expression DSL (SpEL/scripting in DB = injection surface, untestable, scope explosion) and Drools (per earlier decision). "Parameters are data, semantics are code" is the honest, defensible middle. New rule type ⇒ code change — acceptable and correct. **All demo thresholds are toy values, not regulatory values — say so in the README.**

**D2 — Alerts snapshot their rule.** An alert stores `rule_id` + `params_snapshot JSON` (rules are editable; alerts must stay explainable as-at-trigger-time). Rule create/edit/enable/disable each emit an audit event. Maker-checker approval for rule changes stays in the Icebox.

## 3. Evaluation timing (D3) — never let compliance block money

Synchronous evaluation inside the money transaction couples payment success to monitoring health — wrong direction of dependency. Design: **listener for latency, sweeper for completeness.**

- **Listener:** `@TransactionalEventListener(phase = AFTER_COMMIT)` on "entry posted" → evaluate enabled rules for the affected customer(s). Fast path; may be lost if the app dies in the gap.
- **Sweeper:** a scheduled job holds a `monitoring_checkpoint` (last evaluated `journal_entry.id`) and re-evaluates everything above it. Catches listener losses → evaluation is **at-least-once**.
- **Alerting stays exactly-once** via `UNIQUE (rule_id, trigger_entry_id)` on `alert` — the P2 claiming pattern, third reuse. Listener/sweeper double-fire collapses on the constraint.
- Evaluation failures are logged and left to the sweeper; they never surface into money paths (invariant I16).

## 4. Alerts → cases → workflow (D4)

- **Grouping:** if the customer already has a case in `OPEN`/`IN_INVESTIGATION`, new alerts **attach** to it; otherwise a case is created. (Real desks group; one-case-per-alert floods the queue and demos badly.)
- **Case machine:** `OPEN → IN_INVESTIGATION → CLOSED_FALSE_POSITIVE | ESCALATED_STR` (terminal; "STR" is simulated, JFIU-style wording in docs). P2 pattern copy again: transition service = single writer, transition log, `409 ILLEGAL_TRANSITION`.
- Case has: assignee, severity (max of attached alerts), append-only `case_note` rows, linked alerts → linked entries.
- **One desk, many sources (P3 hooks land here):** `case_type ∈ {TRANSACTION_MONITORING, WEBHOOK_ANOMALY, RECONCILIATION}` — P3's `NEEDS_ATTENTION` events and `MISSING_LOCAL` discrepancies become cases through the same pipe.

## 5. Audit hash chain (D5) — the two traps that break naive implementations

**Schema:** `audit_event(id AUTO_INCREMENT, occurred_at, actor, action, entity_type, entity_id, details JSON, prev_hash CHAR(64), curr_hash CHAR(64) UNIQUE, UNIQUE(prev_hash))` — append-only, `@Immutable`.

**Trap 1 — canonicalization.** A hash is over *bytes*; "hash the JSON" is not a specification. Re-serializing JSON (field order, whitespace, number formatting, unicode escapes) differs across library versions and reads → verification breaks later for no visible reason. **Rule: never re-serialize for hashing.** Canonical form v1 is a fixed pipe-delimited UTF-8 string:

```
v1|{id}|{occurred_at as epoch micros}|{actor}|{action}|{entity_type}|{entity_id}|{sha256hex(stored details bytes)}|{prev_hash}
curr_hash = sha256hex(utf8(canonical))
```

`details` is hashed as **the exact bytes stored in the column** — the JSON is payload, not structure. The `v1|` prefix future-proofs format evolution. (This is P6's equivalent of P2's MVCC box: retell it from memory before implementing.)

**Trap 2 — who is "previous"?** Two concurrent appends both read the same head → forked chain. **Rule: appends are serialized on a single `audit_chain_head` row via `SELECT … FOR UPDATE`** (the P2 locking skill, reused), inside the same transaction as the audited action — so I13 (action ⇒ exactly one event) holds atomically. `UNIQUE(prev_hash)` is the belt-and-suspenders: a fork physically cannot commit. Contention is irrelevant at demo scale; async batching → Icebox.

**Genesis:** `prev_hash` of row 1 = `sha256hex("LEDGERPAY_AUDIT_GENESIS_V1")`.

**Verification:** `GET /api/v1/audit/verify` walks id order, recomputes, returns `{ok}` or `{ok:false, first_broken_id}`. Ship the **tamper script** (`scripts/tamper-audit.sql` flips one byte of an old row's details) — the demo's party trick.

**Honesty box (goes in README, verbatim spirit):** the chain is tamper-**evident**, not tamper-proof. An attacker with DB write access can rewrite history *if they recompute every subsequent hash*; and truncating the **tail** is invisible unless the head hash is recorded elsewhere. Real systems therefore anchor the head externally (external store / counterparty / public chain) — out of scope here (Icebox), and the docs must claim exactly this much, no more. Scoping note: the chain covers **human/config/compliance actions**; the money layer's evidence is the ledger itself, which is already append-only — auditing ledger rows into the chain would be duplication, not defense.

## 6. Invariants, tests, suggested card cut

**Invariants (continuing):** **I13** every compliance action emits exactly one audit event, same transaction · **I14** audit chain append-only, canonical v1, `UNIQUE(prev_hash)`, rooted at genesis · **I15** evaluation at-least-once, alerting exactly-once (`UNIQUE(rule_id, trigger_entry_id)`) · **I16** monitoring never blocks or mutates money movement · **I17** case status changes only via its transition service, all logged · **I18** alerts keep `params_snapshot`; rule edits audited.

**Test specs (continuing):**
- **T25** rule-type unit tests incl. boundaries: amount == threshold excluded from STRUCTURING band, == `threshold·(1−m)` included; THRESHOLD at exactly X fires.
- **T26** structuring end-to-end: threshold 500000, margin 5%, count 5, window 24h; seed five transfers of 490000 sen → exactly one alert + one case; a sixth attaches to the same open case.
- **T27** listener + sweeper double-fire on the same entry → one alert (constraint absorbs the race).
- **T28** listener disabled (simulated crash), entries posted → sweeper catches up from checkpoint, alerts appear, checkpoint advances.
- **T29** rule evaluation throws → the transfer still committed and returned success; error logged; sweeper retries later.
- **T30** chain happy path: N events → verify OK.
- **T31** tamper one byte of event k's details via SQL → verify reports first break at exactly k.
- **T32** 10 concurrent audited actions (barrier) → chain intact: every `prev_hash` == predecessor's `curr_hash`, verify OK.
- **T33** canonicalization stability: same fields hash identically across restarts; details with different key order are different bytes ⇒ different hash (documents the stored-bytes semantics).
- **T34** case lifecycle: illegal transition 409, no rows; every legal change has transition row + audit event.
- **T35** KYC gate: unapproved customer's transfer/payment → `403 KYC_REQUIRED`, zero side effects.
- **T36** audit atomicity: inject failure in the audit insert during KYC approve → the whole approval rolls back (I13).

**Suggested cards (draft at phase entry):** P5-01 KYC schema + machine + upload · P5-02 review queue + gate + plain audit events + panel slice (T34-KYC, T35, T36) · **P6-01 audit chain core first** (canonical v1, head lock, verify endpoint, chain-ify migration over P5 rows, tamper script — T30–T33) · P6-02 rule catalog + engine + listener/sweeper (T25–T29) · P6-03 alerts → cases + workflow + P3-hook case types (T26, T34) · P6-04 compliance panel screens + demo seeder scenario (the blueprint's beats 5–6) + phase wrap.

## 7. Audit checklist — P5/P6 entry

1. Retell Trap 1 from memory: why is "hash the JSON" not a spec, and why do we hash the stored bytes of `details`?
2. Retell Trap 2: walk two concurrent appends aloud — what does the head lock do, and what does `UNIQUE(prev_hash)` add on top?
3. What exactly does the chain **not** prove? (full-rewrite with recomputed hashes; tail truncation without an external head record.) Can you say this in an interview without undermining the project?
4. Defend "listener for latency, sweeper for completeness" — what breaks with listener-only? With sweeper-only?
5. Challenge D1: name a rule you'd want that the five types can't express. Does it justify a DSL, or a sixth type?
6. Challenge case grouping — when would one-case-per-alert be better?
7. Defend the scoping call that ledger rows are NOT chained. Any contradiction with CLAUDE.md §4 or invariants I1–I12?

**Assumptions register:** demo thresholds ≠ regulatory values · maker-checker → Icebox · external head anchoring → Icebox · async audit batching → Icebox · watchlist is a mock table, no real list data.
