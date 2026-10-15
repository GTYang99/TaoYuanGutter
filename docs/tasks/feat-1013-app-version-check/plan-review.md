# Current Plan Review — Iteration 5

Task: feat-1013-app-version-check  
Reviewer: Plan Critic Agent  
Review Iteration: 5  
Review Date: 2026-10-13

## Review Result

- [x] APPROVED
- [ ] REQUEST_CHANGES
- [ ] BLOCKED

## Summary

The AC-001 sequence now matches the version-counter invariant: the injected failed Release and debug build run first from P0=1 and leave it unchanged; successful Release builds then produce code 2/P0=2 and code 3/P0=3. The rollback boundary also preserves version-code calculation/persistence and signing while reverting the optional updater. The plan is ready for implementation.

## Review Checklist

| Item | Result | Notes |
|------|--------|------|
| Requirement understood | PASS | Optional updates, login/offline access, manual version-name parts, and monotonic Release codes are preserved. |
| Acceptance Criteria complete | PASS | AC-001 through AC-011 are measurable and mapped to implementation and validation. |
| Repository analysis complete | PASS | Baseline build configuration and current API, lifecycle, FileProvider, and CI structure were checked. |
| Architecture impact reasonable | PASS | The app-level lifecycle coordinator fits the existing Activity/coordinator architecture. |
| Affected modules identified | PASS | Gradle, API, lifecycle/update flow, manifest, FileProvider paths, strings, tests, and CI are covered. |
| Dependencies identified | PASS | Test API, Android installer, emulator, and external JKS are identified. |
| Risks evaluated | PASS | Release task wiring, URL validation, lifecycle, signing, rollback, and production endpoint risks are documented with validation or release gates. |
| Test Plan complete | PASS | AC-001 now tests failure/debug at P0=1 before successful builds consume codes; all update and failure cases have planned evidence. |
| Regression Plan complete | PASS | Login, offline entry, target selection, Activity transitions, forms/photos, and FileProvider use are covered. |
| Open Questions documented | PASS | No test-environment decision remains open; production endpoint replacement is a pre-production release gate. |
| Implementation steps actionable | PASS | Success-gated P0 persistence, strict HTTPS/version validation, lifecycle handling, and installer handoff are explicit. |
| Task size appropriate | PASS | Version management and update installation are one cohesive feature. |
| Rollback strategy (if applicable) | PASS | Rollback preserves the version-code mechanism, signing configuration, and consumed P0; AC-011 verifies the next code. |

## Re-review of Finding 6

Resolved. The Test Plan and AC-001 table now use the same sequence: begin at P0=1; run failed Release and debug builds and verify P0 remains 1; build Release code 2 and persist P0=2; then build code 3 and persist P0=3. No consumed value is reset.

## Blocking Issues

- None.

## Improvement Suggestions

- None. The production endpoint prerequisite is recorded as a pre-production release gate.

## Decision

### APPROVED

Implementation may begin.

## Next Action

- [ ] Planning
- [x] Implementation
- [ ] Requirement Clarification
- [ ] Human Review

## State.yaml Update

```yaml
phase: plan_review
status: approved
plan_review:
  result: APPROVED
  iteration: 5
next_action: implementation
```

## Review Notes

This iteration re-read the current requirement, analysis, plan, state, repository instructions, and Plan Critic rules on `codex/feat-1013-app-version-check`. It independently confirmed that AC-001 and the rollback procedure are internally consistent. No production source or test files were modified; no tests were run because this was a planning review. This approval supersedes the prior Iteration 4 decision.

---

# Prior Review History

Iterations 4 and earlier are retained below for traceability. The Iteration 4 `REQUEST_CHANGES` decision is superseded by this Iteration 5 approval.

# Current Plan Review — Iteration 4

Task: feat-1013-app-version-check  
Reviewer: Plan Critic Agent  
Review Iteration: 4  
Review Date: 2026-10-13

## Review Result

- [ ] APPROVED
- [x] REQUEST_CHANGES
- [ ] BLOCKED

## Summary

The rollback plan now correctly preserves version-code calculation/persistence and signing while reverting the optional updater. One validation sequence remains contradictory: it first consumes code 2 and sets P0=2, then requires a later failed build to leave P0 at 1, while also forbidding resetting a consumed P0. Reorder or split the scenarios so each expected property value matches its actual starting value.

## Review Checklist

