# Root Cause Analysis: ISS-0910-2-09

## Reassessment after verification recurrence (2026-09-11)

### Confirmed facts

- Both the new-form and existing-data connected tests fail with `NoActivityResumedException`.
- In the new-form log, `GutterFormActivity` reaches `RESUMED` at `14:00:54.246` and `PAUSED` at `14:00:54.278`; `GutterBasicInfoFragment.onCreate` starts at `14:00:54.330`, after the pause.
- The log contains no application `FATAL EXCEPTION` or application-triggered `finish()` before the failure. The later `DESTROYED` event occurs during Espresso timeout/cleanup.
- The current implementation no longer inflates the Maps fragment from XML. Map creation is queued from `onPostResume()` after the Activity is resumed.

### Root-cause status

The previous conclusion that synchronous `SupportMapFragment` inflation is the direct cause is not sufficient for this revision: the Activity loses resumed state before the Fragment setup and before the deferred map initialization can run. That conclusion must not be used as the confirmed root cause for the current verification failure.

The confirmed failure class is an Activity top-resumed/lifecycle loss. With the latest batch, all three `GutterBasicInfoUiTest` cases enter `PAUSED` within roughly 15–32 ms after `RESUMED`, before their assertions complete; this is not isolated to a specific field value or assertion. The leading mechanism is now approximately **80% likely**: an interaction between the translucent `FormSheet` window and direct `ActivityScenario` launch on Android 12 causes foreground/top-resumed ownership to be lost immediately after resume. The exact trigger still requires one comparison against the production launch path from `MainShellActivity`; the root cause remains **investigating**, not fully identified.

The field ordering, virtual-point visibility, and upload filtering changes are not implicated by this evidence. The device disconnect during the virtual-point test remains a separate environment issue.

### Confidence boundary

- 80% confidence: the verification failure is caused by the form Activity's foreground lifecycle/top-resumed transition, not by the measured form fields.
- 80% confidence: the translucent `FormSheet` plus direct test launch is the relevant interaction, because the failure repeats across all three form tests and occurs before form assertions.
- Not yet confirmed: whether production launch from `MainShellActivity` reproduces the same transition, or whether the defect is specific to the test launch environment.

### Targeted rerun evidence (2026-09-11 14:27)

- The targeted new-form test was rerun on the same Sony XQ-AU52 / Android 12 device and failed again with `NoActivityResumedException`.
- System `ActivityTaskManager` logged `Activity top resumed state loss timeout` and `Activity pause timeout` at `14:27:19.015–14:27:19.016`.
- `ActivityScenario` reported `RESUMED` at `14:27:18.999`; the app lifecycle then reported `PAUSED` at `14:27:19.059`.
- The task is explicitly reported as `translucent=true`; no second application Activity or app fatal exception appears in the launch window.

This confirms the device's top-resumed timeout mechanism, but does not prove that the translucent window is the cause. An opaque-theme experiment was applied and the targeted test reproduced the same timeout. Confidence that the failure is caused by the `FormSheet` theme alone is therefore **below 50%**; confidence that the failure is in the device/test ActivityTaskManager lifecycle layer is approximately **85%**.

The opaque-theme experiment was reverted because it did not change the failure and would alter the product window behavior without evidence of benefit.

### Final isolation update (2026-09-11)

- The XQ-AU52 device reported `mWakefulness=Asleep` during the failing runs; `always_finish_activities` was `0`.
- After waking the device, the targeted new-form test passed.
- A complete class run exposed the Android Test `InstrumentationActivityInvoker$EmptyActivity` between scenarios; this handoff can pause the next translucent form task. The same run showed the first two form assertions passing and the remaining failure was an outdated `switchPageBar` visibility expectation.
- After correcting that test expectation to match the intentional single-page form, the virtual-point on→off targeted test passed.
- After isolating each `ActivityScenario` launch with `FLAG_ACTIVITY_CLEAR_TASK`, the complete `GutterBasicInfoUiTest` class passed 3/3 and the adjacent `GutterCantOpenUiTest` passed 4/4 on XQ-AU52.

The current evidence supports an environment/test-harness classification for `ISS-0910-2-09`, not a production Activity crash. No production workaround was retained; the mitigation is test-task isolation.

## Earlier investigation evidence (superseded for the current revision)

- The failure occurs in `GutterBasicInfoUiTest.newFormShowsRequiredOrderLabelsButtonsAndDefaults` on Sony XQ-AU52.
- `GutterFormActivity` reaches `RESUMED`, then transitions to `PAUSED` and `STOPPED` about 66 ms later.
- There is no application `FATAL EXCEPTION`, `finish()` log, or stack trace identifying an application crash.
- Espresso waits for the first assertion until the 46-second timeout, after which `ActivityScenario` reports the activity as `DESTROYED` and raises `NoActivityResumedException`.
- The same test uses an activity with a full-screen `SupportMapFragment` and a translucent `FormSheet` theme. Existing-value and `GutterCantOpenUiTest` scenarios do not reproduce this exact failure.

## Earlier root-cause conclusion (superseded)

An earlier revision was assessed as exceeding the Sony XQ-AU52 top-resumed deadline while synchronously inflating the form and XML-declared `SupportMapFragment`. That conclusion applied to the earlier implementation and is retained only as historical evidence; it is not the confirmed cause of the current recurrence.

This is an application startup performance/lifecycle regression, not an app `finish()` call or the `onPause()` draft-sync path. The translucent form theme increases the consequence because the form is expected to remain a foreground overlay, but changing that theme is not required by the root-cause evidence.

## Latest isolation result (2026-09-11)

