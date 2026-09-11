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
- Title: New-form activity is destroyed before UI assertions
- Impact: The new-form UI test cannot verify the measurement-status/default/order/button requirements; the connected suite fails 1 of 12 tests.
- Evidence: `TEST-XQ-AU52 - 12.xml` and the targeted rerun report; `GutterBasicInfoUiTest.newFormShowsRequiredOrderLabelsButtonsAndDefaults` fails twice with `NoActivityResumedException` at line 23. Logcat shows `GutterFormActivity` transitions to `DESTROYED` before the assertion completes, without a matching app `FATAL EXCEPTION`.
- Repro steps: Run `JAVA_HOME=/Applications/Android Studio.app/Contents/jbr/Contents/Home ./gradlew connectedDebugAndroidTest --no-daemon -Pandroid.testInstrumentationRunnerArguments.class=com.example.taoyuangutter.GutterBasicInfoUiTest#newFormShowsRequiredOrderLabelsButtonsAndDefaults` on `XQ-AU52 - 12`.
- Expected: New-form activity remains resumed long enough for the UI assertions to complete.
- Actual: Activity is destroyed and Espresso raises `NoActivityResumedException`.
- Root-cause status: identified. The Activity startup path exceeds the device top-resumed deadline while synchronously inflating the form and `SupportMapFragment`; system logs show `Activity top resumed state loss timeout` followed by `Activity pause timeout`.
- Follow-up evidence (2026-09-11): the same targeted test passed once and failed once on the same XQ-AU52 device/revision. Both failure logs correlate `SupportMapFragment.onCreateView()` main-thread blocking (203–226 ms) with startup. System logs now confirm the Activity top-resumed and pause timeouts; the earlier focus-race classification is superseded by this startup-timeout root cause.
- Root-cause evidence: system `ActivityTaskManager` logs at 10:06:46.536, 10:24:22.408, 10:40:53.778, and 10:46:17.636 report top-resumed and pause timeouts for `GutterFormActivity`; the 10:46:17.104 launch reaches `RESUMED` only after the timeout window. No app fatal exception or explicit `finish()` is present.
- Resolution status: implementation completed; targeted post-fix new-form test passed once. Class-level regression was blocked by the device going offline during reinstall.
- Next action: verification
