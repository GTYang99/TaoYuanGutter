# Execution Report — DBG-1016 Coordinate Picker

## Scope

Continued root-cause investigation for the location picker, nearby import lookup, and imported-point camera symptoms. This phase was diagnostic only; production source was not changed.

## Runtime work

- User unlocked the Mac; booted the dedicated `Medium_Phone` AVD and restored the existing app session.
- Attached Android Studio's Java debugger to `com.example.taoyuangutter` (PID 4816).
- The app was on a blank “新增側溝” setup sheet. From the already-open unsent test session, opened the start-point form and three-tab existing-point import sheet without submitting the session.
- Recent Access returned candidates. Nearby tab entry and each locate action produced a coordinate-mode GET with HTTP 200 while the UI remained `無資料`.
- Moved emulator mock GPS about 2 km to C, then back to supplied B, with the same form open. System GPS/fused locations matched B with 5.0 m / 4.349 m accuracy; the app registered high-accuracy requests. The Nearby screen remained empty.
- The current map canvas rendered blank cyan without basemap or visible current-location marker; therefore this run cannot verify camera movement. Request coordinates were redacted and callback/policy values are not logged, so whether the fresh fix reached the callback or passed refinement remains NOT VERIFIED.
- After the user unlocked the Mac, Android Studio was attached to app PID 4816. A breakpoint on `shouldUseRefinement()` and a method breakpoint on `requestHighAccuracyImportLocation()` did not pause; the debugger had no frames/variables. This is inconclusive about callback execution and may reflect breakpoint binding or source/runtime mismatch. No raw coordinates were logged or exposed.
- A further locate tap issued one redacted `closestNodeDetails` GET (HTTP 200); Nearby remained `無資料`. Android location-service output attributed a HIGH_ACCURACY request to the app, followed by removal of the one-second request about one second later. This is consistent with callback delivery reaching the accuracy/policy path, but does not directly prove it. No second GET followed, making a refinement rejection plausible. A line breakpoint on `requestLocationUpdates()` also did not pause; callback/policy values and camera target remain NOT VERIFIED.
- No point was selected, imported, confirmed, or submitted. No APK was installed.

## Findings updated

- Added the unsuccessful breakpoint capture attempt, the subsequent tap/request evidence, and their limitations to `root-cause.md` and `issue-log.md`.
- Nearby's empty UI follows HTTP 200 in this run. The earlier sanitized response capture confirmed `success=true` and `data=[]` (99% confidence). The host-location-first/refinement mechanism remains the leading explanation, but this fresh-point run cannot prove whether B was the submitted query coordinate. Incident-specific location handoff, refinement callback, and camera target remain NOT VERIFIED.

## Validation

- No build or tests were run; there were no production-code changes, and the developer instruction prohibits running tests unless requested.
- ADB was used only for emulator lifecycle/runtime inspection under the previously authorized emulator investigation. No production point or gutter data was submitted.
- Physical Sony device behavior remains NOT VERIFIED.

## Result

- Phase remains `debug`; status remains `debug_in_progress`; next action remains `debug`.
- A safe form/import route is reachable and the debugger is attached, but breakpoint binding is unresolved. The platform registered a high-accuracy request after the tap, but callback/policy outcome, request-coordinate relation, and camera target remain NOT VERIFIED. Refinement rejection is the leading mechanism, with moderate confidence only. Phase remains `debug`; status remains `debug_in_progress`; next action remains `debug`.

### Source-matched debugger retry — 2026-10-16

