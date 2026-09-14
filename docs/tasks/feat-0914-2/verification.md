# Verification Report

## Revision

- Branch: `feat/側溝清單`
- Implementation commit: `d8b8967`
- Verification commit: `ec4d32c`
- Working tree contains the pre-existing unrelated `GutterApiService.kt` modification; it was not included in either task commit.

## Validation evidence

| Check | Result | Evidence |
|---|---|---|
| Debug Kotlin compilation | PASS | `./gradlew :app:compileDebugKotlin --no-daemon` |
| Debug unit tests | PASS | `./gradlew :app:testDebugUnitTest --no-daemon` |
| Debug APK build | PASS | `./gradlew :app:assembleDebug --no-daemon` |
| Diff whitespace check | PASS | `git diff --check` |
| Figma MCP design context | PASS | File `IfmNbZKhr4wojZ2bF5rYHG`, node `2374:26810`; dimensions recorded in `execution-report.md` |
| Targeted Android instrumentation | PASS | `./gradlew :app:connectedDebugAndroidTest --no-daemon -Pandroid.testInstrumentationRunnerArguments.class=com.example.taoyuangutter.MainShellActivityTest`; all 7 `MainShellActivityTest` cases passed on `Medium_Phone(AVD) - 14`, including Figma geometry/control order, seconds precision, effective node-count rendering, independent draft IDs across coordinator recreation, Espresso Alert add/cancel/confirm callbacks, and requested draft-ID routing. |
| Full Android instrumentation | PASS | `./gradlew :app:connectedDebugAndroidTest --no-daemon`; all connected tests completed successfully on `Medium_Phone(AVD) - 14` in 2m41s. |
| CI | NOT VERIFIED | No CI result is available in the repository or current session. |

## Acceptance criteria

| AC | Result | Evidence / limitation |
|---|---|---|
| AC-001 | NOT VERIFIED | `MapWorkspaceFragment.openAddGutterFlow()` now opens `AddGutterListBottomSheet`; no runtime UI evidence. |
| AC-002 | NOT VERIFIED | Instrumentation proves two independent multi-gutter IDs survive coordinator recreation and are ordered, but UI-level add/select/switch editing was not exercised. |
| AC-003 | NOT VERIFIED | Adapter uses `createdAt`, seconds precision, effective waypoint count, creation order. Targeted instrumentation verifies toolbar/row geometry, left 新增 and right close ordering, list presence, seconds-formatted time, and effective node count for a populated row; multi-row ordering and click-through were not exercised. |
| AC-004 | NOT VERIFIED | Espresso verifies the list add callback and Alert cancel/confirm callback branches at runtime; the specified message and post-confirm persistence of every draft were not fully exercised. |
| AC-005 | NOT VERIFIED | Instrumentation proves repository-backed draft recovery across coordinator recreation and independent IDs; Activity/process recreation and effective-edit autosave were not executed. |
| AC-006 | NOT VERIFIED | Instrumentation proves deleting one persisted draft leaves the other; successful upload interaction was not executed. |
| AC-007 | NOT VERIFIED | Legacy flow remains compilable and unit tests pass; full form/photo/map regression requires instrumentation or manual device smoke test. |

## Regression review

- Existing debug unit tests pass.
- Existing source-level legacy ownership path remains the default for `MainActivity` and non-multi flows.
- Targeted device evidence covers the list layout; the full connected suite passes, but it does not exercise the new multi-gutter workflow end-to-end.

## Result

`NOT VERIFIED` — implementation builds, unit tests and the full connected suite pass, and the Figma-aligned list layout has targeted device evidence, but end-to-end multi-gutter workflow evidence and CI result are unavailable.

## Next action

Add or execute end-to-end instrumentation for AC-001–AC-006 and obtain CI evidence before Release.
