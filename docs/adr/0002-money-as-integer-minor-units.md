# ADR-0002 — Money as integer minor units (`long`), single currency

- **Date:** 2026-07-04
- **Status:** Accepted

## Context

Money arithmetic must be exact. Binary floating point cannot represent common decimal amounts (0.1 + 0.2 ≠ 0.3), and float-money bugs are a classic fintech interview probe. Stripe expresses all amounts as integer minor units. Multi-currency/FX is out of scope.

## Options considered

1. **`double` / `float`** — rejected outright: silent rounding drift, unrepresentable decimal values.
2. **`DECIMAL(19,4)` + `BigDecimal`** — exact and common in ERP systems; but verbose, easy to misuse rounding modes, and mismatched with Stripe's integer amounts.
3. **Integer minor units (`long` / `BIGINT`)** — exact, simple, fast, one-to-one with Stripe amounts; formatting happens only at the display edge.

## Decision

All amounts are `long` minor units in Java and `BIGINT` in MySQL. Field/column names end in `_minor` (e.g., `amount_minor`). Every money-bearing row also stores a `currency` (ISO 4217) column; the system is configured with exactly one allowed currency and validates it at all boundaries. Conversion to major units happens only in the frontend / serialization edge.

**Resolved (2026-07-04):** default currency is **MYR**. The single allowed currency is ISO 4217 `MYR`; the minor unit is the sen (1 MYR = 100 sen), so all `amount_minor` values are denominated in sen.

## Consequences

- ✅ Exact arithmetic; no rounding-mode ceremony; direct parity with Stripe payloads.
- ✅ The currency column is a cheap seam if multi-currency ever returns (it is out of scope now).
- ⚠️ No sub-minor-unit precision (irrelevant here: no FX, no interest accrual).
- ⚠️ Never mix minor and major units — the `_minor` naming convention is the guardrail.

## Revisit when

Multi-currency or interest/fee accrual with sub-cent precision enters scope (both currently excluded).