- Rebuilt the current branch with Android Studio's bundled JBR using `:app:assembleDebug`; build succeeded. Installed that APK with `adb install -r` to `emulator-5554`, then opened the app's offline form path without entering credentials or submitting data. The app process is PID 8728 and Android Studio shows its Java debugger attached to `localhost:51929`.
- Android Studio opened the project `MyLocationController.kt`, whose editor content matches current source. The app is now in offline MainActivity; the Nearby import sheet is not open. This source-matched run did not recreate the reported Nearby scenario.
- After the user authorized continuing with the Mac unlocked, UI automation could still observe Android Studio but input calls failed with `noWindowsAvailable`; no new breakpoint could be set or exercised. The debugger session has not produced frames or callback values.
- Read-only ADB access initially failed because the sandbox could not start the ADB daemon; an approved `adb`-prefix escalation then confirmed both AVDs are connected and PID 8728 is alive. This provided no callback or camera evidence.
- Callback result, cached/refinement accuracy and timestamps, refinement-policy boolean, Nearby request coordinate, and camera target remain NOT VERIFIED. No point was selected, imported, confirmed, or submitted. Sony XQ-AU52 was not used.
- The APK build is verified as successful; no tests were run or added.
- With Android Studio brought to the foreground, a line breakpoint was successfully set on `MyLocationController.kt:86`, at `LocationFixQualityPolicy.shouldUseRefinement(accepted, refinement)`. No exception or lasting production source edit remains; the editor shows the original file contents and `git diff -- app/src/main/java app/src/test` is empty.
- `Medium_Phone` is confirmed as `emulator-5554` (Android 16) and the app process remains PID 8728. Android Studio's Device Manager lists this AVD as running, but the Running Devices panel is blank and the emulator has no accessible desktop window in CUA. The breakpoint has not yet been hit.

### Continuation after user clarified this is the same Session issue — 2026-10-16

- Stayed on `feat/feat-1015-restore-nearby-waypoints`; `git status` showed only the existing untracked `docs/tasks/DBG-1016-coordinate-picker/` investigation artifacts. No production source was changed.
- Rechecked the active Android Studio debugger through CUA. It had stopped in unrelated offline map-tile rendering (`OfflinePolygonTileProvider.getTile`); resumed the app and verified the debugger reports the process running. Read-only ADB then confirmed `emulator-5554` online and `GutterFormActivity` top-resumed.
- Android Studio's Device Manager marks Medium Phone as running, but Running Devices has no device surface. CUA can inspect the IDE but device interaction returns `noWindowsAvailable`. No picker/candidate action was performed in this continuation.
- Static source/history review confirmed the separate Recent/Search candidate path: `onSelected(item)` calls `onWaypointSelected(item)`, and `GutterFormActivity.showImportSelectedWaypoint()` animates the form map to parsed candidate coordinates. This proves the intended path, but without a captured selection event, valid runtime coordinates, highlight state, and camera target it does not identify why the user's candidate viewport stayed at A.
- Updated `root-cause.md` and `state.yaml` to supersede stale “Nearby callback not captured” notes with the direct ≥99% live-run cause, while keeping picker FAB, candidate-selection, and post-import data-return as distinct NOT VERIFIED findings below 95%.
- No point was selected, imported, confirmed, or submitted. No build/tests were run.
- Current phase remains `debug`, status `debug_in_progress`, next action `debug`.

### Shared picker-policy trace and current AVD state — 2026-10-16

- Continued the existing DBG-1016 analysis; no new task was opened and no production source changed.
- Read-only source/history trace found that `MapPointPickerActivity` delegates the FAB to `MyLocationController`: an accepted cached fix immediately moves the map, while a high-accuracy fix only replaces it if it is newer and improves accuracy by at least 10 m. This is consistent with the user's A/B symptom and blue dot, but the picker-specific callback and camera result remain uncaptured; confidence remains below 95% / `NOT VERIFIED`. `git blame` traces this shared gate to `4086c346` (`fix(debug-0917-location): use cached location first`, 2026-09-18).
- Recent/Search row selection still has source paths that add a highlight and animate to parseable candidate coordinates when `formMap` exists. There is no incident-time candidate payload or camera callback, so its root cause remains below 95% / `NOT VERIFIED`.
- Read-only ADB confirmed `emulator-5554` online and a screenshot showed the app on the start-point form. The AVD is listed in Device Manager as running, but Running Devices remains empty; CUA input returns `noWindowsAvailable`. No import/candidate/FAB action was issued, and no point was imported or submitted.
- No build or tests were run; this was diagnostic-only work. Phase remains `debug`; status `debug_in_progress`; next action `debug`.

