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

## ISS-feat-1015-006 — Shared emulator was running another instrumentation suite

- Task: `feat-1015`
- Phase found: `infrastructure`
- Category: `environment`
- Priority: `P2`
- Status: `resolved`
- Impact: The follow-up emulator UI run lost its resumed activity before completing assertions, so that run cannot validate the import sheet.
- Evidence: The targeted `:app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.taoyuangutter.ImportExistingWaypointUiTest` run on `emulator-5554` failed with `NoActivityResumedException` at the row-selection assertion. The same emulator log shows `com.example.tp_ncolso_android.test/...EmptyActivity` displayed at `10-15 10:28:04.133` and its instrumentation test executing concurrently.
- Route: `infrastructure`
- Environment check: `emulator -list-avds` reports only `Medium_Phone`. Starting a second read-only instance on port 5556 failed because the existing instance is running without read-only mode (`Another emulator instance is running`).
- Remediation: Provisioned task-specific AVDs `Feat1015_Test` and `Feat1015_Test_API36`. The latter is available on `emulator-5558` (Pixel 7 profile, Android 16/API 36). The original shared-emulator failure is no longer the constraint. The physical XQ-AU52 was not used, per the user's instruction.
- Resolution evidence: `ImportWaypointMapMarkerTest` passed on `Feat1015_Test_API36`. The UI test was attempted on the isolated AVDs; its remaining focus failure is tracked separately as ISS-feat-1015-007.
- Next action: verification

## ISS-feat-1015-007 — Headless AVD instrumentation window does not gain focus

- Task: `feat-1015`
- Phase found: `infrastructure`
- Category: `environment`
- Priority: `P2`
- Status: `open`
- Impact: The focused import-page UI test cannot reach its first label assertion on the new isolated AVDs because Espresso cannot obtain a focused application window. This prevents fresh emulator evidence for the integrated page-switch UI flow.
- Evidence: On `Feat1015_Test` (emulator-5556, Android 16/API 36, `google_apis_playstore_ps16k`), `ImportExistingWaypointUiTest.tabsKeepOrderDefaultAndRefreshEachSubmittedModeOnReturn` failed at line 146 with `RootViewWithoutFocusException`; a captured screen showed the Pixel Launcher ANR dialog. Per the one-retry limit, it was retried on a second isolated AVD, `Feat1015_Test_API36` (emulator-5558, Android 16/API 36, `google_apis_playstore`), and failed at the same initial assertion with the same exception. At failure, the launcher remained focused; no unrelated instrumentation suite was running on either dedicated AVD. `ImportWaypointMapMarkerTest` passed on emulator-5558.
- Route: `infrastructure`
- Remediation attempted: Created and booted a second isolated AVD using the other installed Android 16 Google Play image, stopped the first task-created AVD to reduce host load, and waited for the replacement launcher to settle before the one permitted retry. No application or test source was changed.
- Remaining limitation: Both test invocations used `-no-window`; Espresso still did not receive window focus on the retry. Do not retry this test again in this evidence cycle. AC-001/AC-004 retain their earlier passing evidence; fresh UI evidence remains unavailable. A future visible-window AVD run may provide evidence.
- Next action: verification

## ISS-feat-1015-008 — Emulator UI assertions fail inconsistently during tab/search flow

- Task: `feat-1015`
- Phase found: `verification`
- Category: `unknown`
- Priority: `P2`
- Status: `open`
- Impact: The independent UI test does not reliably verify the submitted XY_NUM value or Nearby tab entry on the combined revision.
- Evidence: On fixed revision `f5e31eff808c9aa3d04df23927bd660978d93931`, the first targeted `ImportExistingWaypointUiTest` attempt on `Medium_Phone` / Android 16 failed at line 188 with expected `[A001]`, actual `[A0]`. The single permitted retry on the same emulator failed earlier at line 165 with expected selected tab index 1, actual 0. The test was not run again in this evidence cycle.
- Route: `investigation`
- Next action: Determine whether the mismatch comes from application behavior or Espresso/IME/tab interaction timing. Do not patch production or tests until the cause is classified.
