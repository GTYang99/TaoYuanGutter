# Plan Review

Task: feat-1118-2  
Reviewer: Plan Critic Agent  
Review Iteration: 1  
Review Date: 2026-11-24  
Revision Reviewed: `3f712431f616a00e01bb1d347f23a7a0aee4d498` (`codex/feat/feat-1118-2`)

---

# Summary

## Review Result

- [ ] APPROVED
- [x] REQUEST_CHANGES
- [ ] BLOCKED

## Summary

The plan covers the required 29-source scope and maps AC-001 through AC-011 to implementation and validation work. Implementation should wait for Planning to make the full-versus-district data comparison reproducible and to add deterministic pass/fail assertions for concurrent switching and bounded resource lifecycle. Both issues are resolvable within Planning; no external product decision is required.

---

# Review Checklist

| Item | Result | Notes |
|------|--------|------|
| Requirement understood | PASS | The goal, 29 GPKG roles, district behavior, and host/CI versus emulator validation boundary are represented. |
| Acceptance Criteria complete | PASS | The requirement defines AC-001 through AC-011, and the plan traceability table maps all eleven. |
| Repository analysis complete | PASS | Current behavior and affected modules are identified. The repository contains the three full GPKGs; the two external folders contain the expected 13 district files each. |
| Architecture impact reasonable | PASS | The plan confines changes to the offline data, map overlay, layer UI/state, tests, and task evidence. |
| Affected modules identified | PASS | Assets, data store, tile provider, overlay controller, layer UI, map state, and tests are listed. |
| Dependencies identified | PASS | Source GPKGs, existing staging/index/renderer behavior, preferences, map overlays, and host-side validation tools are listed. |
| Risks evaluated | PASS | Package/storage cost, multi-source load pressure, source inconsistency, generation races, and geometry schema differences are covered. |
| Test Plan complete | FAIL | AC-001 has no deterministic full-versus-district equivalence rule; AC-006/AC-008 lack explicit concurrency and bounded-resource assertions. See Findings 1 and 2. |
| Regression Plan complete | FAIL | Existing map behavior is listed, but the repeated-switch and resource-release checks do not define observable pass/fail assertions. See Finding 2. |
| Open Questions documented | PASS | Product questions are recorded as none; the findings below are planning details that can be resolved from repository and source data. |
| Implementation steps actionable | FAIL | Data inventory is planned, but the equivalence algorithm and runtime lifecycle assertions are not specified enough to produce reproducible evidence. |
| Task size appropriate | PASS | This is a broad feature, but the asset, selection, renderer, and lifecycle changes are coupled by the approved behavior. |
| Rollback strategy (if applicable) | PASS | The plan describes returning to the existing full-GPKG path. |

---

# Findings

## Finding 1

Severity:
- [ ] Critical
- [x] Major
- [ ] Minor
- [ ] Suggestion

Category: Test Plan / Data Integrity

Description: AC-001 and the Knowledge Resolution evidence require host-side validation of all 29 files and consistency between the district sources and the two corresponding full datasets. The plan says to compare the 13 district files with each full source, but does not define a reproducible comparison method or the conditions that make the comparison pass or fail. A manifest of hashes and metadata alone cannot demonstrate that district unions contain the expected features without omissions, duplicates, or incorrect district mappings.

Recommendation: Specify the host-side validator and its invocation, the source-to-district mapping and evidence format, and deterministic pass/fail rules for feature counts and content equivalence. Use an existing stable feature key where available; otherwise define a canonical comparison based on the source schema and normalized geometry/attributes. Include checks for missing, extra, duplicate, or misassigned features, as well as the required table, geometry type, CRS, and packaged-asset mapping.

Planning Response: Pending Planning revision.

Status:
- [x] Open
- [ ] Resolved

---

## Finding 2

Severity:
- [ ] Critical
- [x] Major
- [ ] Minor
- [ ] Suggestion

Category: Test Plan / Concurrency and Resource Lifecycle

Description: AC-006 requires latest-selection atomic switching, while AC-008 requires providers, overlays, and caches not to accumulate without bound. The plan mentions rapid switching, generation checks, bounded cache, release, and recording overlay/memory observations, but does not define deterministic assertions that prove the newest overlapping generation wins, the previous complete result remains until commit, or repeated switching releases resources and keeps counts within a fixed bound.

Recommendation: Add a controlled overlapping-generation test that completes requests out of order and asserts the expected visible generation and absence of duplicate full/district overlays. Add a repeated full/partial/empty selection lifecycle scenario with explicit provider close/cancel assertions and a documented cache/overlay bound that remains stable after release. Record the scenario and expected results in the AC traceability and regression plan.

Planning Response: Pending Planning revision.

Status:
- [x] Open
- [ ] Resolved

---

## Finding 3

Severity:
- [ ] Critical
- [ ] Major
- [ ] Minor
- [x] Suggestion

Category: Evidence Currency

