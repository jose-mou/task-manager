# task-manager

## Development methodology

This project works with **Spec Driven Development (SDD)** and **strict TDD**. All feature work follows the three-phase pipeline, started with the `/sdd <feature description>` command:

1. **Specification** — the `spec-writer` agent turns the user prompt into a simple spec under `specs/`. Nothing is implemented until the user approves the spec (`Status: APPROVED`).
2. **Implementation** — the `implementer` agent implements the approved spec on a `feat/<spec-name>` branch using strict TDD (red → green → refactor; no production code without a failing test first).
3. **Review + PR** — the `reviewer` agent reviews the implementation against the spec, applies corrections keeping tests green, and opens a PR to `main` with `gh`.

Rules:
- Specs live in `specs/`, written in English, one file per feature.
- Every acceptance criterion in a spec must map to an automated test.
- Conventional commits only; never add Co-Authored-By or AI attribution.
- Do not merge to `main` directly; all changes land via PR from the pipeline.
