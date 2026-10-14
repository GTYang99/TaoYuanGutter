# Historical FEAT-1006 Verification — Two-GPKG Scope

## Target Revision and Initial State

- Target commit: `cded0859415658effe3eaf65f7747c95a06b85be` (`docs(FEAT-1006): start independent verification`).
- Branch: `codex/feat-1006-gpkg-restart`; no upstream is configured.
- Initial worktree state: clean (`git status --porcelain=v1` returned no entries) immediately before verification.
- The target commit changes only `docs/tasks/feat-1006/state.yaml` from its parent. The implementation and test sources under review were already committed, and the worktree stayed clean during evidence collection. No production source or tests were changed.

## Inputs Reviewed

Reviewed `AGENTS.md`, `ai/verification-rules.md`, `ai/testing-rules.md`, `ai/issue-management.md`, `requirement.md`, `analysis.md`, `plan.md`, `state.yaml`, `plan-review.md`, `execution-report.md`, `asset-manifest.md`, `issue-log.md`, the task evidence, the task implementation diff, and the related unit and instrumentation tests.

## Independent Evidence Collected

- `git diff --check HEAD^ HEAD`: PASS.
- `:app:testDebugUnitTest`: PASS on this worktree using Android Studio's bundled JBR and the temporary non-secret Maps placeholder required by manifest processing. The temporary `local.properties` was removed after the run. This executes the projection, colors, GeoPackage Polygon/MultiPolygon decoding, holes, and zoom-profile unit tests.
- Direct SHA-256 and byte-length checks on both current source assets: PASS. Legacy source is 19,615,744 bytes with SHA-256 `4b0df3e90dab556b3de0bd859511dee5a3af5d171255e4157e32b8d3f6e1ecaf`; possible-gutter source is 60,661,760 bytes with SHA-256 `de68967c0fd607b1921a8bd3856d2a6d70e754d2bbb966beca89c82f6cc16177`.
- Direct readback of the existing release APK `app/build/outputs/apk/release/app-release-unsigned.apk` (43,752,006 bytes): both GPKGs are present with exact source lengths and hashes; the obsolete GeoJSON asset is absent. APK-compressed sizes are 7,668,975 and 22,349,403 bytes. The APK-level source verification passes; the APK's total byte count differs by 24 bytes from the total recorded in `asset-manifest.md` (43,752,030 bytes), with no acceptance limit on total APK size.
- Reviewed the committed Android 14 emulator evidence and the actual assertions in `OfflinePolygonDataStoreInstrumentedTest` and `OfflinePolygonMapHostInstrumentedTest`. The recorded results are `OK (5 tests)` and `OK (3 tests)` on `emulator-5554` (`sdk_gphone64_arm64`, Android 14). The host test covers both main-map hosts' UI toggles, form/picker visibility propagation, real populated/sparse tiles, zooms 16/18/20, pan, repeated enable/disable, provider release, and reopening each host twice. The data-store test covers the real packaged files, tile colors, labels, provider release, and obsolete-cache cleanup.
- Inspected the saved MainActivity screenshots `evidence/AC-002-main-legacy.png` and `evidence/AC-002-main-possible.png`; both show their colored offline features over the basemap. The source decoder retains GeoPackage Polygon/MultiPolygon rings, the fill uses even-odd paths, and the unit tests exercise holes and multiple polygon members.
- Reviewed `evidence/AC-001-emulator-okhttp-capture.md`: during the recorded four-host emulator run, 374 app-process OkHttp GET entries were captured, including other map-layer traffic, and none named `legacyDitch` or `roadServey`. This is app-level OkHttp evidence, not a raw packet capture.
- Fresh emulator instrumentation was not started. ADB initially could not start inside the sandbox; after read access was granted, `emulator-5554` was present, Android 14, and still had a `com.example.taoyuangutter.test` process. Available activity diagnostics did not establish that no instrumentation was active, so starting another runner would risk interfering with another test. The committed emulator run is used as reviewed supporting evidence, not represented as a new verification run.
- Remote CI evidence is unavailable. Task state and execution report show CI pending; no upstream is configured for this branch. Local JVM validation is not described as remote CI.

## Acceptance Criteria

