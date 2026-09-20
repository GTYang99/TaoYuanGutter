# Verification Report

## Revision and boundary

- Revision: `3833484`
- Branch: `fix/debug-0920-1-表單問題`
- Scope: API omission for tie-in/cant-open, tie-in inspection exemption, form transitions, and preservation of empty `IS_CONNECTING` during inspection-to-edit handoff.
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

## Final follow-up verification — empty `IS_CONNECTING`

| Area | Result | Evidence |
|---|---|---|
| Edit preload empty-value handling | PASS | User confirmed the physical-device flow has no issue after commit `3833484`. |
| Connection-pipe radio state | PASS | Empty `IS_CONNECTING` does not select either radio button in the tested edit flow. |
| Exempt-mode request omission | PASS | Existing mapper tests plus user physical-device confirmation; `IS_CONNECTING` is omitted from the request. |

## Final verification state

Implementation scope verification: `PASS`.

CI and release records were not run in this workspace and remain pending separately.

## API acceptance interpretation

For `IS_CANTOPEN=true` or `IS_TIEINPOINT=true`, the request retains the mode-identification flags and omits the exempt parameters entirely. It does not send `null`, an empty string, `0`, or `false` for those omitted fields. Normal non-exempt nodes retain the existing `IS_CONNECTING` Boolean behavior.

## Regression review

- Normal nodes still serialize `IS_CONNECTING=false` when selected as `無`.
- Cant-open/tie-in precedence still prevents both mode flags from being true simultaneously.
- Photo slot 1 remains associated; exempt slots 2/3 are omitted from `img_ids`.
- Existing UI transition behavior was not broadened beyond the reported fields.

## Final verification state

`NOT VERIFIED` for full release readiness. The implementation and focused automated checks pass, but the requested final physical-device validation and full connected regression remain outstanding.
