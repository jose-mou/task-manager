---
description: Run the full SDD workflow (spec → approval → TDD implementation → review → PR) from a feature prompt
argument-hint: <feature description>
---

Run the project's SDD workflow for this feature request: $ARGUMENTS

You are the orchestrator. Delegate all real work to the project agents and follow the phases IN ORDER. Never skip the approval gate.

## Phase 1 — Specification
Launch the `spec-writer` agent with the feature request. When it returns, show the user the generated spec (full content) and ask for approval.

**STOP here and wait for the user.**
- If the user requests changes, relaunch `spec-writer` with the feedback and ask again.
- Only when the user explicitly approves: update the spec header to `Status: APPROVED` and continue.

## Phase 2 — Implementation (TDD)
Launch the `implementer` agent with the approved spec path. It must follow strict red/green/refactor TDD on a `feat/<spec-name>` branch. Relay its final report (criteria coverage + test results) to the user.

## Phase 3 — Review + PR
Launch the `reviewer` agent with the spec path and the feature branch. It reviews against the spec, applies corrections keeping tests green, and opens the PR with `gh`.

Finish by reporting to the user: the PR URL, the review findings and how they were resolved, and any blocking questions.
