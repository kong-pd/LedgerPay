# ADR-0007 — Account is a balance-free wallet identity

- **Date:** 2026-08-19
- **Status:** Accepted

## Context

Phase 1 introduces the customer-facing Account before Phase 2 introduces LedgerAccount and double-entry postings. Storing a mutable balance in Account would create two competing sources of truth once the ledger exists.

## Options considered

1. A balance-free Account owned by payments, with one later LedgerAccount per Account.
2. Exactly one Account per Customer.
3. Store a mutable balance directly on Account.

## Decision

Account is a customer-facing wallet identity owned by payments.

- A Customer may own multiple Accounts.
- Account stores customer ID, name, status, currency, and timestamps.
- Customer ownership is immutable after Account creation.
- Account status is ACTIVE or SUSPENDED.
- Currency is fixed to MYR.
- Account stores no balance or money amount.
- Names are unique within one Customer.
- The database foreign key uses ON DELETE RESTRICT.
- Phase 2 will provision exactly one LedgerAccount for each Account through a ledger-owned interface.

The domain and API call the resource Account. The persistence table is named `customer_account` to avoid ambiguity with database user accounts and SQL terminology.

## Consequences

- Payments owns the product-facing lifecycle without writing ledger tables.
- The ledger remains the future single source of truth for balances.
- Multiple wallets per Customer remain possible.
- A Customer with Accounts cannot be deleted accidentally.
- Account creation does not provision LedgerAccount until Phase 2.