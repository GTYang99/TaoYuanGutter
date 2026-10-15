# Issue Log

## ISS-feat-1015-001 — Nullable selection callback breaks instrumentation page switch

- Task: `feat-1015`
- Phase found: `verification`
- Category: `implementation_regression` (instrumentation test harness only)
- Priority: `P2`
- Status: `resolved`
- Impact: The focused emulator test cannot complete Nearby-tab assertions after exercising selection callbacks. No production behavior failure has been demonstrated.
- Evidence: `:app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.taoyuangutter.ImportExistingWaypointUiTest` failed on `emulator-5554`. The retry failed at the Nearby location-button visibility assertion. `renderPage()` clears selection before switching page controls, and the test callback appends nullable selections to `CopyOnWriteArrayList`, which rejects `null`.
- Route: `debug` → `implementation_debug`
- Resolution: Replaced the nullable collection with `AtomicReference<NodeDetails?>` plus a null-clear counter. The final focused emulator test passed with selection and clear assertions.
- Next action: verification

## ISS-feat-1015-002 — Search tab tap matcher does not select tab in populated Nearby state

- Task: `feat-1015`
- Phase found: `implementation`
- Category: `implementation_regression` (instrumentation interaction only)
- Priority: `P2`
- Status: `resolved`
- Impact: The extended emulator test cannot reliably switch tabs by clicking the nested tab label after a row is populated.
- Evidence: One corrected run failed at the Search prompt and the hierarchy showed Nearby still selected after `withText("搜尋點位").perform(click())`; the later run failed after `withText("附近點位").perform(click())` because the location button remained invisible. Both tab labels are descendants of a clickable `TabView`.
- Route: `debug` → `implementation_debug`
- Resolution: The test now targets each `TabView` by content description and asserts the selected tab position after each switch. The final focused emulator test passed.
- Next action: verification

## ISS-feat-1015-003 — ActivityScenario occasionally saves FragmentManager before sheet setup

- Task: `feat-1015`
- Phase found: `implementation`
- Category: `implementation_regression` (instrumentation setup only)
- Priority: `P2`
- Status: `resolved`
- Impact: The focused emulator test can fail before exercising the sheet when `DialogFragment.show()` tries to commit after the ActivityScenario activity has saved state.
- Evidence: The first and latest focused emulator runs failed at the test setup call to `sheet.show(...)` with `IllegalStateException: Can not perform this action after onSaveInstanceState`; the intervening run reached UI assertions.
- Route: `debug` → `implementation_debug`
- Resolution: The test now moves `ActivityScenario` to `RESUMED` and asserts the activity and FragmentManager state before showing the sheet. The final focused emulator test passed.
- Next action: verification
