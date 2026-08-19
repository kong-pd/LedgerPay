# ADR-0006 — Payments owns customers and accounts

- **Date:** 2026-08-19
- **Status:** Accepted

## Context

Phase 1 introduces Customer and Account CRUD before the ledger and compliance workflows exist. ADR-0001 fixes the modular monolith at four modules (`payments`, `ledger`, `compliance`, and `common`), but it does not assign ownership of Customer or Account. Leaving ownership implicit would make later KYC gating and ledger-account provisioning prone to circular dependencies.

## Options considered

1. **Payments owns Customer and Account** — keeps the payment-facing customer and wallet-account lifecycle together. Compliance owns only KYC decisions, while ledger owns only accounting records.
2. **Compliance owns Customer and Account** — aligns Customer with future KYC, but makes basic payment flows depend on a module that is not introduced until Phase 5 and encourages a payments/compliance cycle.
3. **Create a fifth customer module** — gives customer identity an independent boundary, but expands the accepted architecture and scope before Checkpoint A.

## Decision

The `payments` module owns Customer, Account, and their REST APIs.

- `ledger` will own `LedgerAccount` and expose a Java interface for accounting-account provisioning when Phase 2 introduces that model.
- `compliance` will own KYC records and later implement a transaction-eligibility interface consumed by `payments`.
- Modules exchange identifiers and purpose-built DTOs/interfaces, not JPA entities or repositories.
- Phase 1 Account creation does not write ledger tables; that integration is deliberately deferred to the audited Phase 2 design.

## Consequences

- Customer and Account CRUD can be completed without inventing a new module.
- The ledger remains the sole writer of ledger tables.
- KYC can be added without moving the core Customer record into compliance.
- Future cross-module interfaces must be designed to keep dependency direction acyclic.