| Item | Result | Notes |
|------|--------|------|
| Requirement understood | PASS | Optional updates, login/offline access, and monotonic release version codes are preserved. |
| Acceptance Criteria complete | PASS | AC-001 through AC-011 are measurable and map to implementation/validation. |
| Repository analysis complete | PASS | Confirmed the baseline Gradle config maps P0 directly to versionCode, which the revised rollback now explicitly excludes. |
| Architecture impact reasonable | PASS | The app-level lifecycle coordinator fits the existing Activity/coordinator architecture. |
| Affected modules identified | PASS | Gradle, API, lifecycle/update flow, manifest, FileProvider paths, strings, tests, and CI are covered. |
| Dependencies identified | PASS | Test API, Android installer, emulator, and external JKS are identified. |
| Risks evaluated | PASS | Gradle task behavior, URL validation, lifecycle, signing, and rollback boundaries are documented. |
| Test Plan complete | FAIL | The ordered Release test steps require P0=1 after a successful build has already advanced it to 2. |
| Regression Plan complete | PASS | Login, offline entry, target selection, Activity transitions, forms/photos, and FileProvider use are covered. |
| Open Questions documented | PASS | No implementation decision remains open; production endpoint replacement is a pre-production release gate. |
| Implementation steps actionable | PASS | The success-gated build action and strict response validation are explicit. |
| Task size appropriate | PASS | Version management and update installation are one cohesive feature. |
| Rollback strategy (if applicable) | PASS | The revised rollback retains version-code calculation/persistence, signing, and consumed P0. |

## Finding 6

Severity:
- [ ] Critical
- [x] Major
- [ ] Minor
- [ ] Suggestion

Category: Test plan sequencing and version-counter invariant

Description: The Release build test in `plan.md` first successfully builds from P0=1, producing code 2 and persisting P0=2. It then says to inject a failed Release build and prove P0 remains 1. Those steps cannot both occur in the same copy without resetting a consumed version code, which the plan explicitly prohibits. The AC-001 table repeats the success-before-failure sequence, so the expected unchanged value is ambiguous there as well.

Recommendation: Reorder the single-copy scenario: start at P0=1, run injected-failure Release and debug checks (both leave P0=1), then successfully assemble code 2 (P0=2), followed by the next successful Release (code 3/P0=3). Alternatively, use clearly named independent copies and state each copy's initial and expected P0. Update both the Test Plan bullets and AC-001 table; do not reset P0 after a successful artifact.

Planning Response: AC-001 now uses one isolated copy in a monotonic order without resetting P0: start at 1; run injected Release failure and debug build (P0 stays 1); successfully assemble code 2 (P0 becomes 2); then successfully assemble code 3 (P0 becomes 3). P1/P2/P3 are checked throughout. Both the Test Plan prose and AC-001 table use this same order.

Status:
- [ ] Open
- [x] Resolved

## Planning Revision Submitted After Iteration 4

The contradictory AC-001 sequence is corrected in both the Test Plan and acceptance table. Failure and debug checks now occur while P0 is 1, before any successful Release consumes a version. Successful builds then advance it in order from 1 to 2 and from 2 to 3. The plan expressly forbids resetting P0 after consumption. This revision is ready for Plan Review iteration 5; Finding 6 has not yet been independently re-reviewed.

## Blocking Issues

- Make the AC-001 release/failure/debug test order consistent with the P0 values and the no-reset invariant.

## Improvement Suggestions

- None.

## Decision

### REQUEST_CHANGES

Planning should correct the test sequence and resubmit for review. Implementation must not begin while this validation gap remains open.

## Next Action

- [x] Planning
- [ ] Implementation
- [ ] Requirement Clarification
- [ ] Human Review

## State.yaml Update

```yaml
phase: plan_review
status: changes_requested
plan_review:
  result: REQUEST_CHANGES
  iteration: 4
next_action: planning
```

## Review Notes

This iteration re-read the current state, requirement, analysis, plan, repository instructions, and Plan Critic rules on `codex/feat-1013-app-version-check`. It independently confirmed the rollback boundary now preserves the release version-code mechanism and signing settings. No production source or test files were modified; no tests were run because this was a planning review. This review supersedes the prior Iteration 3 decision.

---

# Prior Review History

The prior Iteration 3 review and earlier findings are retained below for traceability. The Iteration 3 `REQUEST_CHANGES` decision is superseded by this Iteration 4 review.

# Current Plan Review — Iteration 3

