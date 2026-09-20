# Verification Report

## Revision and boundary

- Revision: `fa6de4d`
- Branch: `fix/debug-0920-1-表單問題`
- Scope: API omission for tie-in/cant-open, tie-in inspection exemption, and focused regression checks for the existing form transitions.
- Existing untracked `.worktrees/` was not modified.

## Results

| Area | Result | Evidence |
|---|---|---|
| API request omission | PASS | `StoreDitchNodeRequestMapperTest` verifies mode flags remain while exempt detail keys, `IS_CONNECTING`, and photo IDs for slots 2/3 are absent. |
| Tie-in inspection display | PASS | `InspectionPresentationTest` and `GutterInspectTieInUiTest#tieInPointInspectionHidesExemptDetailsAndPhotos`. |
| Cant-open clear behavior | PASS | `GutterCantOpenUiTest#confirmDialogClearsAffectedFields` and `#cancelDialogKeepsOriginalState`. |
| Tie-in transition behavior | PASS | `GutterBasicInfoUiTest#tieInPointWarnsBeforeClearingAndCancelPreservesData`. |
| JVM regression | PASS | `:app:testDebugUnitTest`, 32 tests completed successfully. |
| Debug build | PASS | `:app:assembleDebug`. |
| AndroidTest compilation | PASS | `:app:compileDebugAndroidTestKotlin`. |
| Full connected regression | NOT VERIFIED | The full suite was intentionally not rerun after the user requested limited testing; an earlier attempt stalled in `RootViewPicker` with no resumed activity. |
| User physical-device acceptance | NOT VERIFIED | User will perform the final real-device test. |

## API acceptance interpretation

For `IS_CANTOPEN=true` or `IS_TIEINPOINT=true`, the request retains the mode-identification flags and omits the exempt parameters entirely. It does not send `null`, an empty string, `0`, or `false` for those omitted fields. Normal non-exempt nodes retain the existing `IS_CONNECTING` Boolean behavior.

## Regression review

- Normal nodes still serialize `IS_CONNECTING=false` when selected as `無`.
- Cant-open/tie-in precedence still prevents both mode flags from being true simultaneously.
- Photo slot 1 remains associated; exempt slots 2/3 are omitted from `img_ids`.
- Existing UI transition behavior was not broadened beyond the reported fields.

## Final verification state

`NOT VERIFIED` for full release readiness. The implementation and focused automated checks pass, but the requested final physical-device validation and full connected regression remain outstanding.
