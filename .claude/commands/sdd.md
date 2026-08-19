---
description: Run the full SDD workflow (spec → approval → OpenAPI contract → red acceptance tests → parallel back/front TDD implementation → parallel reviews → E2E verification → PR) from a feature prompt
argument-hint: <feature description>
---

Run the project's SDD workflow for this feature request: $ARGUMENTS

You are the orchestrator: delegate all real work to the project agents, own the git branch and commits, and follow the phases IN ORDER. Commit after each phase with conventional commits (never Co-Authored-By or AI attribution).

**Degradation rule**: skip any phase the spec makes unnecessary. "API impact: None" → skip api-designer and acceptance-tester; "UI impact: None" → skip frontend-implementer and the frontend reviewer; backend untouched → skip its implementer/reviewer. Never run ceremony the spec does not require.

## Phase 1 — Specification (approval gate)
Launch `spec-writer` with the feature request. Show the user the FULL spec content and ask for approval.

**STOP and wait for the user.**
- Changes requested → relaunch `spec-writer` with the feedback and ask again.
- On explicit approval: set `Status: APPROVED`, create branch `feat/<spec-name>` from `main`, commit the spec (`docs: ...`).

## Phase 2 — API contract
Launch `api-designer` with the spec path. Commit (`feat: ... api contract`).

## Phase 3 — Red acceptance tests
Launch `acceptance-tester` with the spec path. Verify its report shows the new tests red and existing tests green. Commit (`test: ...`).

## Phase 4 — Implementation (parallel, TDD)
Launch `backend-implementer` and `frontend-implementer` **in parallel** (single message, two Task calls), each with the spec path. They touch disjoint directories and do not commit. When both finish, run both suites yourself to confirm green, then commit (`feat: ...`).

## Phase 5 — Review (parallel)
Launch two `reviewer` instances **in parallel**: one with scope `backend`, one with scope `frontend`. When both finish, confirm suites are green and commit corrections (`refactor: ...` / `fix: ...`). Surface any blocking questions to the user before continuing.

## Phase 6 — E2E verification + PR
Launch `e2e-verifier` with the spec path and branch.
- **On failure**: route ONE bounded correction to the implementer of the failing side (backend or frontend), commit the fix, and relaunch `e2e-verifier` ONCE. If it fails again, STOP and escalate to the user with the failure evidence — never loop further.
- **On success**: the verifier opens the PR.

Finish by reporting: the PR URL, per-phase summary, review findings and how they were resolved, and any open questions.