Task: feat-1013-app-version-check  
Reviewer: Plan Critic Agent  
Review Iteration: 3  
Review Date: 2026-10-13

## Review Result

- [ ] APPROVED
- [x] REQUEST_CHANGES
- [ ] BLOCKED

## Summary

The revised plan resolves the prior version-persistence and HTTPS-validation findings. This re-review found one remaining rollback inconsistency: the rollback test says to revert source/config changes while retaining only version properties, but the repository baseline uses P0 directly as `versionCode`. After a successful build consumes code 2, reverting the increment logic and keeping P0=2 would make the next APK code 2 again, contrary to AC-011. Planning should preserve an increment mechanism during rollback and test that exact rollback path before implementation begins.

## Review Checklist

| Item | Result | Notes |
|------|--------|------|
| Requirement understood | PASS | The plan retains optional updates, login/offline access, and current-screen state. |
| Acceptance Criteria complete | PASS | AC-001 through AC-011 are measurable and map to validation. |
| Repository analysis complete | PASS | Confirmed the baseline `app/build.gradle.kts` assigns `versionCode = getVersionCode()` directly at line 42. |
| Architecture impact reasonable | PASS | The app-level lifecycle coordinator fits the existing Activity/coordinator architecture. |
| Affected modules identified | PASS | Gradle, API, lifecycle/update flow, manifest, FileProvider paths, strings, tests, and CI are covered. |
| Dependencies identified | PASS | Test API, Android installer, emulator, and external JKS are identified. |
| Risks evaluated | PASS | Release task wiring, URL validation, lifecycle, signing, and rollback risks are addressed or explicitly tested; rollback has the finding below. |
| Test Plan complete | FAIL | AC-011's rollback steps remove the version increment configuration, so the expected strictly higher APK code is not achievable from the stated retained properties alone. |
| Regression Plan complete | PASS | Login, offline entry, target selection, Activity transitions, forms/photos, and FileProvider use are covered. |
| Open Questions documented | PASS | No implementation decision remains open; production endpoint replacement is a pre-production release gate. |
| Implementation steps actionable | FAIL | The forward Release path is actionable, but the rollback procedure does not retain a mechanism for subsequent version-code increments. |
| Task size appropriate | PASS | Version management and update installation are one cohesive feature. |
| Rollback strategy (if applicable) | FAIL | It preserves the counter value but, as written, can remove the code that advances it. |

## Finding 5

Severity:
- [ ] Critical
- [x] Major
- [ ] Minor
- [ ] Suggestion

Category: Rollback and version-code monotonicity

Description: AC-011 and the rollback plan require the next Release after rollback to use a strictly higher code. However, AC-011 says to revert feature source/config changes while retaining version properties, and the rollback plan reverts this task's feature code. The current baseline Gradle configuration assigns `versionCode = getVersionCode()` directly. For example, after code 2 is successfully assembled and P0 becomes 2, reverting the task's `app/build.gradle.kts` changes while keeping P0=2 makes the next APK code 2 again. Retaining the property value alone does not satisfy AC-011.

Recommendation: Specify that rollback retains the Release version-code increment and persistence mechanism, even if the optional update-flow code is reverted, or define another rollback configuration that guarantees the next APK code is greater than every consumed code. Revise AC-011's steps to use the same file/change scope as the rollback plan and verify the produced APK metadata is strictly higher.

Planning Response: The rollback boundary now explicitly reverts only the optional updater flow and preserves `app/build.gradle.kts` Release version-code calculation/success-gated persistence, Release signing configuration, and the consumed P0 value in `app/version.properties`. AC-011 now tests this exact scope: after code 2 is consumed, rollback the updater while preserving the release controls, then verify the next signed APK is code 3 and P0 becomes 3. The pre-task baseline is expressly excluded because it directly maps P0 to versionCode.

Status:
- [ ] Open
- [x] Resolved

## Planning Revision Submitted After Iteration 3

The rollback plan now defines the same boundary as AC-011: revert the optional updater flow while preserving the Release version-code calculation and success-gated persistence, Release signing configuration, and consumed `P0_VERSION_CODE`. The test starts from P0=1, builds code 2, performs that scoped rollback, then verifies the next signed Release is code 3 and P0 becomes 3. The repository baseline's direct `versionCode = getVersionCode()` configuration is explicitly excluded from rollback. This revision is ready for Plan Review iteration 4; Finding 5 has not yet been independently re-reviewed.

## Blocking Issues