- The same targeted test was executed twice on the same `XQ-AU52 - 12` device and the same committed revision `4aa4d06`.
- Run 1 passed: `1 test, 0 failures`, completed in about 5 seconds.
- Run 2 failed: `1 test, 1 failure`, `NoActivityResumedException` after 45.5 seconds.
- The failing run again reached `RESUMED`, then `PAUSED/STOPPED` about 63 ms later; `DESTROYED` occurred during failure cleanup.
- Both runs show `SupportMapFragment.onCreateView()` blocking the main thread for about 203–226 ms. The pass run remained resumed long enough for the assertions; the fail run did not.
- No app `FATAL EXCEPTION`, explicit `finish()`, or logged second application Activity was captured.

The earlier pass/fail variation motivated deferring map creation. That change is already present in the current revision, but the latest failure still pauses before Fragment setup, so it does not close the current issue.

## Affected acceptance criteria

- AC-001, AC-002, AC-003, AC-005: assertions cannot run because the new-form activity is not resumed.
- AC-006: the connected regression suite fails even though the affected `GutterCantOpenUiTest` cases pass.

## Investigation evidence

- `app/build/outputs/androidTest-results/connected/debug/TEST-XQ-AU52 - 12.xml`: one failure in a 12-test connected run.
- `app/build/outputs/androidTest-results/connected/debug/XQ-AU52 - 12/logcat-com.example.taoyuangutter.GutterBasicInfoUiTest-newFormShowsRequiredOrderLabelsButtonsAndDefaults.txt`: lifecycle sequence and absence of app fatal exception.
- Earlier `GutterFormActivity.onCreate()` implementation: initialized the map fragment and form pager before the first assertion. The current implementation queues map initialization from `onPostResume()` instead.

# Root Cause Analysis: ISS-0910-2-12

## Confirmed behavior

- The previously corrected six-field ordering defect reappeared after the virtual-point visibility work.
- The affected fields are depth, width, material, damage, hanging/road-crossing pipes, and silt; their titles can be displaced or remain visible outside the intended virtual-point form.
- The report is distinct from the empty gray switch-bar issue and from the original `ISS-0910-2-05` ordering defect.

## Root cause

`reorderEditableSections()` must move individual title rows into the approved order. Its `directContentRow()` helper walks upward from a title, but the virtual-section boundary was handled incorrectly: when the current row's parent was `llVirtualHidden1`, `llVirtualHidden2`, or `llVirtualHidden3`, the helper returned the wrapper rather than the current row. Since the six affected titles originally share `llVirtualHidden3`, each lookup resolved to the same wrapper. The ordered list consequently contained duplicate wrapper references and could not preserve title/control adjacency.

## Resolution and review evidence

- Corrected `directContentRow()` to return the candidate row when its parent is `formContent` or a virtual wrapper.
- Added the recurrence to `issue-log.md` as `ISS-0910-2-12` with the affected code path, impact, resolution commit, and validation limitation.
- Resolution commit: `64ac477`.
- Build and static checks passed; connected UI verification remains `NOT VERIFIED` because the instrumentation run did not complete on XQ-AU52.


# Root Cause Analysis: ISS-0910-2-11

## Confirmed behavior

- The original virtual-point flow allowed the user to turn virtual mode off and restore the normal form navigation behavior.
- The current form still exposes `cbIsVirtual` and its listener, but turning it off does not restore the original page-navigation UI/interaction state.

## Root cause

This is a regression from the UI layout redesign in commit `cb3b90f` (`fix: UI 版面改版`). The original `applyVirtualModeUi(isVirtual)` used the virtual-state argument to control both behaviors:

- `switchPageBar.visibility = if (isVirtual) View.GONE else View.VISIBLE`
- `viewPager.isUserInputEnabled = !isVirtual`

The redesign replaced both state-dependent expressions with unconditional values:

- `binding.switchPageBar.visibility = View.GONE`
- `binding.viewPager.isUserInputEnabled = false`

As a result, the checkbox can change `isVirtualMode` and the form fields can be restored by `GutterBasicInfoFragment.setVirtualMode(false)`, but the Activity-level navigation state remains permanently disabled/hidden. The close/off action therefore appears to be missing even though the checkbox listener still exists.

## Affected files and risk

- `app/src/main/java/com/example/taoyuangutter/gutter/GutterFormActivity.kt`: `applyVirtualModeUi()` and its initial `setupTabButtons()` interaction.
- `app/src/main/java/com/example/taoyuangutter/gutter/GutterBasicInfoFragment.kt`: existing field visibility toggle remains present and is not the root cause.
- Risk: restoring the original conditional behavior may expose the page switch bar in non-virtual mode, so the implementation must preserve the current single-page product intent if that visibility was intentionally removed; the required behavior decision is specifically that virtual mode can be turned off and normal form interaction restored.

## Additional field-visibility finding

The virtual-mode field filter also omitted `cbCantOpen`. Because the measurement-status row contains both `btnPendingDeploy` and `cbCantOpen`, virtual mode still displayed the inapplicable 「無法開蓋」 option. The expected virtual form keeps the measurement-status title and 「待架站」 only, plus the measurement-coordinate number field and location section.

## Minimum fix scope

Restore the virtual-state transition contract at the Activity layer: when virtual mode is turned off, re-enable the normal form navigation/close path and keep virtual mode's hidden/disabled state while it is on. Hide `cbCantOpen` while virtual mode is on. Add a regression test that toggles `cbIsVirtual` on and off and verifies both the checkbox state and the restored controls. Do not change API keys, field order, photo slots, or map startup behavior.
