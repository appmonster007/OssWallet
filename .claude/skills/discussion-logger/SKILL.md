---
name: discussion-logger
description: Capture the outcomes and full reasoning of a discussion into a structured, version-aware Markdown file. Use this skill whenever the user wants to record, save, log, or document the result of a discussion, design conversation, brainstorming session, architecture debate, or any decision-making exchange — including phrases like "save this discussion", "write up our decision", "log the reasoning", "record what we decided", "document this design choice", or "update the discussion file". ALSO use it when the user passes in an existing discussion .md file and wants to continue, refine, revisit, or revise the reasoning, even if they don't say the word "skill". Each file tracks ONE ongoing topic over time, preserving superseded decisions as history rather than deleting them. Trigger this whenever a conversation has produced a decision, conclusion, logic choice, or feature direction worth persisting.
---

# Discussion Logger

Persist the *outcome and the complete reasoning* of a discussion into a highly structured Markdown file under `discussions/` in the project root. One file = one ongoing topic, refined over many sessions. Superseded decisions are never deleted — they are marked deprecated and kept as an audit trail, with the new direction flagged as the active strategic choice.

## When to use

- The user asks to save / log / document / write up a discussion or decision.
- The user provides an existing discussion file and wants to continue or revise it.
- A conversation has reached a conclusion (a decision, a chosen approach, a defined logic, a feature direction) that should be persisted with its rationale.

## Core principles

1. **Capture complete reasoning.** Every entry must give the full picture: *why*, *what*, *how*, *where*, *when*, *who*, and the *alternatives considered* and *why they were rejected*. The reader should never have to ask "but why did we do this?" — the file answers it.
2. **One topic per file, continuously refined.** Do not start a new file for the same topic. Continue the existing one.
3. **Preserve history; never overwrite reasoning.** When a prior decision changes, mark the old entry as `DEPRECATED` (or `SUPERSEDED`) — do not delete it. Add the new decision as the `ACTIVE` strategic choice and link the two so the evolution is traceable.
4. **Highly structured and consistent.** Every file follows the same template (below) so files are scannable and machine-parseable.

## Workflow

### Step 1 — Determine new file vs. continuation

- If the user passed an existing discussion `.md` file (or one exists in `discussions/` matching the topic): **read it fully first** with the `view` tool before writing anything. Treat its contents as authoritative context.
- Otherwise, this is a new discussion file.

Run `ls discussions/ 2>/dev/null` from the project root to check for an existing directory and related files. If the user named a topic that closely matches an existing file, ask the user whether to continue that file rather than creating a duplicate.

### Step 2 — Set up location

Ensure a `discussions/` directory exists in the **project root**:

```bash
mkdir -p discussions
```

Filename: `discussions/<kebab-case-topic>.md` (e.g. `discussions/auth-token-strategy.md`). If continuing, reuse the existing filename exactly.

### Step 3 — Extract the reasoning from the conversation

Before writing, mentally assemble, for each decision/conclusion reached in the discussion:

- **What** was decided / concluded / chosen.
- **Why** — the driving rationale, constraints, goals, and trade-offs.
- **How** — the mechanism, approach, or implementation path.
- **Where** — the part of the system / scope it applies to.
- **When** — timing, sequencing, triggers, or conditions; plus the date it was decided.
- **Who / context** — who raised it or what prompted it, if relevant.
- **Alternatives considered** and **why rejected**.
- **Open questions / risks**, if any.

Do not invent reasoning that wasn't present. If a field genuinely has no information, write `_Not discussed._` rather than fabricating. (Capture all reasoning that *exists*; never hallucinate to fill a slot.)

### Step 4 — Write or update the file

Use the template in `references/template.md`. Read it now if you have not already:

- **New file:** instantiate the full template, fill the metadata header, add the first decision entry/entries.
- **Continuation — new decision:** append a new decision entry under "## Decisions" with the next sequential ID and status `ACTIVE`. Update the "Decision Log Summary" table and the "Last updated" metadata.
- **Continuation — revising a past decision:** do **not** edit the old entry's reasoning. Instead:
  1. Change the old entry's status to `DEPRECATED` (or `SUPERSEDED`), add a `Superseded by: D-00X` line, and add a one-line note on *why* it no longer holds.
  2. Add a new entry with the next ID, status `ACTIVE`, including a `Supersedes: D-00Y` line and full reasoning for the change.
  3. Update the summary table and metadata.

Always re-read the file region you are about to edit immediately before editing (prior views may be stale).

### Step 5 — Confirm

Briefly tell the user what was written/updated (which decision IDs, new vs. superseded), and the file path. Do not dump the whole file back.

## Status vocabulary

- `ACTIVE` — the current strategic choice in force.
- `DEPRECATED` / `SUPERSEDED` — preserved for history; no longer valid. Must point to the entry that replaced it.
- `PROPOSED` — under discussion, not yet adopted.
- `REJECTED` — considered and declined (keep for the record).

## Reference

- `references/template.md` — the canonical file structure. Always conform to it.
