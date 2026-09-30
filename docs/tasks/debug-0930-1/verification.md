# Verification

## Revision Under Review

- Fixed implementation revision: `397ebd7d5cd76f3ce1286aba279a58cafd4528f9`
- Branch: `codex/debug-0930-1`
- Worktree was clean before this verification record was created.
- This report records developer-side evidence; an independent verifier has not reviewed the revision.

## Acceptance Criteria

| AC | Result | Evidence |
|---|---|---|
| AC-001 | NOT VERIFIED | `activity_main.xml` declares `btnApiEnvironment`; `MapWorkspaceFragment.setupApiEnvironmentButton()` exposes it when the gate is true and opens the selector. Debug build and instrumentation source compile. The planned emulator UI run was not performed: ADB found only a physical Xperia device, while this plan explicitly scoped emulator instrumentation. |
| AC-002 | PASS (source/release compile) | When the gate is false, the view is `GONE`, disabled, and has no click listener; `GutterApiClient.selectTarget()` rejects changes. `:app:compileReleaseKotlin` passed. |
| AC-003 | PASS (unit/source) | `BackendEndpointsTest.apiBackendTargetsMapToApprovedUrls` verifies all three URLs. `GutterApiClientEnvironmentTest` verifies Taipei default and distinct cached services for BASE and DEMO. `GutterRepository` resolves the current service for each request. No live request was sent. |
| AC-004 | PASS (source review) | `GutterApiClient` builds only the selected target service and contains no fallback branch. The shared HTTP client does not redirect API failures to another backend. |
| AC-005 | PASS (source/release compile) | The UI gate is `BuildConfig.DEBUG`; false hides and detaches the selector. Release Kotlin compilation passed. |

## Test and Build Evidence

- Targeted unit tests passed: `BackendEndpointsTest`, `GutterApiClientEnvironmentTest`.
- Repository CI local-parity tests passed: `PhotoUploadSlotStateTest`, `PhotoUploadCandidateResolverTest`, `StoreDitchNodeRequestMapperTest`.
- `:app:assembleDebug` passed.
- `:app:compileDebugAndroidTestKotlin` passed.
- `:app:compileReleaseKotlin` passed.
- `git diff --cached --check` passed before implementation commit.
- Remote CI result: `NOT VERIFIED`.
- Real API reads/writes: not run.

## Regression Review

- Default API target remains Taipei.
- Existing `ACTIVE_API_TAPIEI_URL` constant remains an alias for the Taipei URL.
- WMS/WMTS endpoint constants were not changed.
- Repository callers that pass a fixed fake `GutterApiService` retain that behavior through the compatibility constructor.
- Cross-target fallback was not added.

## Final Result

`NOT VERIFIED`: AC-001 needs the planned emulator UI run. Remote CI and independent verification are also still outstanding. No implementation failure was found in the source or executed unit/build checks.

## Next Action

Keep the task in Verification until an independent review and the scoped emulator UI evidence are available. Then record the remote CI result before Release.
