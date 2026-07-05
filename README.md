# LedgerPay

A payments + compliance middle platform, built on Stripe **test mode**: payment orchestration backed by a double-entry ledger, with a RegTech compliance layer — KYC onboarding, transaction monitoring with automatic case creation, and a tamper-evident audit chain.

> Solo learning/portfolio project. No real money, no real card data, test-mode keys only.

**Status:** Phase 0 — docs-first, pre-development. See [ROADMAP.md](ROADMAP.md).

## Why this exists

To learn and demonstrate the engineering layer real fintech teams build *above* the card networks: ledger correctness, idempotency, payment state machines, webhook reconciliation, and KYC/AML operations.

## Documentation map

| Doc | Purpose |
|---|---|
| [ROADMAP.md](ROADMAP.md) | Phase plan, milestones, phase rituals |
| [docs/blueprint.md](docs/blueprint.md) | The system in plain words + the five-minute demo scene — read when it feels abstract |
| [docs/scope.md](docs/scope.md) | In/out of scope, scope-change rule, icebox, per-phase Definition of Done |
| [docs/architecture.md](docs/architecture.md) | Component view, module boundaries, design notes |
| [docs/stories.md](docs/stories.md) | Personas and epic-level user stories |
| [docs/adr/](docs/adr/) | Architecture Decision Records (binding) |
| [docs/design/](docs/design/) | Detailed design specs (e.g. ledger-spec.md — DRAFT until audited at phase entry) |
| [CLAUDE.md](CLAUDE.md) | AI-assisted development: context pack, working rules, hard invariants |
| [docs/ai/curriculum.md](docs/ai/curriculum.md) | Learning curriculum + self-check questions per phase |
| [docs/ai/tasks/](docs/ai/tasks/) | Task cards — the unit of work for each AI session |

## Quickstart

_Arrives with the Phase 0 code skeleton: `docker compose up` → app + MySQL + health check._
