# Task card template

One card = one AI session = one reviewable chunk (target ≤ ~2 h of owner time). Copy into `docs/ai/tasks/PX-NN-slug.md`. Cards are written at phase entry; the assistant may refine steps but may **not** silently expand scope.

---

## PX-NN — Title

**Phase:** X · **Prereq cards:** … · **New concepts (teach first — see curriculum):** …
**Context to load:** `CLAUDE.md` (always) + [specific docs/ADRs]

### Goal
One or two sentences: what exists after this card that did not before.

### In scope
- …

### Out of scope
- … (anything discovered mid-card goes to `docs/scope.md` Icebox)

### Test specs — for correctness-critical work, write these tests FIRST, get owner approval, then implement
- [ ] Given / when / then, with concrete values
- [ ] At least one adversarial case (duplicate, concurrency, illegal transition, tampering — whatever fits)

### Steps sketch
1. …

### Acceptance criteria — owner verifies by *running*, not by reading
- [ ] Command/request X produces Y
- [ ] All tests green locally and in CI

### Handback checklist
- [ ] `CLAUDE.md` §6 Current state updated
- [ ] `ROADMAP.md` progress ticked (if milestone)
- [ ] Any new decision → draft ADR (never decided silently)
- [ ] **Adversarial self-review:** assistant lists 3 plausible ways this change could violate `CLAUDE.md` §4 invariants and shows why it does not — or fixes it
