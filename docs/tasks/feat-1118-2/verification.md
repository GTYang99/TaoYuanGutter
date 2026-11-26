# Verification Report

## Revision Under Test

- Commit: `0f9125cd2714b6121480437b70b0d0fef44c3f82` (`codex/feat/feat-1118-2`).
- Verification source: immutable `git archive` snapshot at `/private/tmp/feat1118-2-verification.9AYzJV`.
- The main checkout also contained an uncommitted change to `gradle/libs.versions.toml` (`agp = "9.5.3"`, while the tested commit contains `9.4.1`). It was preserved and excluded from the test snapshot.
- A temporary, non-secret `MAPS_API_KEY=ci-placeholder` was supplied in the snapshot's `local.properties` for the build.
- The task state recorded this exact commit before testing. `HEAD` remained unchanged during verification.

## Final Result

**FAIL — implementation.** AC-003, AC-006, AC-007, and AC-008 have source-level evidence contradicting the approved behavior or plan. Route to Debug before re-implementation. AC-005, AC-009, AC-010, and AC-011 remain `NOT VERIFIED` as noted below; they are not counted as passes.

## Checks Performed

| Check | Result | Evidence |
|---|---|---|
| Focused JVM tests | PASS | `./gradlew --offline --no-daemon :app:testDebugUnitTest --tests 'com.example.taoyuangutter.map.OfflinePolygonSourceTest' --tests 'com.example.taoyuangutter.map.MapOverlayControllerStateTest' --tests 'com.example.taoyuangutter.main.MainViewModelTest'`; 9 tests, 0 failures/errors (2 + 4 + 3; XML under `/private/tmp/feat1118-2-verification.9AYzJV/app/build/test-results/testDebugUnitTest/`). |
| Debug APK build | PASS | `./gradlew --offline --no-daemon :app:assembleDebug`; `BUILD SUCCESSFUL` in 26 seconds. |
| APK asset readback | PASS | Python `zipfile` inspection of `app/build/outputs/apk/debug/app-debug.apk` found 29 `assets/*.gpkg` entries. |
| Host GPKG validator | PASS under the resolved source-authority decision | `python3 docs/tasks/feat-1118-2/evidence/validate_gpkg_sources.py --manifest docs/tasks/feat-1118-2/evidence/asset-manifest.json`; output: `metadata=PASS`, `union_consistency=PASS`, `partition_consistency=FAIL`, exit 0. The per-district label differences remain diagnostics under the accepted decision; the required geometry union and metadata gates pass. |
| Emulator instrumentation | NOT VERIFIED | `adb devices -l` could not start the ADB daemon in this sandbox (`could not install *smartsocket* listener: Operation not permitted`). No emulator test was executed. |
| Remote CI | NOT VERIFIED | Task state still has `ci.build` and `ci.test` pending. GitHub commit workflow lookup returned no PR-triggered workflow runs for this commit. |
| Diff whitespace check | FAIL (non-AC documentation issue) | `git diff --check 8d484d5a6d31924d099e793476f898ed99594aa..HEAD` reports trailing whitespace in `docs/tasks/feat-1118-2/plan-review.md` lines 3–6 and 176. No production files were changed during Verification. |

## Acceptance Criteria

| Criterion | Result | Evidence and reason |
|---|---|---|
| AC-001 | PASS | The host validator passes metadata and full/district geometry-union checks under the resolved data-source decision. APK readback confirms 29 packaged GPKGs. Per-district label differences are retained as diagnostics, as approved in `knowledge-resolution.md` and `plan.md`. |
| AC-002 | PASS | Source review confirms the fixed 13-district order in `LayersBottomSheet.kt`, all districts selected by default, and all three target overlay arguments default enabled. `MapOverlayControllerStateTest.allDistrictsAreSelectedByDefault` passes. |
| AC-003 | FAIL | Partial district selection only sets `cbDistrictAll.isChecked` to false; no code sets `isIndeterminate` for a partial selection (`LayersBottomSheet.kt:96-105`). The required tri-state half-selected indicator is absent. See ISS-004. |
| AC-004 | PASS | `OfflinePolygonSourceCatalog` maps full selection to the three full sources, partial selection to the matching legacy/possible district assets, and the region layer to its full source with `TOWNNAME`. The focused source tests pass for full and district/fallback mapping. Emulator rendering was not exercised. |
| AC-005 | NOT VERIFIED | Source filtering uses selected district membership and returns no rows for an empty selection, but no query-level JVM test or emulator map assertion was run in this round. ADB was unavailable. |
| AC-006 | FAIL | `OfflinePolygonOverlayManager.setVisibility` calls `clearOverlays()` as soon as districts/generation change (`OfflinePolygonTileProvider.kt:205-209`), before new sources finish loading. This removes the previous complete result instead of preserving it until an atomic swap. See ISS-005. |
| AC-007 | FAIL | On source failure, the fallback is the full GPKG, but `getTile` queries it with the entire selected-district set (`OfflinePolygonTileProvider.kt:52-61`). The per-district `queryDistricts` value on the fallback definition is not used, so a single failed district can return other selected districts and duplicate rows from successful district sources. See ISS-006. |
| AC-008 | FAIL | `release()` evicts only the tile cache (`OfflinePolygonTileProvider.kt:90-94`). `OfflinePolygonDataStore` keeps completed source futures in a static map with no eviction/close path (`OfflinePolygonDataStore.kt:59-77`); each source holds open SQLite source/index databases. Switching selections therefore does not release unused source/index handles, and the manager rebuilds all overlays instead of diffing sources. See ISS-007. |
| AC-009 | NOT VERIFIED | `MainViewModelTest` passes save/load and explicit-empty-selection cases, but uses in-memory preferences and does not exercise an app/process restart on Android. |
| AC-010 | NOT VERIFIED | Existing map, basemap, geometry, and overlay regression behavior was not exercised on an emulator in this round. |
| AC-011 | NOT VERIFIED | The focused JVM tests, Debug build, source/asset inspection, and host validator passed. Instrumentation, emulator performance/ANR checks, full CI-equivalent tests, and remote CI are still missing. |

## Regression and Coverage Review

- The reviewed implementation adds 26 district assets and source-selection/fallback paths while changing the tile provider. Focused tests cover catalog mapping, selection state/generation increment, and preference serialization; they do not cover the planned deferred out-of-order loader, atomic visible-generation swap, per-district fallback query, or fixed 20-round resource lifecycle bounds.
- Emulator map rendering, restarts, overlay interaction with other layers, and performance were not verified because ADB could not start in the sandbox.
- The final result is `FAIL`; no `NOT VERIFIED` result is treated as `PASS`, and Release must not proceed.

## Issues and Next Action

- ISS-004 — AC-003 tri-state indicator missing; priority P2.
- ISS-005 — AC-006 old overlay removed before replacement is ready; priority P1.
- ISS-006 — AC-007 fallback query is not limited to the failed district; priority P1.
- ISS-007 — AC-008 unused SQLite source/index resources are retained; priority P2.
- Required next action: `debug` for the implementation failures. After the approved fixes, run the missing lifecycle/fallback tests and emulator checks, obtain CI results, and verify the new fixed commit.
