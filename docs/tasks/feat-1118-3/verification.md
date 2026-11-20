# Verification Report

## Scope and Revision

- Task: `feat-1118-3`
- Branch: `codex/feat/feat-1118-3`
- Implementation revision: `5d26cf1c`
- Follow-up task-state revision: `71a7407f` (documentation only)
- Worktree contains only the pre-existing, unrelated `gradle/libs.versions.toml` modification (`AGP 9.4.1 -> 9.5.1`).
- No production code was changed during this verification pass.

## Commands and Results

| Check | Result | Evidence |
|---|---|---|
| Full JVM suite | PASS | `./gradlew testDebugUnitTest`; 33 tests, BUILD SUCCESSFUL |
| Instrumentation compilation | PASS | `./gradlew compileDebugAndroidTestKotlin`; BUILD SUCCESSFUL |
| Debug APK build | PASS | `./gradlew assembleDebug`; BUILD SUCCESSFUL |
| Source scan | PASS | No active tie-in UI/request behavior; remaining `IS_TIEINPOINT` references are compatibility cleanup, the read-only repair exception, or tests/fixtures |
| Physical instrumentation runtime | NOT VERIFIED | No usable ADB/device was available |
| Baseline-to-target upgrade | NOT VERIFIED | Upgrade flow from `78d6df31` was not executable without a device/environment |
| CI | NOT VERIFIED | No CI result was available |

## Acceptance Criteria

| AC | Result | Evidence / reason |
|---|---|---|
| AC-001 | NOT VERIFIED | Source and updated instrumentation assertions remove the control and interaction; device UI execution is unavailable. |
| AC-002 | NOT VERIFIED | Inspect presentation source/test covers legacy input without the label; device/dropdown execution is unavailable. |
| AC-003 | PASS | `StoreDitchNodeRequestMapperTest` and full JVM suite pass; request mapping omits `IS_TIEINPOINT` across supported point modes. |
| AC-004 | NOT VERIFIED | Compatibility code and JVM coverage pass, but Intent/Bundle and draft runtime flows were not executed on device. |
| AC-005 | NOT VERIFIED | General-point validation path is restored and compiled; end-to-end UI submit/retry requires instrumentation runtime. |
| AC-006 | PASS | `SubmittedDraftResumePolicyTest` covers repairable legacy tie-in unlock and non-tie-in read-only preservation; full JVM suite passes. |
| AC-007 | NOT VERIFIED | APK build passes, but baseline installation/update and startup/data-retention flow were not executed. |
| AC-008 | NOT VERIFIED | Per-element tolerant parsing is implemented and compiled; damaged-draft migration requires runtime/device evidence. |
| AC-009 | NOT VERIFIED | JVM, instrumentation compilation, Debug build, and source scan pass; runtime instrumentation, upgrade, and CI remain unavailable. |

## Regression Review

- `IS_CONNECTING` behavior for normal points was restored because the approved knowledge-resolution record excludes its historical removal from this task.
- `IS_CANTOPEN` and virtual-point branches remain distinct in form/request handling.
- Legacy tie-in data is normalized as a normal point, except for the explicitly scoped submitted read-only repair unlock.
- Draft persistence strips the legacy runtime marker and parses list elements independently so malformed entries do not invalidate all usable data.
- Existing unrelated AGP worktree modification was not staged or changed.

## Final Result

**NOT VERIFIED**

The implementation and all available non-device evidence support approximately 95% confidence. Release is still blocked by physical instrumentation runtime, baseline upgrade evidence, and CI results. The next action is infrastructure/device verification on the committed revision; no further production-code repair is indicated by the available evidence.

## Minimal Verification Round — 2026-11-20

- Revision: `f683cdee` (documentation follow-up on implementation revision `5d26cf1c`)
- Command:
  `./gradlew testDebugUnitTest --tests 'com.example.taoyuangutter.api.StoreDitchNodeRequestMapperTest' --tests 'com.example.taoyuangutter.api.GutterRepositoryStoreDitchBoundaryTest' --tests 'com.example.taoyuangutter.api.StoreDitchResponseParsingTest' --tests 'com.example.taoyuangutter.pending.GutterSessionDraftTest' --tests 'com.example.taoyuangutter.gutter.GutterCompletionPolicyTest' --tests 'com.example.taoyuangutter.gutter.GutterImportValidatorTest' --tests 'com.example.taoyuangutter.gutter.InspectionPresentationTest' --tests 'com.example.taoyuangutter.gutter.SubmittedDraftResumePolicyTest' --tests 'com.example.taoyuangutter.gutter.WaypointAdapterPresentationTest'`
- Result: **PASS** — 33 tests completed; Gradle `BUILD SUCCESSFUL`.
- Coverage: request omission, virtual/normal point mapping, legacy response parsing, draft retention/normalization, submitted legacy repair policy, required-field validation, inspection presentation, and waypoint presentation.
- No production code or existing unrelated AGP worktree change was modified during this round.
- Overall task result remains **NOT VERIFIED** pending physical instrumentation, baseline upgrade, and CI evidence.
