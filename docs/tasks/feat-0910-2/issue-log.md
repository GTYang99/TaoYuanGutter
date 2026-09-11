# Issue Log

## ISS-0910-2-01

- Task: feat-0910-2
- Phase: implementation
- Category: environment
- Priority: P1
- Status: open
- Title: Android validation environment has no Java runtime
- Impact: Gradle compilation, unit tests, Android UI tests, build, and lint cannot be executed locally.
- Evidence: `./gradlew compileDebugAndroidTestKotlin --no-daemon` stopped with `Unable to locate a Java Runtime`.
- Next action: verification

## ISS-0910-2-02

- Task: feat-0910-2
- Phase: debug
- Category: `implementation_regression`
- Priority: P0
- Status: resolved
- Title: Form entry crashes during runtime View reordering
- Impact: Core form validation is blocked and release readiness cannot be established.
- Evidence: Real-device logcat at 2026-09-10 16:12:01: `IllegalStateException: The specified child already has a parent` at `GutterBasicInfoFragment.reorderEditableSections(GutterBasicInfoFragment.kt:511)`, called from `onViewCreated()`.
- Root cause: nested Views remain attached to their original parent when `formContent.addView()` is called.
- Repro steps: Open the feat-0910-2 form on Sony XQ-AU52.
- Resolution evidence: parent-safe reordering was installed and the user subsequently entered the form without a crash.
- Next action: retain the original crash log as historical evidence.

## ISS-0910-2-03

- Task: feat-0910-2
- Phase: implementation
- Category: environment
- Priority: P1
- Status: open
- Title: Photo upload API returns server SQL schema error
- Impact: Photo upload completion cannot be confirmed.
- Evidence: Device dialog reports HTTP 500 and SQL Server error `Invalid column name 'xy_num'` while updating `Map_ditch_nodes_images`; SQL includes `node_id`, `xy_num`, `uploaded_by`, and image IDs.
- Client comparison: the Android photo multipart request sends only `file`, `node_id`, and `fileCategory`; the feat-0910-2 diff did not change the photo upload request.
- Classification: backend/API schema mismatch, not a client field rename proven by current evidence.
- Next action: backend owner to align the image table/API query, then rerun photo upload verification.

## ISS-0910-2-04

- Task: feat-0910-2
- Phase: implementation
- Category: `implementation_regression`
- Priority: P1
- Status: resolved
- Title: Field titles shift after photo cards become visible
- Impact: Measurement fields become visually detached from their labels after all three photos are captured.
- Evidence: Real-device report after capturing photo slots 1-3; affected titles are width, depth, material, broken, hanging, and silt.
- Fix scope: ensure the correct outer title rows are used and explicitly request a form hierarchy remeasure after photo visibility changes.
- Resolution evidence: user confirmed the affected titles remain correctly positioned after capturing three photos.
- Next action: retain the real-device result as manual regression evidence.

## ISS-0910-2-05

- Task: feat-0910-2
- Phase: implementation
- Category: `implementation_regression`
- Priority: P1
- Status: resolved
- Title: Field-title mapping uses virtual wrapper instead of individual rows
- Impact: Width, depth, material, broken, hanging, and silt titles start in the wrong positions.
- Evidence: The affected title rows are direct children of `llVirtualHidden3`; the previous ancestor helper returned the wrapper itself, so the individual rows were not interleaved with their matching controls.
- Fix scope: return the direct child row when its parent is a virtual section, then apply the approved field order.
- Resolution evidence: user completed real-device validation and reported no issue with the initial order or the three-photo state.
- Next action: retain automated regression coverage as a separate NOT VERIFIED item until the build environment is available.

## ISS-0910-2-06

- Task: feat-0910-2
- Phase: verification
- Category: `verification_failure`
- Priority: P1
- Status: open
- Title: Connected regression report has an Espresso visibility failure
- Impact: AC-006 cannot be verified; the affected `GutterCantOpenUiTest` result is 1 failure out of 2 tests.
- Evidence: `app/build/outputs/androidTest-results/connected/debug/TEST-Medium_Phone(AVD) - 14.xml`; `cancelDialogKeepsOriginalState` failed while setting `etDepth` because Espresso reported a non-empty global visible rectangle was unavailable.
- Classification: provisionally `environment`; the test cannot currently be rerun because the CLI environment has no Java Runtime, so implementation regression is not established.
- Next action: infrastructure