- Make the rollback procedure retain or replace the version-code increment mechanism, then test the actual rollback scope and verify the next APK's code is strictly higher.

## Improvement Suggestions

- None.

## Decision

### REQUEST_CHANGES

Planning should revise the rollback strategy and AC-011 test procedure, then submit the plan for review again. Implementation must not begin while this issue remains open.

## Next Action

- [x] Planning
- [ ] Implementation
- [ ] Requirement Clarification
- [ ] Human Review

## State.yaml Update

```yaml
phase: plan_review
status: changes_requested
plan_review:
  result: REQUEST_CHANGES
  iteration: 3
next_action: planning
```

## Review Notes

This iteration independently re-read the current requirement, analysis, plan, state, repository instructions, and Plan Critic rules on `codex/feat-1013-app-version-check`. It supersedes the prior Iteration 2 approval. No production source or test files were modified; no tests were run because this was a planning review.

---

# Prior Review History

The Iteration 2 review and Iteration 1 findings are retained below for traceability. The Iteration 2 `APPROVED` decision is superseded by Iteration 3 above.

# Plan Review

Task: feat-1013-app-version-check  
Reviewer: Plan Critic Agent  
Review Iteration: 2 (re-review; iteration 1 retained below)  
Review Date: 2026-10-13

---

# Summary

## Review Result

- [x] APPROVED
- [ ] REQUEST_CHANGES
- [ ] BLOCKED

## Summary

Re-review confirms the returned planning gaps are resolved. Release uses P0 + 1 and persists that same value only in the successful `:app:assembleRelease` lifecycle task action; the plan defines controlled success/failure/debug checks, validates versions and HTTPS URLs before prompting, rejects HTTPS downgrade redirects, and preserves consumed version codes during rollback. The production endpoint replacement is a pre-production release gate. Implementation can begin after this approved planning package.

# Review Checklist

| Item | Result | Notes |
|------|--------|------|
| Requirement understood | PASS | The plan preserves login/offline access and current-screen state as required. |
| Acceptance Criteria complete | PASS | AC-001 through AC-011 are measurable and mapped to implementation and validation. |
| Repository analysis complete | PASS | Confirmed the existing BASE URL, target-specific Retrofit factory, cleartext allowance limited to the test IP, FileProvider, and CI test filters. |
| Architecture impact reasonable | PASS | An app-level lifecycle coordinator fits the existing Activity/coordinator architecture. |
| Affected modules identified | PASS | Gradle, API, lifecycle/update flow, manifest, FileProvider paths, strings, tests, and CI are covered. |
| Dependencies identified | PASS | Test API, Android installer, emulator, and external JKS are identified. |
| Risks evaluated | PASS | Version wiring, lifecycle behavior, signing, and LAN availability are recorded; see blocking findings below. |
| Test Plan complete | PASS | AC-001 covers successful, injected-failure, debug, and subsequent Release builds in an isolated copy; AC-002/003/009 cover malformed versions, non-HTTPS URLs, and downgrade redirects. |
| Regression Plan complete | PASS | Login, offline entry, target selection, Activity transitions, forms/photos, and FileProvider use are covered. |
| Open Questions documented | PASS | No implementation decision remains open; production endpoint replacement and verification is recorded as a pre-production release gate. |
| Implementation steps actionable | PASS | Release versionCode is P0 + 1; atomic persistence is an action on `:app:assembleRelease` after dependencies succeed, with no finalizer. Strict response validation precedes prompting. |
| Task size appropriate | PASS | This is a broad feature, but version management and update installation are one cohesive user-facing capability. |
| Rollback strategy (if applicable) | PASS | Rollback retains the highest P0 consumed by any successfully assembled Release APK; the next Release uses a higher code. |

# Findings

## Finding 1

Severity:
- [ ] Critical
- [x] Major
- [ ] Minor
- [ ] Suggestion

Category: Release version persistence