Description: The existing `verification.md` reports a previous nine-criterion mapping, while the revised requirement and plan use AC-001 through AC-011 and add the 26 district GPKGs and source-consistency validation. Earlier execution and verification evidence should not be mistaken for evidence against this revised scope.

Recommendation: When implementation and independent verification resume, clearly supersede the earlier report and produce evidence against the current eleven acceptance criteria and fixed revision.

Planning Response: Pending Planning revision.

Status:
- [x] Open
- [ ] Resolved

---

# Blocking Issues

- ISS-001: Full-versus-district source equivalence has no reproducible pass/fail method.
- ISS-002: Latest-generation switching and bounded resource lifecycle have no deterministic assertions.

# Improvement Suggestions

- Treat the existing execution and verification reports as historical evidence for the earlier scope; replace or explicitly supersede them when verifying the revised eleven-AC requirement.

---

# Decision

## REQUEST_CHANGES

Planning should update the implementation plan with the missing validation definitions and resubmit it for review. No external clarification is needed.

---

# Next Action

- [x] Planning
- [ ] Implementation
- [ ] Requirement Clarification
- [ ] Human Review

---

# State Update

```yaml
phase: plan_review
status: changes_requested
next_action: planning
plan_review:
  result: REQUEST_CHANGES
  iteration: 1
```

---

# Review Notes

- Review was performed in the dedicated worktree on the task branch at `3f712431f616a00e01bb1d347f23a7a0aee4d498`; the worktree was clean before review artifacts were written.
- The source folders referenced by the requirement were present and contained 13 possible-gutter district GPKGs and 13 existing-gutter district GPKGs.
- No tests or builds were run; this was a plan review. `git diff --check` passed for the tracked state update.

---

# Definition of Done

- [x] All checklist items reviewed
- [x] Findings documented
- [x] Blocking Issues identified
- [x] Improvement Suggestions separated
- [x] Decision recorded
- [x] state.yaml updated

---

# Re-review Addendum

Review Iteration: 2  
Revision Reviewed: `3f712431f616a00e01bb1d347f23a7a0aee4d498` plus Planning artifact revisions  
Review Date: 2026-11-24

## Planning Responses

### ISS-001 — Full and district GPKG consistency

Status: Resolved.

`plan.md` now defines a host-side validator invocation and deterministic comparison:

- reads all 29 files' metadata, hashes, sizes, counts, CRS, geometry types, bounds, and mappings;
- canonicalizes decoded geometry plus non-geometry attributes into row signatures without relying on district-local `fid`;
- compares each full-source district-filtered signature multiset with its 13 district-source multiset;
- fails on missing, extra, duplicate, or misassigned signatures and records the diff in manifest evidence.

This makes AC-001 reproducible on the development machine/CI and keeps the phone/emulator limited to package and runtime behavior checks.

### ISS-002 — Latest-generation switching and resource lifecycle

Status: Resolved.

`plan.md` now requires:

- a controlled deferred-loader test where generation 2 completes before generation 1 and must remain the only committed visible generation;
- an assertion that the previous complete result remains visible until the new generation commits;
- a fixed 20-round full/district/empty lifecycle scenario;
- active overlay/provider bounds per current mode, cache bound of `48 * active provider count`, and zero provider/overlay/cache counts after release.

The regression plan and AC traceability now point to these deterministic assertions rather than observation-only evidence.

### Finding 3 — Historical evidence currency

Status: Resolved as a planning control.

The revised plan explicitly treats earlier execution and verification reports as historical context. New evidence must be produced against the revised 11-AC requirement and the fixed implementation revision.

## Re-review Checklist

| Item | Result | Notes |
|---|---|---|
| Requirement understood | PASS | 29-source full/district scope and validation boundary are explicit. |
| Acceptance Criteria complete | PASS | AC-001–AC-011 remain unchanged and traceable. |
| Repository analysis complete | PASS | Existing three-source implementation and required new source seams are identified. |
| Architecture impact reasonable | PASS | Changes remain limited to offline sources, overlays, UI state, tests, and evidence. |
| Affected modules identified | PASS | Source manifest, data store, provider, controller, state, and tests are covered. |
| Dependencies identified | PASS | Source directories, existing renderer, host validator, emulator, and cache lifecycle are covered. |
| Risks evaluated | PASS | Asset, concurrency, schema, source mismatch, and lifecycle risks are addressed. |
| Test Plan complete | PASS | Host-side equivalence and deterministic concurrency/resource tests are specified. |
| Regression Plan complete | PASS | Fixed out-of-order and 20-round lifecycle assertions are specified. |
| Open Questions documented | PASS | No unresolved product questions. |
| Implementation steps actionable | PASS | Validator, signatures, fallback, generation, and resource bounds are actionable. |
| Scope appropriate | PASS | 29-source scope follows approved decisions; unrelated behavior is excluded. |
| Rollback exists | PASS | Reversion to three full-GPKG path is documented. |

## Final Decision

- [x] APPROVED
- [ ] REQUEST_CHANGES
- [ ] BLOCKED

Implementation may begin after this fixed plan-review revision. Production code was not modified during Plan Critic.