## ISS-0910-2-11

- Task: feat-0910-2
- Phase: debug
- Category: `implementation_regression`
- Priority: P1
- Status: open
- Title: Virtual-point off action no longer restores normal form behavior
- Impact: Users can enter virtual-point mode but cannot fully return to the original normal interaction state.
- Evidence: Current `GutterFormActivity.applyVirtualModeUi()` hardcodes `switchPageBar` to `GONE` and `viewPager.isUserInputEnabled` to `false`. Original behavior in the pre-UI-redesign implementation used `if (isVirtual) GONE else VISIBLE` and `!isVirtual`; both changes are visible in commit `cb3b90f`.
- Root cause: Activity-level virtual-mode UI handling was changed from state-dependent behavior to unconditional hiding/disabling during the UI redesign. The checkbox listener and Fragment field visibility logic still exist, so the defect is specifically the missing Activity-level off-state restoration.
- Minimum fix: restore the virtual on/off state transition contract, hide the inapplicable `cbCantOpen` while virtual mode is active, and add focused toggle regression coverage. Implemented in the current follow-up.
- Next action: implementation_debug

## ISS-0910-2-07

- Task: feat-0910-2
- Phase: verification
- Category: `implementation_regression`
- Priority: P1
- Status: resolved
- Title: Photo layout remeasure callback uses destroyed fragment binding
- Impact: Configuration recreation can crash with a `NullPointerException` after photo state rendering schedules a layout request.
- Evidence: `GutterCantOpenUiTest.snapshotSurvivesConfigurationRecreation` failed at `GutterBasicInfoFragment.renderPhotoSectionState` after `_binding` was cleared.
- Fix scope: resolve the current nullable binding inside the posted callback and return when the view lifecycle has ended.
- Resolution evidence: the subsequent emulator report recorded all four `GutterCantOpenUiTest` cases passing.
- Next action: retain lifecycle recreation as a regression check.

## ISS-0910-2-08

- Task: feat-0910-2
- Phase: verification
- Category: `implementation_regression`
- Priority: P1
- Status: resolved
- Title: Offline form drops existing Intent data before prefill
- Impact: Existing broken/silt selections were lost in offline edit/test flows, preventing AC-004 preservation.
- Evidence: `GutterBasicInfoUiTest.existingBrokenAndSiltValuesArePreserved` failed because the offline branch selected `buildEmptyData()` without merging `GutterFormContract.readFormData(intent)`.
- Fix scope: merge Intent form data over the offline empty-data template while preserving the existing draft branch.
- Resolution evidence: source fix compiled successfully; connected re-test was blocked by disconnected devices.
- Next action: rerun `GutterBasicInfoUiTest` on a connected device.

## ISS-0910-2-09

- Task: feat-0910-2
- Phase: verification
- Category: `implementation_regression`
- Priority: P1
- Status: open
- Title: Form Activity is destroyed before new-form and existing-data UI assertions
- Impact: The new-form path cannot verify AC-001, AC-002, AC-003, or AC-005; the existing-data path cannot verify AC-004. The connected suite fails before the form assertions can complete.
- Evidence: `TEST-XQ-AU52 - 12.xml` and the targeted rerun report; `GutterBasicInfoUiTest.newFormShowsRequiredOrderLabelsButtonsAndDefaults` fails twice with `NoActivityResumedException` at line 23. Logcat shows `GutterFormActivity` transitions to `DESTROYED` before the assertion completes, without a matching app `FATAL EXCEPTION`.
- Repro steps: Run `JAVA_HOME=/Applications/Android Studio.app/Contents/jbr/Contents/Home ./gradlew connectedDebugAndroidTest --no-daemon -Pandroid.testInstrumentationRunnerArguments.class=com.example.taoyuangutter.GutterBasicInfoUiTest#newFormShowsRequiredOrderLabelsButtonsAndDefaults` on `XQ-AU52 - 12`.
- Expected: New-form activity remains resumed long enough for the UI assertions to complete.
- Actual: Activity is destroyed and Espresso raises `NoActivityResumedException`.
- Root-cause status: reassessed. The current revision defers `SupportMapFragment` creation until `onPostResume()`, while the latest failure pauses the Activity before `GutterBasicInfoFragment.onCreate`; therefore the previous synchronous-Maps explanation is stale for this recurrence.
- Historical evidence from an earlier revision: a targeted run passed once and failed once while `SupportMapFragment.onCreateView()` took 203–226 ms. This supported the earlier map-startup hypothesis, but it cannot explain the current recurrence because map creation is now deferred and the latest failure pauses before Fragment setup.
- Root-cause evidence still confirmed: the system moves `GutterFormActivity` out of the resumed state; no app fatal exception or explicit `finish()` is present. The trigger remains under investigation.
- Recurrence evidence (2026-09-11): after the follow-up commits through `64ac477`, a real-device `connectedDebugAndroidTest` run failed both `newFormShowsRequiredOrderLabelsButtonsAndDefaults` (127.416 s, line 25) and `existingBrokenAndSiltValuesArePreserved` (143.303 s, line 63) with `NoActivityResumedException`. Logcat shows the Activity being destroyed after each Espresso timeout and no app `FATAL EXCEPTION`. The failure occurs before the later device disconnect, so it is an implementation regression rather than solely an environment limitation.
- Resolution status: prior implementation is not verified; recurrence requires a new root-cause investigation.
- Next action: debug

