# AI Session Prompt Kit

> **Principle: the repository is the handoff. Prompts only point the model's face at it.**
> Never rely on a model "remembering" past chats, and never paste old chat transcripts as context — they drift, bloat, and carry stale decisions. The current truth is always: `CLAUDE.md` + the current task card + the docs the card lists.

## Recommended setup (claude.ai)

Create a **Project** named *LedgerPay*. Put into its project knowledge: `CLAUDE.md`, `ROADMAP.md`, `docs/scope.md`, plus the **current phase's** spec and cards. Set the project instructions to one line: *"Follow CLAUDE.md strictly; it is binding."* Every new chat in the project then starts pre-loaded. **Refresh the knowledge files after each phase tag** — stale knowledge is worse than none. Without Projects, attach/paste the same files at the top of each session. (If you ever switch to Claude Code, `CLAUDE.md` at repo root is read automatically.)

## 0. What you literally type in the input box

**With the Project set up** (knowledge holds CLAUDE.md + current cards), the first message of a session is short — four jobs only: point at the binding file, name the card, paste the one thing that changes (§6), kick off:

```text
读 project knowledge 里的 CLAUDE.md,它是约束性文件,严格执行其中 §2 会话协议
和 §4 硬性不变量。本次只做这张任务卡:docs/ai/tasks/<CARD>.md(也在 knowledge
里),不越界。当前状态(CLAUDE.md §6 原文):<粘贴 §6>。
从卡上列的概念小课开始教我,中文对话,产物英文。会话结束时给我三样:§6 的更新
文本、commit message、对照 §4 的对抗性自检。
```

**Without a Project:** attach the files (CLAUDE.md + the card + the docs it lists) and paste **Template 1 below verbatim** as the message body. The long form is *more* reliable than the short one — restating the rules in the prompt itself raises compliance even when the files are attached. Use the short form only when Project instructions already carry the rules.

## Template 1 — Execute a task card (the everyday session)

```text
You are working on LedgerPay. Read CLAUDE.md fully first — it is binding,
especially §2 (session protocol) and §4 (hard invariants). Then read the task
card below and the docs it lists under "Context to load".

Rules for this session:
- Work ONLY within this card's scope. Discoveries outside it → docs/scope.md Icebox.
- Teach each "new concept" on the card before writing code for it; make me restate it.
- For correctness-critical work: write the card's test specs as real tests FIRST,
  show me, wait for my approval, then implement.
- If you need a decision not covered by an ADR: STOP and present options + a
  recommendation as a draft ADR. Never decide silently.
- Conversation in Chinese; all artifacts in English.
- End of session: give me (a) the CLAUDE.md §6 "Current state" replacement text,
  (b) a commit message, (c) your adversarial self-review against §4.

Current state (from CLAUDE.md §6): <paste §6 here>
Task card: <paste card / attach file>
Begin with the concept mini-course.
```

## Template 2 — Concept course (phase entry, step ①)

```text
Teach me the Phase <N> concepts listed in docs/ai/curriculum.md (attached),
one concept at a time: plain words first, a concrete example with MYR sen
amounts, THEN the terminology. Make me restate each concept in my own words
before moving to the next — do not accept vague restatements. Finish by asking
me the phase's self-check questions and grade my answers strictly; if I fail
one, reteach it differently. Conversation in Chinese; terms in English.
Afterwards, help me draft this phase's Definition of Done for docs/scope.md
in my own words — do not write it for me.
```

## Template 3 — Spec audit (phase entry, step ② — Phases 2/3 have DRAFT specs)

```text
I have completed the Phase <N> concept course. Walk me through the audit
checklist in <spec file> (§12 for ledger-spec / §6 for the P3 doc), one item
at a time. For each item, make ME answer first, then critique my answer. Where
I disagree with the spec, help me draft the amendment + ADR change. At the
end, list what flips from Proposed to Accepted, and generate the exact file
edits.
```

## Template 4 — Milestone audit (where strong-model credits go)

Spend premium-model usage here — hunting bugs is higher leverage per token than writing code.

```text
Act as a hostile senior fintech reviewer of LedgerPay Phase <N>.
Inputs: CLAUDE.md, the phase spec, the diff / key source files, the phase DoD.
Hunt specifically for:
1. Violations of CLAUDE.md §4 hard invariants (worst possible failure).
2. Silent deviations from the spec, and silent scope additions.
3. Tests that are missing, weakened, or that pass without proving the invariant
   they claim to prove.
4. Concurrency/atomicity holes (idempotency, locking, isolation levels, races).
5. Anything a Hong Kong fintech interviewer would grill me on.
Output: ordered findings with severity, exact file/line, and the concrete fix.
Do not be polite. Do not summarize what is fine.
```

## Session hygiene (the rules that keep cheap-model sessions safe)

1. **One card = one session.** Card done → new session. Fresh context beats a long degraded one.
2. **The document wins.** If the model contradicts an ADR/invariant, correct it once; if it keeps drifting, end the session and start clean — arguing with drift wastes the context window.
3. **You run everything.** Acceptance = the card's checkboxes tick on *your* machine, not the model's confidence.
4. **Close the loop.** No session ends without the §6 update text and a commit. The repo is the memory; an unclosed session is amnesia.

## Red flags a session has gone wrong

Proposes a new library without an ADR · suggests "simplifying" by updating/deleting ledger rows · skips writing tests first "to save time" · hand-waves a concept it was supposed to teach · produces code touching files outside the card's scope · says "this should work" instead of pointing at a passing test.