Description: Step 7 requires advancing `P0_VERSION_CODE` only after successful `:app:assembleRelease`, but the plan leaves the Gradle task wiring unresolved and only says to validate a controlled failure. If persistence is attached as a Gradle finalizer, Gradle runs that finalizer even when the finalized task fails or is `UP-TO-DATE`; a failed release can therefore still advance the property unless the write explicitly checks the assembly result. See the [Gradle finalizer task behavior](https://docs.gradle.org/current/userguide/controlling_task_execution.html#sec:finalizer_tasks).

Recommendation: Update the plan to specify a success-gated persistence mechanism for the actual `:app:assembleRelease` result. Keep the APK's configured version code and the persisted property tied to the same computed next value. Add a failure test that fails an assembly task and proves P0 is unchanged, alongside successful release, debug, and other non-release cases.

Planning Response: Release variant versionCode is current P0 + 1. The same value is atomically persisted by an action attached directly to the `:app:assembleRelease` lifecycle task after dependencies succeed. A finalizer is excluded. Validation uses an isolated P0=1 copy for success, injected dependency failure, debug build, and subsequent successful release.

Status:
- [ ] Open
- [x] Resolved

---

## Finding 2

Severity:
- [ ] Critical
- [x] Major
- [ ] Minor
- [ ] Suggestion

Category: API contract and URL validation

Description: The requirement contract says the Android record must contain a three-part numeric version and an HTTPS APK URL. The plan covers parsing and download failures, but it does not explicitly validate the URL scheme before displaying the update prompt. A malformed or non-HTTPS URL could reach the update flow despite AC-003 and AC-009 requiring invalid URL data to be treated as no update.

Recommendation: Make response validation an explicit implementation step: accept the expected Android record only when the version is exactly three numeric components and the APK URL is a valid HTTPS URL; otherwise return a quiet no-update result before prompting. Add unit cases for malformed URLs and non-HTTPS schemes, and ensure download redirects do not downgrade from HTTPS.

Planning Response: API data is accepted only with exactly three numeric version components (including integer overflow rejection), a parseable HTTPS APK URL, and an HTTPS-only redirect chain. Invalid data returns no update before prompt display; AC-002/003/009 cover the rejection cases.

Status:
- [ ] Open
- [x] Resolved

---

## Finding 3

Severity:
- [ ] Critical
- [x] Major
- [ ] Minor
- [ ] Suggestion

Category: Rollback and version-code monotonicity

Description: The rollback plan says to restore `app/version.properties` from the task baseline if a Release build advanced it. That can lower P0 after a successful build and cause a later artifact to reuse a version code, contrary to the requirement that successful Release assembly advances the counter.

Recommendation: Roll back feature code while retaining the highest P0 value consumed by any successful Release build. Record that the code remains consumed even if the generated artifact was not distributed; do not reset the counter based only on the task branch baseline.

Planning Response: Requirement AC-001 and AC-011 plus the rollback procedure retain every version code consumed by a successfully assembled Release APK. Reverting feature code cannot reset P0; the next Release code must be higher.

Status:
- [ ] Open
- [x] Resolved

---

## Finding 4

Severity:
- [ ] Critical
- [ ] Major
- [ ] Minor
- [x] Suggestion

Category: Production endpoint release gate

Description: The plan correctly records that the current endpoint is test-only and the production URL is still pending, but it does not name the release evidence that will prove the test URL has been replaced before production release.

Recommendation: Carry this known prerequisite into the Release checklist or an explicitly linked follow-up task when production release is prepared. It does not block implementation against the approved test endpoint.

Planning Response: The plan names the pre-production gate: replace the test endpoint and capture successful verification of the production response and HTTPS APK URL.

Status:
- [ ] Open
- [x] Resolved

# Re-review of Findings 1–4

Findings 1–3 are resolved as detailed above. Finding 4's suggestion was carried into the production release gate.

# Blocking Issues

- None.

# Improvement Suggestions

- None. The production endpoint replacement and verification suggestion is recorded as a release gate.

# Decision

## APPROVED

Planning resolved the blockers and carried the production endpoint suggestion into the release gate. Implementation may begin.

# Next Action

- [ ] Planning
- [x] Implementation
- [ ] Requirement Clarification
- [ ] Human Review

# State.yaml Update

```yaml
phase: plan_review
status: approved
plan_review:
  result: APPROVED
  iteration: 2
next_action: implementation
```

# Review Notes

Iteration 1 reviewed baseline `13865d875fad6f949695f7d01a626736c41b317d` and returned Findings 1–3. Planning then revised the requirement, analysis, and plan artifacts. Iteration 2 reviewed those revised artifacts on `codex/feat-1013-app-version-check`. No production source or test files were modified and no tests were run because this was planning/review work.

# Definition of Done

- [x] All checklist items reviewed
- [x] Findings documented
- [x] Blocking Issues identified
- [x] Improvement Suggestions separated
- [x] Decision recorded
- [x] state.yaml updated