| Criterion | Result | Evidence and reasoning |
|---|---|---|
| AC-001 | PASS | Direct APK readback confirms both packaged GPKG byte streams match the verified source hashes and the old GeoJSON is absent. The committed app-process request capture records 0 `legacyDitch`/`roadServey` requests among 374 observed GET requests while other map-layer requests occurred. The emulator host test exercises the local providers in all four hosts. |
| AC-002 | PASS | The focused JVM suite passes geometry-header, Polygon hole, MultiPolygon-member/hole, projection, and exact opaque-color tests. The emulator data-store test exercises both real GPKGs and exact fill-color pixels; saved emulator screenshots show both layers over the basemap. The renderer uses even-odd fill paths and draws each decoded polygon member and ring. |
| AC-003 | PASS | The committed host instrumentation asserts both exact layer toggles and provider visibility in MainActivity and MapWorkspaceFragment. It opens GutterFormActivity and MapPointPickerActivity with each requested layer state and checks the resulting provider state and local tiles. Source review confirms all four hosts use `OfflinePolygonOverlayManager`. |
| AC-004 | PASS | The committed emulator host test moves each host through zoom 16/18/20, pans through populated and sparse extents, checks populated and empty provider tiles, and asserts synchronous camera calls finish in under one second. The execution record reports no crash or ANR and records a MainActivity PSS observation of 164,785 KiB. Index construction uses a dedicated background executor; tile caching is bounded to 48 entries. This is focused responsiveness evidence, not a broad performance profile. |
| AC-005 | PASS | The provider's release path evicts its tile cache; the manager removes overlays and releases providers on disable or host teardown. Instrumentation asserts references are cleared, repeated enable does not add another provider, and re-enable restores one provider. The data-store test also verifies a released provider returns `NO_TILE`. |
| AC-006 | PASS | Instrumentation asserts exact titles `水務局舊資料（.gpkg）` and `可能側溝位置（.gpkg）`. Diff review from the prior GPKG/GeoJSON implementation shows the possible-layer title is the only layer-sheet string changed for this source update; the saved UI screenshot also reflects the current labels. |
| AC-007 | PASS | The style function clamps at 2.5 tile pixels through zoom 16, interpolates linearly to 1.5 at zoom 20, and yields 2.0 at zoom 18; endpoint zooms and widths are parameters. JVM tests pass for default and alternate profiles. The emulator data-store test renders both real sources at zooms 16/18/20 and checks that alternate outline width changes edge pixels while the interior fill remains exact. |

## Regression Review

- Focused local JVM tests pass, including unchanged projection behavior, exact layer colors, real geometry decoding paths, and the zoom profile.
- Reviewed the four host wiring and teardown paths. The committed emulator host run reports successful map readiness, controls or propagated visibility, populated/sparse tile behavior, repeated host reopen, and no crash or ANR.
- Request evidence shows unrelated map traffic remains active (`regions` and `map_ditch_nodes_labels`) while neither removed feature-data layer is requested.
- No full application regression suite was run; the approved plan says full regression is not required absent a broad navigation, unrelated-layer, or startup failure trigger.
- Static source review confirms renderer errors return `NO_TILE` with a diagnostic log rather than crashing the map. Malformed-source recovery was not separately injected in this verification.

## CI and Final Result

- Remote CI build: NOT VERIFIED; no CI result is recorded for this revision.
- Remote CI test: NOT VERIFIED; no CI result is recorded for this revision.
- The local JVM suite passed, but it does not satisfy the remote CI gate.
- No acceptance criterion failed. Verification result: **NOT VERIFIED** because the required CI evidence is unavailable. This does not advance the task to Release.
- Issue classification: `environment` (missing remote CI result / branch has no upstream configured); priority P1 because the release gate is blocked, although local validation passed. Recorded as `ISS-FEAT-1006-007`. No implementation defect was found, so Debug is not indicated.
- Next action: obtain CI evidence for this exact revision through the authorized CI path, then resume Verification. Do not treat the local test run as CI or push/merge as part of this verification.

## Outcome

- Acceptance criteria: AC-001 through AC-007 PASS on the reviewed committed evidence.
- Overall result: NOT VERIFIED (CI gate evidence missing).
- `phase`: `verification`
- `status`: `verification_not_verified`
- `next_action`: `infrastructure`

## Current Independent Verification — Three GPKG Scope

### Revision and Evidence Basis

