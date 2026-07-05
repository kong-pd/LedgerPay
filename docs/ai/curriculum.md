# Learning curriculum & self-checks

**Rule:** every phase opens with a concept course. The owner *passes* a concept by ① restating it in plain words, ② writing the related DoD / acceptance criteria / doc section, ③ answering the self-checks below without notes.

**To any AI assistant teaching from this file:** plain words first, one concept at a time, concrete example before terminology, ask the owner to restate before moving on. Do not proceed to code while a concept on the current card is still unclear.

## Phase 1 — REST & Spring foundations
**Concepts:** Controller–Service–Repository layering · JPA entity lifecycle basics · DTO vs entity · bean validation · global error handling · REST resource design & status codes · pagination.
**Self-checks:**
- Why do we never return a JPA entity from a controller? Name two concrete bugs it causes.
- What should a failed validation return, and what belongs in the error body?
- What is the N+1 query problem, in one sentence?

## Phase 2 — Ledger core (the dense one)
**Concepts:** double-entry bookkeeping (debits/credits, why every movement has two legs, what "the books balance" means) · append-only + reversal entries · ACID & `@Transactional` · idempotency (key design, unique constraints, response replay) · state machines (states, guarded transitions, transition log).
**Self-checks:**
- Why is editing a wrong ledger row worse than posting a reversal entry?
- A client's payment request times out and it retries — walk through exactly what the server does at every layer so money moves once.
- Two identical requests arrive in the same millisecond. What *guarantees* a single posting? (The answer must involve a database constraint, not just code.)
- Mid-posting, the second leg's insert throws an exception. What is in the database afterwards, and why?
- Name one transition your payment state machine must forbid, and what the API returns when it is attempted.

## Phase 3 — Stripe, webhooks, reconciliation
**Concepts:** PaymentIntent lifecycle · hosted card fields (why card data never touches us — the PCI story) · webhook signature verification · duplicate & out-of-order delivery · reconciliation as "trust but verify".
**Self-checks:**
- Why must the webhook handler be idempotent even though the API layer already handles idempotency?
- Stripe says a payment succeeded but our ledger has nothing. Give two plausible causes and what reconciliation does about each.
- What attack does webhook signature verification stop?

## Phase 4 — Panel & auth
**Concepts:** JWT anatomy (header/payload/signature; what is *not* encrypted) · token storage trade-offs · CORS (what it protects and why the browser enforces it) · role-based access.
**Self-checks:**
- Can a user read the contents of their JWT? Can they modify it? Why?
- Your React app gets a CORS error but the same request works in curl. Explain to a junior what is happening.

## Phase 5 — KYC
**Concepts:** KYC/CDD and why regulated institutions must do it · onboarding state machine · reason codes · data-minimisation instinct (fake documents only; PDPA awareness).
**Self-checks:**
- Why does the business gate ("no trading before KYC approval") live in the backend, not the UI?
- What must an approve/reject action leave behind for an auditor?

## Phase 6 — Monitoring, cases, audit chain
**Concepts:** AML typologies (large amount, velocity, structuring/smurfing, watchlists) · alert → case lifecycle · false-positive economics · STR & JFIU (HK terms) · hash chain = tamper-**evident**, not tamper-proof.
**Self-checks:**
- Explain "structuring" and how a rule catches amounts sitting *just under* a threshold.
- Someone edits an old audit row directly in MySQL. What exactly breaks, and how does the verify endpoint expose it?
- Why is "tamper-evident, not tamper-proof" the honest claim, and what would real systems add? (external anchoring)

## Phase 7 — Ship it
**Concepts:** multi-stage Docker builds · env-based configuration · structured logs & Actuator · cost guardrails (budget alarms, stop when idle).
**Self-checks:**
- Why is the final image smaller than the build image, and why does that matter?
- Your live demo dies mid-interview. What is the fallback plan? (recorded demo)
