# Verification Report

## Scope and Revision

- Task: `feat-1118-3`
- Branch: `codex/feat/feat-1118-3`
- Commit under review: `108a6dc2bd441a87cc053d130c058f2b9eac5141`
- Verification started with `HEAD` unchanged at the commit above.
- Worktree was not clean at start: pre-existing modification `gradle/libs.versions.toml` changes AGP `9.4.1` to `9.5.1`. Tests and build therefore ran with that uncommitted environment change; exact clean-commit verification is `NOT VERIFIED`.

## Commands and Results

| Check | Result | Evidence |
|---|---|---|
| Targeted JVM tests for mapper, response parsing, draft, completion, and inspection | FAIL | `./gradlew testDebugUnitTest --tests ...` — 27 tests, 5 failures |
| Focused compatibility/request tests | PASS | `GutterRepositoryStoreDitchBoundaryTest`, `StoreDitchResponseParsingTest`, `GutterSessionDraftTest`, virtual request omission, and photo-slot test passed |
| Debug build | PASS | `./gradlew assembleDebug` completed successfully |
| Runtime source scan | PASS with compatibility exceptions | `IS_TIEINPOINT` remains only in draft legacy cleanup and deprecated response compatibility code under `app/src/main`; no UI/request behavior references were found |
| Physical/device verification | NOT VERIFIED | `adb devices` could not start the ADB daemon (`Operation not permitted`) |
| CI | NOT VERIFIED | No CI result was available in the repository or connected environment |

## Failed JVM Tests

The following existing tests still assert the removed behavior:

- `StoreDitchNodeRequestMapperTest.normalNewNodeOmitsXyNumAndSendsUppercaseConnectionFlagsAsBooleans`
- `StoreDitchNodeRequestMapperTest.tieInPointOmitsExemptDetailAndConnectingParameters`
- `StoreDitchNodeRequestMapperTest.cantOpenOmitsExemptDetailAndConnectingParameters`
- `GutterCompletionPolicyTest.connectPointUsesCantOpenDetailExemption`
- `InspectionPresentationTest.tieInInspectionHidesTheSameDetailFieldsAsCantOpen`

These assertions expect `IS_TIEINPOINT` in request JSON or tie-in-specific detail/photo exemptions, which contradict the approved requirement. The failure blocks the test gate, but Verification did not modify tests or production code.

## Acceptance Criteria

| AC | Result | Evidence / reason |
|---|---|---|
| AC-001 | NOT VERIFIED | Runtime source scan shows the removed form references, but no instrumentation/device result is available. |
| AC-002 | NOT VERIFIED | Mapper/presentation compatibility evidence is partial; inspect/dropdown UI was not executed. |
| AC-003 | NOT VERIFIED | Legacy request-boundary and virtual omission tests pass, but the full mapper test set is not green and lacks a clean committed-revision run. |
| AC-004 | NOT VERIFIED | Legacy response parsing and draft retention JVM tests pass; Intent/Bundle and virtual-point runtime flows were not exercised. |
| AC-005 | NOT VERIFIED | No form instrumentation or device evidence for blocking incomplete legacy data and retrying after completion. |
| AC-006 | NOT VERIFIED | No read-only legacy tie-in unlock test was executed. |
| AC-007 | NOT VERIFIED | Baseline-to-target upgrade was not executed; ADB was unavailable. |
| AC-008 | NOT VERIFIED | No damaged-draft upgrade/device test was executed. |
| AC-009 | FAIL | Debug build passed, but targeted JVM verification had 5 failures; instrumentation, upgrade validation, and CI are unavailable. |

## Regression Review

- Debug compilation and APK assembly passed.
- Focused legacy response parsing, draft retention, request-boundary omission, virtual-point omission, and photo-slot tests passed.
- The existing targeted suite is not compatible with the approved removal of tie-in runtime behavior and must be reconciled before Verification can pass.

## Final Result

**FAIL**

Root category: `implementation` / test-suite alignment failure. Next action: `debug`; update the failing test expectations under an approved Debug/Re-Implementation flow, then rerun verification on a clean committed revision. Physical upgrade/UI evidence and CI remain required before Release.
