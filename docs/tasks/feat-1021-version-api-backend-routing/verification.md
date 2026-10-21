# Independent Verification — feat-1021-version-api-backend-routing

## Revision and checkout

- Commit under verification: `7176b5bebe3b9e0425d2757389b0dcefa700b15a` (documentation/state commit; implementation under test: `7f31d966bf14909231062e51d7fbb4cbf11c915e`).
- Branch: `codex/feat-1021-version-api-backend-routing`.
- Checkout state at verification start: clean. `HEAD` remained `7176b5be...` throughout verification. No production or test files were modified. A temporary ignored `local.properties` with only `MAPS_API_KEY=verification-placeholder` was created for Gradle and removed automatically.

## Acceptance criteria

| AC | Result | Evidence and reason |
|---|---|---|
| AC-001 | PASS | `AppVersionRepositoryTest.versionCheckUsesSelectedTargetAndItsMappedUrl` selects BASE and asserts the resulting endpoint URL is `http://192.168.10.84/TY_RSGDBIP/api/appVersion`. `GutterApiClient.versionCheckService(target)` delegates to `serviceFor(target)`, whose Retrofit base URL comes from `BackendEndpoints.apiBaseUrl(target)`; `GutterApiService.getAppVersion` retains `@GET("api/appVersion")` with no parameters. |
| AC-002 | PASS | The same focused test selects TAIPEI and asserts `https://taipei.srgeo.com.tw/TY_RSGDBIP_BK/api/appVersion`; the production service uses the mapped Retrofit base URL plus the parameter-free GET path. |
| AC-003 | PASS | The same focused test selects DEMO and asserts `https://demo.srgeo.com.tw/TY_RSGDBIP_BK/api/appVersion`; the production service uses the mapped Retrofit base URL plus the parameter-free GET path. |
| AC-004 | PASS | `AppVersionRepository` reads `selectedTarget` once at method entry and passes that snapshot to the service factory. `targetIsCapturedWhenCheckStartsAndNextCheckUsesNewSelection` holds a TAIPEI request while changing selection to DEMO and asserts the started call retains TAIPEI and the next call receives DEMO. |
| AC-005 | PASS | The implementation diff changes only version-service routing; response validation and `AppVersionUpdateApplication` parsing/comparison/prompt/cancel code are unchanged from `9377f41a`. The repository still returns null for unsuccessful HTTP responses and caught non-cancellation exceptions, with no alternate-target retry. `unsuccessfulHttpResponseDoesNotOfferUpdate` passed. Existing UI lifecycle behavior is preserved by unchanged source; its instrumentation suite was not run in this verification. |

## Validation performed

- `git diff --check 9377f41a..7176b5be`: PASS.
- `git show --check 7f31d966`: PASS.
- `:app:testDebugUnitTest --tests com.example.taoyuangutter.api.AppVersionRepositoryTest --tests com.example.taoyuangutter.common.BackendEndpointsTest --rerun-tasks --no-daemon --console=plain`: PASS; 32 tasks executed, including the focused test task. The command used Android Studio bundled JBR and a temporary placeholder Maps key.
- `:app:assembleDebug --no-daemon --console=plain`: PASS; local build reported `BUILD SUCCESSFUL` (50 tasks, 4 executed and 46 up-to-date), using the same temporary placeholder.
- CI: NOT VERIFIED. `execution-report.md` records CI as pending; this local branch has no CI run evidence for the fixed revision. The workflow `.github/workflows/android-ci.yml` is configured to run on push/PR/manual dispatch, but no provider run/result was available to verify. CI is a required gate before Release.
- Physical device / live backend: NOT RUN, consistent with the verification assignment. No live-server availability claim is made.
- Android instrumentation `AppVersionUpdateFlowTest`: NOT RUN; the changed surface is service routing and focused JVM tests/source review cover the modified behavior, but UI lifecycle execution is not claimed.

## Regression review

- Ordinary API target mapping is reused without edits; `BackendEndpointsTest.apiBackendTargetsMapToApprovedUrls` passed.
- No automatic fallback was introduced: each repository check calls only the selected target service. HTTP 503 returns no update; exceptions remain fail-open.
- Version response parsing, semantic version comparison, update prompt/cancel, and download flow have no production diff in this change.
- Test limitation: routed URL assertions compose the existing endpoint mapping from the target captured by the repository factory; no MockWebServer/live request asserted the final Retrofit wire URL. Source inspection confirms the target-aware service delegates to the same mapping.

## Verification result

**NOT VERIFIED** — all five acceptance criteria pass based on source/diff review and focused JVM evidence, but required CI status for this exact revision is still pending/unavailable. This is a gate evidence limitation, not a discovered implementation failure. No failed acceptance criteria. Once CI results are recorded for the committed revision, resume Verification and update this result.
