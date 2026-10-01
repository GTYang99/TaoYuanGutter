# Independent Verification

## Revision Under Review

- Fixed revision: `2b0f14325ffc94f3564cc3e93ad6c082898b474f` (`codex/debug-0930-1`).
- Worktree was clean before verification. The implementation source is unchanged from `6ee9273`; later commits before this verification only updated task records.

## Physical Device Context

```yaml
revision: 2b0f14325ffc94f3564cc3e93ad6c082898b474f
device:
  serial: QV710EDR3A
  model: XQ-AU52
  android_version: "12 (API 31)"
app:
  package: com.example.taoyuangutter
  build_variant: debug
```

## Acceptance Criteria

| AC | Steps | Result | Actual Result | Evidence | Retry Count |
|---|---|---|---|---|---:|
| AC-001 | Install current debug APK; open LoginActivity; inspect and tap the environment control. | PASS | Login page showed `API：台北`; selector listed `內網 BASE`, `台北`, and `DEMO`. | Physical XQ-AU52 UI hierarchy filtered to `btnApiEnvironment` and those three labels. | 1 |
| AC-002 | Review the false-gate path and release BuildConfig. | PASS | `BuildConfig.DEBUG` is false in release; LoginActivity sets the control GONE and clears its listener when the gate is false; `selectTarget()` rejects changes. | `GutterApiService.kt:287,305-308`; `LoginActivity.kt:193-197`; generated release `BuildConfig.java`; release build passed. | 0 |
| AC-003 | Select BASE on device; verify process reset; run focused endpoint/client unit tests; review repository service lookup. | PASS | Selecting BASE displayed `API：內網 BASE`; after force-stop/reopen it returned to `API：台北`. Tests confirmed all three URL mappings and distinct selected services. Repository resolves the selected service when API calls access its provider. | Physical device UI; `BackendEndpointsTest` (2 tests) and `GutterApiClientEnvironmentTest` (2 tests), all passed; `BackendEndpoints.kt:9-20`; `GutterApiService.kt:301-318`; `GutterRepository.kt:69-75`. No backend request was sent. | 0 |
| AC-004 | Review selected-service construction and endpoint routing. | PASS | Retrofit is built against the selected target URL; no cross-target fallback or retry exists. | `GutterApiService.kt:311-319`; `BackendEndpoints.kt:4-5,16-20`. | 0 |
| AC-005 | Build release and inspect the release gate. | PASS | Release build succeeded with `BuildConfig.DEBUG = false`; the false-gate path hides and detaches the control. | `:app:assembleRelease` passed; generated release `BuildConfig.java:7`; `LoginActivity.kt:193-197`. | 0 |

## Validation and Regression Review

- Focused tests plus debug build: `:app:testDebugUnitTest` for `BackendEndpointsTest` and `GutterApiClientEnvironmentTest`, then `:app:assembleDebug` — **PASS**, 4 tests, 0 failures, 0 skipped.
- Release build: `:app:assembleRelease` — **PASS**.
- Remote CI for the fixed revision — **PASS**: [Android CI run 36696611501](https://github.com/GTYang99/TaoYuanGutter/actions/runs/36696611501). The Unit tests and debug build job and all its steps completed successfully.
- Device steps did not submit login credentials or send API traffic. BASE selection and process-local reset were verified without backend reads or writes.
- Regression review: Taipei remains the default; WMS/WMTS constants remain fixed to Taipei; the selector is referenced only by LoginActivity and its login layout; no fallback was added.
- The first UI hierarchy read returned no root while the device was asleep; after waking it, the single retry succeeded. No test case was repeated after passing.
- No screenshot was retained. Automatic review rejected a full-device screenshot because it could expose prefilled login fields; filtered UI hierarchy evidence was used instead.

## Issues

無。

## Validation Limitations

無。A real API request was intentionally not sent; target routing is supported by the endpoint mapping and selected-service tests plus source review.

## Failure Classification

不適用（PASS）。

## Next Action

`release`

## Final Result

`PASS`