### Visible AVD retry — 2026-10-16

- Inspected the AVD process arguments and confirmed it had been started with `-no-window`, explaining why Android Studio previously showed a blank Running Devices surface. Restarted the same dedicated `Medium_Phone` through Android Studio Quick Boot in visible mode.
- Android 16 boot completed. The current branch debug APK installed successfully. Android Studio's process chooser showed the app on `Medium_Phone`; the Java debugger attached to app PID 2662.
- The emulator displayed a `System UI isn't responding` dialog. After choosing Wait, the device transport closed; Android Studio reported the target disconnected and the debugger detached before any picker/Nearby locate action. Breakpoint 86 was not hit.
- This retry did not capture location callbacks or camera state. Root-cause confidence remains below 95%; exact callback, refinement decision, query coordinate, and camera target remain NOT VERIFIED. No point was confirmed or submitted, no physical Sony device was used, and no production source was modified.

### Live Nearby callback capture — 2026-10-16

- Continued in the same `DBG-1016` investigation and task branch; no separate debug task was created.
- Android Studio paused in the live `GutterFormActivity.requestHighAccuracyImportLocation()` callback at `shouldUseRefinement(accepted, accurate)`.
- `freshLocations` contained one fresh fused fix at 4.417 m accuracy. The accepted host fix in the same callback frame was 5.0 m; the debugger expression returned `(5.0, 4.417)`.
- Stepping over the condition landed after the full `if` body, proving the policy returned `false`. Source requires at least 10 m accuracy improvement; the observed improvement was 0.583 m. The callback therefore skipped `showImportMyLocationMarker()` and `startNearbyAutoSearch()`.
- This directly confirms why the fresh location caused neither a Nearby marker/camera update nor a refinement API call in this captured run. The initial host fix had already driven the first query. Exact initial query coordinates and the reason the backend returned no records remain unverified.
- Separate picker FAB, candidate-selection camera, and post-import A/B data-return symptoms remain NOT VERIFIED. No point was selected, imported, confirmed, or submitted. No production source was changed and no tests were run.

### Feature-session attribution follow-up — 2026-10-16

- Compared the live callback with history: `3b38be9d` (2026-10-15) restored Nearby and routed it through the host GPS/refinement flow; the ≥10 m shared quality gate originated in `4086c346`.
- In the captured Nearby run, the restored feature received a fresh 4.417 m fix against an accepted 5.0 m fix, rejected it at the 10 m refinement gate, and skipped the marker/camera update plus fresh-location query. This is the cause of the captured Nearby stale-location failure at ≥99% confidence.
- The standalone picker FAB and Recent/Search selection camera have different runtime paths and remain below 95% / NOT VERIFIED. No production code, form data, or tests were changed in this follow-up.

### AVD access retry — 2026-10-16

- Revalidated `emulator-5554` online and captured its screen read-only. The app is on the start-point form with the map and existing-point import entry visible; no data was imported or submitted.
- Android Studio Device Manager marks Medium Phone running, but “Show” did not make a CUA-controllable device surface available; Running Devices remains empty. Picker-FAB and candidate-selection runtime evidence remains blocked on surfacing the emulator window.
- No production files changed and no tests/build were run.

### AVD visibility retry after user unlocked the Mac — 2026-10-16

- Android Studio became CUA-accessible after the user unlocked the Mac. Device Manager continued to show Medium Phone as running; Running Devices remained empty.
- Retried the Device Manager Show action, but no emulator surface appeared in the app/window inventory. No app navigation, location action, or data change occurred. Picker-FAB and candidate-selection runtime evidence remains NOT VERIFIED.
- No production files changed and no tests/build were run.