- Task: `FEAT-1006`.
- Target: `ede6d5b79d26a9bceb8b36fdcfd53791c7736efb`, branch `feat/feat-1006-wmts-offline-data`.
- Initial worktree was clean. The diff from latest emulator-validated implementation/test commit `630936735d859f8d4364956b0a6602030cdb0cf9` to this target contains documentation updates only; no application or test source changed after the recorded emulator run.
- Independently checked the three app source asset hashes against the approved manifest and read back the current Debug APK `app/build/outputs/apk/debug/app-debug.apk`. Its three GPKG entries each match the source byte-for-byte and SHA-256; the superseded GeoJSON entry is absent. APK size is 49,303,843 bytes.
- Reviewed the current source, unit and instrumentation assertions, `execution-report.md`, the emulator request capture, screenshots, style baseline evidence, plan, and prior implementation diff. Runtime evidence reused from the committed Android 16 emulator run on `emulator-5554` (`sdk_gphone16k_arm64`); no new App/runtime command was run during this independent review, and no physical device was used.
- GitHub Actions [Android CI run #15](https://github.com/GTYang99/TaoYuanGutter/actions/runs/38526214063) was independently fetched. It has `head_sha=ede6d5b79d26a9bceb8b36fdcfd53791c7736efb`, status `completed`, conclusion `success`; its `Unit tests and debug build` job passed the task-scoped JVM tests and Debug APK assembly.
- `git diff --check 6309367..HEAD` — PASS. No Release build, merge, or deployment was performed as part of Verification.

### Acceptance Criteria

| AC | Result | Evidence |
|---|---|---|
| AC-001 | PASS | Instrumentation checks the three packaged GPKGs against recorded size and SHA-256; direct APK readback independently confirms all three current Debug APK entries match source bytes exactly and no GeoJSON is packaged. The emulator OkHttp capture recorded 0 `legacyDitch`, `roadServey`, or `regions` requests among 113 successful GETs while unrelated map traffic remained active. All four host providers were exercised by the emulator host suite. |
| AC-002 | PASS | JVM and emulator tile assertions cover exact opaque `#156D1D`/`#FA0000` fills, Polygon holes, MultiPolygon members, and EPSG:3826 projection control points. Saved MainActivity screenshots show the two thematic layers over the loaded basemap; region projection is normalized from its GPKG-declared geographic CRS before indexing. |
| AC-003 | PASS | The committed emulator host test covers MainActivity and MapWorkspaceFragment toggles and the gutter-form and point-picker hosts with requested overlay visibility. It queries local thematic/region providers in each host and checks provider visibility. The test source and implementation remain unchanged at the target. |
| AC-004 | PASS | The Android 16 emulator host suite completed 3 tests in 75.925 seconds, exercised pan/zoom through populated and sparse areas across the four hosts, and reported no crash, ANR, or failed map-ready callback. Camera changes returned in under one second; recorded MainActivity PSS was 164,785 KiB. These are focused scenario observations, not a broad performance profile. |
| AC-005 | PASS | Instrumentation asserts provider/overlay cleanup on disable and host teardown, bounded tile cache (48 entries), no duplicate provider on repeated enable, one provider after re-enable, and `NO_TILE` after release. The same checks ran in the committed emulator suite. |
| AC-006 | PASS | Source and instrumentation assert exact titles `水務局舊資料`, `可能側溝位置`, and preserved `桃園行政區`; the two thematic suffixes are absent. Review of the layer-sheet change found no other label/layout change in scope. |
| AC-007 | PASS | `OfflinePolygonStyle` exposes endpoint widths/reference zooms and interpolates continuously; JVM assertions cover 2.5 at/below zoom 16, 2.0 at zoom 18, 1.5 at/above zoom 20, and alternate parameters. Emulator tests render both real thematic GPKGs at zooms 16/18/20 and show edge changes under alternate widths while preserving interior fill and holes. |
| AC-008 | PASS | The packaged source and instrumentation verify table `桃園市行政區`, SRS 100000, MultiPolygon metadata, and 13 features. Projection control-point tests and the emulator's populated z13 tile validate coordinate normalization. The local style matches the saved WMTS baseline (transparent background, `#762E77` stroke, 2 px opaque core / 4 px antialiased footprint); all four hosts exercise the region provider and visibility. The live emulator capture observed 0 `regions` requests; controls/order/z-index/transparency are checked by tests/source. |

### Regression Review and Result

- No unrelated feature-data requests replaced the local data path; the request capture includes 109 successful unrelated `map_ditch_nodes_labels` requests and no prohibited feature requests.
- Four-host instrumentation, real-data tile assertions, asset readback, projection/style tests, and CI all pass. Physical-device evidence is intentionally out of scope under the approved emulator-only constraint.
- All AC-001 through AC-008: **PASS**. CI build and test: **PASS** for the exact target SHA. Overall Verification: **PASS**.
- Current state: `phase: verification`, `status: verification_passed`, `next_action: release`. Release signoff remains a separate phase; this Verification did not build or publish a Release artifact.
