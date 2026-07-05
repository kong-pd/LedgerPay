# ADR-0001 — Modular monolith, not microservices

- **Date:** 2026-07-04
- **Status:** Accepted

## Context

LedgerPay is built by one part-time developer over ~18 weeks. Its core domain is a double-entry ledger whose defining invariant — every journal entry's legs commit together or not at all — is trivially guaranteed inside a single database transaction, and notoriously hard across service boundaries (sagas, outboxes, compensation). The owner has already built a microservices project (ColdWatch), so repeating that pattern proves nothing new; demonstrating *architectural judgment* does.

## Options considered

1. **Microservices** — matches prior experience and résumé fashion; but turns atomic ledger posting into a distributed-transaction problem, multiplies ops burden, and is the classic graveyard for solo correctness-critical projects.
2. **Plain monolith** — simplest; but boundaries erode silently and there is no evolution story.
3. **Modular monolith** — one deployable, one database, hard module boundaries in the package structure; keeps single-transaction atomicity and preserves clean seams for future extraction.

## Decision

Modular monolith. Four modules: `payments`, `ledger`, `compliance`, `common`. Cross-module interaction only via Java interfaces (later, application events where appropriate). Ledger tables are written exclusively by the `ledger` module.

## Consequences

- ✅ Balanced ledger posting is one `@Transactional` method — the correctness story stays simple and testable.
- ✅ One thing to run, deploy, and demo.
- ⚠️ Boundary discipline is on us: prefer package-private visibility; consider ArchUnit tests (icebox).
- 🎤 Interview line: "I've built microservices before; here I deliberately chose a monolith because the ledger's invariant demands transactional atomicity — architecture serves correctness."

## Revisit when

Multiple teams, independent scaling needs, or divergent deployment cadences exist — none of which apply to a solo portfolio project.