### Legacy-flow comparison — 2026-10-16

- Compared approved `feat-1015` AC-003 and the restored callback against the exact pre-0917 baseline `e32432d1` (2026-09-17), which AC-003 identifies. Legacy Nearby accepted the first fresh fix with accuracy ≤30 m and immediately updated the marker and queried with that coordinate. The current callback instead compares against an accepted host/cache fix using the ≥10 m `LocationFixQualityPolicy` introduced in `4086c346` (2026-09-18).
- `git blame` confirms the current callback's 10 m refinement gate and guarded camera/query update were introduced by `4086c346`; feature-restoration commit `3b38be9d` reactivated Nearby but did not modify the gate. This distinguishes the originating change from the session that reconnected the Nearby feature to it.
- The captured 5.0 m accepted / 4.417 m fresh pair improved by only 0.583 m. The predicate returned false; because current code stops location updates before the predicate, it neither moves/query-refines with the fresh fix nor waits for another fix. This confirms the app-side Nearby regression against AC-003 for the captured run (≥99%). It does not explain the backend's first `data=[]` result because the initial request coordinates were not captured.
- The user confirmed the Android Studio Device Manager Show action was completed. Rechecking through CUA still shows Medium Phone as running but no Running Devices surface or separate emulator app/window; no new picker/candidate runtime evidence was obtained. No point was imported or confirmed, and no production source was changed.
- Standalone picker camera and imported-coordinate return remain separate NOT VERIFIED findings; the Nearby root cause is now classified as `implementation_regression`.

### Picker camera-command capture — 2026-10-16

- The Medium Phone AVD is now visible in Android Studio's Running Devices panel; the Java debugger is attached to the app.
- From the standalone picker screen, tapped the lower-right location FAB. A debugger capture showed a fresh fused fix with 4.603 m accuracy. The refinement comparison against an equal-accuracy accepted fix returned false; the camera target at that callback remained the picker’s default center.
- For a later tap, a breakpoint in `MyLocationController.moveMap()` was hit on the `map.animateCamera(...)` invocation. Its `location` local was a fused current fix with 4.603 m accuracy. This establishes that the app reaches the camera-command path for the tap.
- The debugger paused before the camera call. Stepping moved into a Google Play Services task frame; resuming did not yield a post-call camera target. The picker uses `GoogleMap.MAP_TYPE_NONE` with a fixed center reticle and the visible screen cannot establish camera movement. Exact saved-point A→B flow and the animation completion/result remain NOT VERIFIED.
- No point was confirmed or submitted. No production source was changed and no tests/build were run. The standalone picker root cause remains below 95%; the Nearby refinement-gate regression remains ≥99% for its captured run.

### Coordinate handoff clarification — 2026-10-16

- Re-read the live source path against the user's confirmed action: they tapped the picker location FAB but did not press bottom `確定`.
- The FAB callback in `MapPointPickerActivity` intentionally does not update form data. The `確定` handler alone returns the current camera target in `RESULT_OK`; `GutterFormActivity.locationPickerLauncher` ignores canceled/non-OK results and updates the current form, fragment, and session waypoint only for a valid OK result.
- Therefore no new form coordinate after that operation is expected behavior (≥99% confidence). This resolves that sub-symptom and does not explain the separately reported camera staying at A or the three-tab import candidate/return behavior.
- No production code, test, or build was changed/run. Current phase remains `debug`, status `debug_in_progress`, next action `debug`.

### Feature-session attribution check — 2026-10-16