- Current classification: environment/test lifecycle failure is approximately 85% likely; the translucent `FormSheet` theme alone is below 50% likely. The opaque-theme experiment was applied and reverted because the targeted rerun reproduced the same `Activity top resumed state loss timeout`. A production `MainShellActivity` launch comparison is still required before changing Activity window behavior.
- Final isolation update: XQ-AU52 was asleep during the failing runs (`mWakefulness=Asleep`, `always_finish_activities=0`). After waking, the targeted new-form test passed. The remaining class-run lifecycle failures are associated with Android Test `EmptyActivity` task handoff between scenarios; no production crash was reproduced. No production workaround was retained.
- Test-task isolation update: `GutterBasicInfoUiTest.launchForm()` now adds `FLAG_ACTIVITY_CLEAR_TASK`. The complete `GutterBasicInfoUiTest` class passed 3/3 and `GutterCantOpenUiTest` passed 4/4 on XQ-AU52. This provides direct evidence that cross-scenario task handoff was the trigger; no application crash was observed.

## ISS-0910-2-10

- Task: feat-0910-2
- Phase: verification
- Category: `environment`
- Priority: P1
- Status: open
- Title: Full connected regression cannot start because no device is connected
- Impact: AC-006 cannot receive complete post-fix evidence.
- Evidence: `JAVA_HOME=/Applications/Android Studio.app/Contents/jbr/Contents/Home ./gradlew connectedDebugAndroidTest --no-daemon` after `f723ade` returned `com.android.builder.testing.api.DeviceException: No connected devices!`.
- Next action: infrastructure

## ISS-0910-2-12

- Task: feat-0910-2
- Phase: implementation
- Category: `implementation_regression`
- Priority: P1
- Status: resolved_pending_verification
- Title: Field-row mapping regression reappeared after virtual-point visibility fix
- Impact: The six fields for depth, width, material, damage, hanging/road-crossing pipes, and silt can be treated as one virtual wrapper during reordering; their titles then detach from their controls and unrelated residual blocks can remain visible.
- Evidence: User reported recurrence after the prior `ISS-0910-2-05` correction. In `GutterBasicInfoFragment.directContentRow()`, the branch for `llVirtualHidden1/2/3` returned the wrapper instead of the current direct child row. Multiple calls from `reorderEditableSections()` therefore resolved to the same `llVirtualHidden3` instance.
- Root cause: The helper's ancestor boundary was interpreted as the container to return, rather than the row immediately below that container. This made the ordered view list contain duplicate virtual-wrapper references instead of individual title rows.
- Resolution: Return the current candidate row when its parent is `formContent` or a virtual wrapper. Implemented in commit `64ac477`.
- Validation: `git diff --check`, `assembleDebug`, and `compileDebugAndroidTestKotlin` passed. Connected UI execution was attempted on XQ-AU52 but instrumentation did not reach completion because the Activity was not resumed; final device verification remains pending.
- Next action: verification
