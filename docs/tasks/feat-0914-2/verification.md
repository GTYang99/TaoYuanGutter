# Verification Report

## Revision

- Branch: `feat/側溝清單`
- Implementation commit: `d8b8967`
- Verification commit: `f9b5337`
- Working tree contains the pre-existing unrelated `GutterApiService.kt` modification; it was not included in either task commit.

## Validation evidence

| Check | Result | Evidence |
|---|---|---|
| Debug Kotlin compilation | PASS | `./gradlew :app:compileDebugKotlin --no-daemon` |
| Debug unit tests | PASS | `./gradlew :app:testDebugUnitTest --no-daemon` |
| Debug APK build | PASS | `./gradlew :app:assembleDebug --no-daemon` |
| Diff whitespace check | PASS | `git diff --check` |
| Figma MCP design context | PASS | File `IfmNbZKhr4wojZ2bF5rYHG`, node `2374:26810`; dimensions recorded in `execution-report.md` |
| Targeted Android instrumentation | PASS | `./gradlew :app:connectedDebugAndroidTest --no-daemon -Pandroid.testInstrumentationRunnerArguments.class=com.example.taoyuangutter.MainShellActivityTest`; `MainShellActivityTest` passed on `Medium_Phone(AVD) - 14`, including the Figma geometry/control-order assertion. |
| Full Android instrumentation | NOT VERIFIED | The earlier full `connectedDebugAndroidTest` run did not complete within the available execution window and was stopped. |
| CI | NOT VERIFIED | No CI result is available in the repository or current session. |

## Acceptance criteria

| AC | Result | Evidence / limitation |
|---|---|---|
| AC-001 | NOT VERIFIED | `MapWorkspaceFragment.openAddGutterFlow()` now opens `AddGutterListBottomSheet`; no runtime UI evidence. |
| AC-002 | NOT VERIFIED | Multi-item coordinator and ID routing are implemented; no instrumentation result proving two-item switching. |
| AC-003 | NOT VERIFIED | Adapter uses `createdAt`, seconds precision, effective waypoint count, creation order. Targeted instrumentation verifies toolbar/row geometry, left 新增 and right close ordering, and list presence; populated multi-row rendering was not exercised. |
| AC-004 | NOT VERIFIED | Alert text/buttons and cancel/confirm callbacks are implemented; no runtime interaction evidence. |
| AC-005 | NOT VERIFIED | Room ownership, immutable `createdAt`, ID allocator, and immediate-save routing are source-verified; process recreation was not executed. |
| AC-006 | NOT VERIFIED | Success path deletes by selected draft ID and removes only that coordinator item; upload interaction was not executed. |
| AC-007 | NOT VERIFIED | Legacy flow remains compilable and unit tests pass; full form/photo/map regression requires instrumentation or manual device smoke test. |

## Regression review

- Existing debug unit tests pass.
- Existing source-level legacy ownership path remains the default for `MainActivity` and non-multi flows.
- Targeted device evidence covers the list layout only; no device evidence is available for the map, form, photo, upload, or pending-draft UI regressions.

## Result

`NOT VERIFIED` — implementation builds, unit tests pass, and the Figma-aligned list layout has targeted device evidence, but end-to-end workflow evidence, the full instrumentation suite, and CI result are unavailable.

## Next action

Add or execute end-to-end instrumentation for AC-001–AC-007, rerun the full instrumentation suite, and obtain CI evidence before Release.
