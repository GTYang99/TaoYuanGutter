# FEAT-1006 Independent Verification

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