- Compared the active `feat-1015` restoration commit `3b38be9d` to current `HEAD` for `MapPointPickerActivity.kt` and `MyLocationController.kt`.
- `MyLocationController.kt` is unchanged across that range. Picker changes in the range are limited to replacing remote WMS polygon tile overlays with the offline polygon manager and releasing that manager in `onDestroy`; the location-button, camera, and result-handoff paths are unchanged.
- The cached-first / ≥10 m refinement policy predates the restoration session (`4086c346`, 2026-09-18). This rules out the current `feat-1015` code diff as the source of the standalone picker’s camera behavior with high confidence, while leaving its exact incident-time branch unresolved.
- Nearby is distinct: `feat-1015` newly reconnected the Nearby flow to this shared refinement gate rather than restoring the pre-0917 ≤30 m fresh-fix behavior. The directly captured Nearby regression remains ≥99%.

### Host-location and import-return source trace — 2026-10-16

- Traced MainActivity's location callback through form launch: a successful callback stores the exact fix in `lastKnownLocation`; the add/inspect launch routes pass it through `GutterFormNavigator`, which attaches coordinates, timestamp, and accuracy to the intent. If the user's main-map move to B and form opening were in the same MainActivity instance, stale A host data is not the leading static explanation. Incident intent extras were not captured.
- Traced Recent/Search selection and import: a selected row requests a highlight and `animateCamera` to valid candidate coordinates; the import callback pre-fills the form fragment's coordinates and notifies draft change; collected `NODE_X`/`NODE_Y` merge into form state, session waypoint, and successful result; MainActivity then updates the parent waypoint from that result. Static source shows no confirmed B-to-A overwrite when the candidate is valid and the form returns `RESULT_OK`.
- This does not prove the incident's row selection, import callback, valid candidate fields, camera animation completion, or result payload. Candidate viewport and incident-specific A/B return root causes remain NOT VERIFIED and below 95%.
- No emulator interaction or form submission was performed in this continuation. No production code changed; no tests/build run. Current phase remains `debug`, status `debug_in_progress`, next action `debug`.

### Current emulator recheck — 2026-10-16

- Revalidated the authorized AVD with read-only ADB: `emulator-5554` is online, `GutterFormActivity` is top-resumed, and a screenshot shows the start-point form with the import-entry control. This is not the picker or candidate sheet, so it does not provide incident callback evidence.
- Android Studio remains on the active branch with the Java-only debugger session resumed; its embedded Running Devices surface has no rendered device content in the accessibility tree. No UI action, import, coordinate confirmation, or submission was performed.
- Static tracing confirms MainActivity's successful location callback supplies its current saved fix to GutterFormActivity on launch. For an imported candidate, the form returns candidate coordinates to the parent only on successful `RESULT_OK`; a canceled/back return leaves the parent waypoint unchanged. Whether the incident form was completed before returning remains to be confirmed.

### User clarification on form exit — 2026-10-16

- The user says they used system Back rather than the purple Complete button when returning from the form.
- Source review shows ordinary `GutterFormActivity.handleNavigateBack()` calls `buildAndFinishWithResult()` and sets `RESULT_OK`; if required inputs are incomplete, it first displays an incomplete-exit warning, and the positive `確認` action also returns `RESULT_OK`. Only a genuine `RESULT_CANCELED` leaves the parent's original waypoint untouched. Therefore “did not press purple Complete” alone does not explain A remaining in the parent. At this point, the incident result payload remains the missing evidence.
- No incident result code/payload was captured; no UI action was performed during this continuation.
- Follow-up answer: the user says system Back showed the `尚未填寫完畢` warning; they pressed `確認` and returned to `AddGutterBottomSheet`. This path calls `buildAndFinishWithResult()` and returns `RESULT_OK`, so the earlier canceled-result hypothesis does not fit this reported exit sequence. The form/result coordinates and parent camera target remain uncaptured.
- Follow-up answer: the user does not remember whether the form coordinate fields showed A or B before system Back. Source trace confirms the import button callback prefills form data and dismisses the import sheet, but does not itself pan the map or redraw the session waypoint marker; those effects depend on the earlier candidate-row selection callback. This narrows what must be captured but leaves the incident's selected-row callback, form coordinate values, result payload, and parent camera target unverified.

