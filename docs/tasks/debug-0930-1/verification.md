# Independent Verification

## Revision Under Review

- Fixed revision: `b7ceb17a8c27d2a4c0a217f653b2b54213fcf63f` (`codex/debug-0930-1`).
- Worktree was clean at verification start.
- The only changes after implementation revision `6ee9273` are task documentation and state; production/test source is unchanged.

## Acceptance Criteria

| AC | Result | Evidence |
|---|---|---|
| AC-001 | PASS | Independently reproduced on Android 14 emulator `emulator-5554`, package `com.example.taoyuangutter`: LoginActivity showed `API：台北`; tapping it opened BASE/Taipei/DEMO choices; selecting BASE changed the button to `API：內網 BASE`; force-stop/relaunch reset it to `API：台北`. The installed debug APK was built from this worktree's unchanged implementation source (current fixed revision differs from implementation revision only in task docs/state). No API request was made. |
| AC-002 | PASS | Source review: when the gate is false, the login view is `GONE`, its click listener is cleared, and `GutterApiClient.selectTarget()` returns false. `ENABLE_GROUP_SIMULATION` is `BuildConfig.DEBUG`; the recorded release Kotlin compile passed. |
| AC-003 | PASS | `BackendEndpointsTest.apiBackendTargetsMapToApprovedUrls` asserts the three approved URLs. `GutterApiClientEnvironmentTest` checks Taipei default and distinct selected services; `GutterRepository` resolves `GutterApiClient.instance` per call. The execution report records these targeted tests passing. No live API request/write was made. |
| AC-004 | PASS | Source review: service construction uses only the selected target's URL; no cross-target fallback/retry is present. Existing WMS/WMTS URLs remain fixed to Taipei. |
| AC-005 | PASS | `ENABLE_GROUP_SIMULATION` directly uses `BuildConfig.DEBUG`; the false-gate path hides/disables the selector. The execution report records successful release compilation. |

## Validation and Regression Review

- Reviewed implementation diff from `397ebd7` through `6ee9273`, the current source, tests, and the scoped emulator evidence in `execution-report.md`.
- Previously recorded targeted endpoint/client JVM tests, CI-parity tests, debug/release builds, and instrumentation compilation are consistent with source at the fixed revision; those results were not rerun in this verification environment.
- Attempted the two focused JVM test classes. They could not start because no Java runtime is available (`Unable to locate a Java Runtime`).
- AC-001 emulator interaction was independently completed using elevated ADB after normal ADB startup was denied.
- Remote CI: `NOT VERIFIED`; no remote run/result is recorded.
- No live backend traffic was sent. WMS/WMTS routes remain unchanged; default selection remains Taipei; selection remains process-local.

## Final Result

`NOT VERIFIED`: AC-001 through AC-005 pass, but remote CI has no result. No implementation failure was found. Preserve the fixed revision and resume Verification when remote CI evidence is available.

## Next Action

`verification`
