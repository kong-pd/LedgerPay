# LedgerPay

A payments + compliance middle platform, built on Stripe **test mode**: payment orchestration backed by a double-entry ledger, with a RegTech compliance layer — KYC onboarding, transaction monitoring with automatic case creation, and a tamper-evident audit chain.

> Solo learning/portfolio project. No real money, no real card data, test-mode keys only.

**Status:** Phase 0 done — runnable backend skeleton. See [ROADMAP.md](ROADMAP.md).

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

Prerequisites:

- Docker Desktop
- Java 21 and Maven 3.9+ for local `mvn -B verify`

Create a local environment file:

Windows PowerShell:

```powershell
Copy-Item .env.example .env
```

macOS/Linux:

```bash
cp .env.example .env
```

Start MySQL and the Spring Boot app:

```powershell
docker compose up
```

Check application health:

```powershell
Invoke-RestMethod http://localhost:8080/actuator/health
```

Expected response:

```json
{"status":"UP"}
```

Run the verification suite:

```powershell
mvn -B verify
```

Everyday IDE loop:

1. Start only MySQL with Docker Compose:

   ```powershell
   docker compose up mysql
   ```

2. Run `LedgerPayApplication` from the IDE.
3. Use `jdbc:mysql://localhost:3306/ledgerpay` from the host machine.

Inside Docker Compose, the app uses `jdbc:mysql://mysql:3306/ledgerpay`; from the IDE, the app uses `localhost:3306` because it is running on the host.