### Acceptance-criteria and environment recheck — 2026-10-16

- Re-read the active feat-1015 ACs and current issue/task state. AC-003 explicitly requires legacy nearby marker/camera and re-query behavior; the captured 5.0 m → 4.417 m refinement rejection remains a confirmed implementation regression for that run. AC-005 requires shared candidate markers, selected-point highlight, and import; current instrumentation checks marker object state but does not assert integrated rendered camera movement or this incident's result payload.
- CUA recheck returns `The Mac is locked`; the AVD/debugger remains unavailable for a fresh incident capture. No UI action was performed. No production source or tests were changed, and no build/test was run.
- Current outcomes: Nearby captured-path cause ≥99%; standalone picker camera NOT VERIFIED / <95%; imported candidate selection and A/B return NOT VERIFIED / <95%; form coordinate absence when the picker was left without Confirm ≥99% expected behavior. Keep `phase: debug`, `status: debug_in_progress`, `next_action: debug`.


### Candidate import callback capture — 2026-10-16

- With the user-authorized Medium Phone AVD visible, opened the three-tab import sheet and selected a Recent candidate. The source breakpoint in `showImportSelectedWaypoint()` was hit for the row callback; the `NodeDetails` item had parseable latitude/longitude and an identifier. The map showed candidate markers and the selected row. This verifies the callback path for a functioning Recent candidate, but it was not the user-reported Nearby/B incident and the camera target was not numerically captured.
- Tapped the non-final purple `匯入` button. Android Studio then paused at the entry to `handleImportedNodeDetails()` (line 650), proving the selected item reached the import callback. The debugger disconnected from the target VM before the argument/form coordinate values could be read. No final Complete/Save action was taken.
- The runtime capture therefore confirms Recent row selection and import-callback delivery in this controlled session, but does not establish whether the imported candidate coordinates reached the BasicInfo fields or whether the viewport moved from A to B in the reported Nearby scenario. Those incident-specific conclusions remain NOT VERIFIED.
- No production source was changed and no tests/build were run. The app/debugger connection was lost during inspection; whether the target process exited or the debug connection dropped for another reason is unknown.
- Follow-up environment check: Android Studio Device Manager now shows Medium Phone stopped, Running Devices has no emulator surface, and its target-VM console records a disconnect. The UI click to launch the AVD failed with `windowNotFoundAtPosition`; no app data or code was changed. Further runtime capture is blocked until the dedicated emulator is available again.


### Handoff source audit and AVD restart retry — 2026-10-16

- Re-read the entire import handoff: fragment prefill sets coordinate state from non-empty candidate fields and emits a draft change; the activity merges draft data; successful form exit returns resolved coordinates; MainActivity applies the returned coordinates and updates waypoint data before fitting the camera. This is positive static evidence for the expected B path when the candidate data reaches the callback, but it is not evidence that the user's incident did so.
- Retried starting Medium Phone from Android Studio. Quick Boot selection returned to the IDE, but Device Manager still shows the AVD stopped and Running Devices remains empty. The target VM console still records disconnection. No further runtime callback/data capture is available in the current state.
- No production source changed; no tests/build were run; no final save or submit occurred.

### User-unlocked retry — 2026-10-16

- The user confirmed the Mac was unlocked. Android Studio was accessible; its target selector showed Medium Phone, but the debugger console still reported a disconnected target VM and Device Manager/Running Devices showed no running emulator. A Quick Boot retry returned to Android Studio without opening a device window.
- The `handleImportedNodeDetails()` breakpoint remained visible, but the app was not relaunched. No new import, form-coordinate, result-payload, or camera evidence was captured. This remains an environment limitation for runtime follow-up, not a new app finding.
- No production source changes, build/tests, or final save/submit actions were performed.

### AVD boot retry — 2026-10-16

