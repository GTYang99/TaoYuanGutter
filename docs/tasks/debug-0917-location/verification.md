# Verification Report

## Revision and Environment

| Item | Evidence |
|---|---|
| Production revision under test | `4086c34 fix(debug-0917-location): use cached location first` |
| Task-document revision at test start | `2fa9230054942f7405ef969daad0ef9dc9e3e9fc` |
| Worktree | No tracked-file changes; unrelated untracked `.worktrees/` was preserved. |
| Device | Sony XQ-AU52, Android 12, `adb-QV710EDR3A-hF5XZF._adb-tls-connect._tcp` |
| Package | `com.example.taoyuangutter` debug APK built from the production revision |

## Acceptance Criteria

### AC-001 — NOT VERIFIED (partial physical evidence)

Expected: every applicable current map screen centers from a usable recent location without waiting for a new GPS fix.

- **Main workspace map:** PASS for this entry point. After panning away from the blue-dot location, the `現在位置` control was tapped at `2026-09-18 06:52:46 +0800`. The capture taken 0.25 s later already showed the blue dot centered; the 1.0 s capture remained centered.
- **Current add-flow map picker:** PASS for this entry point. After panning away, its current-location control returned the map crosshair to the blue-dot location within the 0.25 s capture window at `2026-09-18 06:56 +0800`.
- **Standalone `MapPointPickerActivity` and form/import nearby lookup:** NOT VERIFIED on device. The exercised current add flow uses the workspace picker overlay. The authenticated account's recent-point request returned HTTP 422 and did not expose a nearby-location control in the available import sheet.

The shared controller and source routing support the remaining paths, but source inspection cannot substitute for their physical acceptance evidence.

### AC-002 — NOT VERIFIED

Expected: a newer high-accuracy result updates state and corrects only when materially better.

- `LocationFixQualityPolicyTest` passed for the 10 m improvement, non-improvement, stale, null, and timestamp cases.
- Android location diagnostics showed each user action registered `HIGH_ACCURACY` GPS work for `com.example.taoyuangutter`.
- During this session the available fused location was network-based with `hAcc=27.637 m`; no newer materially better GPS fix arrived. Therefore no second correction occurred, as required by the policy, but the accepted-correction path was not physically observed.

### AC-003 — NOT VERIFIED

Expected: permission denial, unavailable location, and high-accuracy timeout give the existing-equivalent outcome and leave no active callback.

- Source review confirms controller cancellation on main-map and point-picker destruction, plus import callback removal and its existing 25-second timeout.
- Permission-denial, unavailable-location, timeout, and post-close callback cases were not executed on the physical device in this run.

## Automated Evidence

| Check | Result |
|---|---|
| Focused JVM tests: `LocationFixQualityPolicyTest`, `MainMapLocationRecenterReloadTrackerTest` | PASS |
| Debug APK build: `./gradlew assembleDebug` with Android Studio JBR | PASS |
| Static whitespace check: `git diff --check` | PASS |
| CI | NOT VERIFIED — no CI result is available in the repository context. |

## Regression Review

- Main-map recenter completed and triggered the existing side-layer reload toast at zoom 18.
- Current add-flow picker returned to the location without confirming or saving a selected point.
- The import sheet displayed an existing HTTP 422 data-query failure; this validation did not import or modify data.
- The unfilled form showed its existing `尚未填寫完畢` guard when closing; no submission action was taken.

## Result

**NOT VERIFIED.** The fixed build has direct physical evidence for immediate cache-first centering in the main map and current picker, but the full cross-screen set, accepted high-accuracy correction, and error/lifecycle cases lack execution evidence. Release remains blocked.

## Next Action

Run the remaining physical cases with a session/data state that exposes the standalone picker and form nearby lookup, and with a fresh GPS fix that improves accuracy by at least 10 m. Then rerun permission-denial, unavailable-location, and timeout/post-close cases.