- Read-only SDK inventory identified the existing `Medium_Phone` AVD. Direct boot attempts with the default software renderer and with host graphics plus snapshot loading disabled both hung in the emulator QEMU main loop and showed crash-dialog diagnostics. `adb devices -l` remained empty; both hung emulator processes were stopped.
- No app launch, callback capture, build, test, final save/submit, or production source edit occurred. Runtime capture remains an environment limitation.

### Import screen source-path reconciliation — 2026-10-16

- Source search confirms the current form launches `ImportExistingWaypointBottomSheet` for the three-tab flow. The separate `ImportExistingWaypointActivity` has no current launcher/call site in the source tree. The bottom sheet delegates selection to the map/highlight callback and Import to `handleImportedNodeDetails()`.
- The captured Nearby host-fix/refinement-gate path explains the A-centered request/camera and empty Nearby list for the captured run (≥99%). Incident-specific selection, form fields, result extras, and parent camera remain NOT VERIFIED / below 95%.

### User opened Nearby tab — 2026-10-16

- User confirmed the Nearby tab was open in Medium Phone; no candidate Import or final save was tapped. ADB confirmed `GutterFormActivity` was top-resumed.
- Sanitized OkHttp logs show the Recent no-coordinate request returned HTTP 200 and the Nearby coordinate-mode request to the same endpoint also returned HTTP 200. The user reported the Nearby list still showed no data. The response body was not retained; exact request coordinates are redacted.
- This corroborates that the empty result is returned through a successful HTTP response. The debugger was no longer shown as attached in the IDE follow-up state, so this attempt did not capture the location callback, refinement decision, submitted coordinate, or camera target. The earlier direct callback capture remains the evidence for the ≥99% refinement-gate attribution.
- No candidate was selected/imported and no form was saved. No production source was changed; no test was run. Keep incident-specific candidate/form/result/camera findings NOT VERIFIED.
- During an IDE follow-up, a stale accessibility index hit `Apply Code Changes` instead of the debugger-attach action. Android Studio briefly showed `Gradle Build Running`; no source changes had been made for application code, and no app data was changed. Build completion was not established, so no build result is claimed and CI/build state remains pending.

## Nearby bugfix and validation — 2026-10-16

- Scope: `ISS-DBG-1016-NEARBY-003`, `feat-1015` AC-003/AC-005. The fix accepts the first fresh Nearby fix within the existing 30 m accuracy threshold and always updates the location marker/camera and re-queries with that fix. The shared `LocationFixQualityPolicy` and its other callers are unchanged.
- Added `NearbyLocationFixPolicyTest`: the captured 5.0 m → 4.417 m case is accepted; stale, future-dated, missing-accuracy, and over-30 m cases are rejected.
- `./gradlew :app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin` — PASS (`BUILD SUCCESSFUL`). The sandbox initially blocked Gradle's user cache; rerunning with the installed Android Studio JDK and authorized Gradle cache access passed.
- `./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.taoyuangutter.ImportExistingWaypointUiTest` — PASS on `Medium_Phone (AVD) - 16`, serial `emulator-5554`: 1 test, 0 failures. The test checks tab/query behavior and candidate callback/import wiring using a fake API.
- The supplied known-good coordinate was set on the dedicated AVD. The instrumentation runner returned to the launcher and cleared the app login state; the app is now at login and requires the user to sign in before the real-backend result/list check. That live endpoint/UI check is **NOT VERIFIED** pending sign-in. The reported `data=[]` at a different location is a successful empty result, not a transport failure.
- No physical device was connected. No candidate was imported, confirmed, or saved.
- Debug APK: `app/build/outputs/apk/debug/app-debug.apk`; package `com.example.taoyuangutter`.

## Current result

- Fix is implemented; code-level validation passed. Real-backend confirmation at the known-good location remains pending user sign-in on the AVD.
- Sony XQ-AU52 was not used, as requested.
