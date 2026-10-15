# Root Cause — ISS-feat-1015-001

## Symptom

The focused emulator UI test failed after tapping the Nearby tab because `btnMyLocation` remained invisible.

## Evidence

- The failure was reproduced on the single permitted retry on `emulator-5554`; the report identifies `btnMyLocation` as `INVISIBLE` at the test assertion.
- The test's `onWaypointSelected` callback appends every value to `CopyOnWriteArrayList<NodeDetails?>`.
- `ImportExistingWaypointBottomSheet.renderPage()` calls `clearSelection()` before applying the selected page's visibility state. `clearSelection()` correctly invokes `onWaypointSelected(null)` when a tab is switched.
- `CopyOnWriteArrayList` does not accept null values. The test callback therefore throws during the tab-switch event, preventing the rest of `renderPage()` from showing the Nearby controls.

## Classification

This is an implementation defect in the new instrumentation-test callback collector. No production-code defect is evidenced. The first test attempt also reported a FragmentManager saved-state exception while showing the sheet; that failure did not repeat on the one retry, whose failure produced the deterministic Nearby assertion above.

## Scope and Risk

Only `ImportExistingWaypointUiTest.kt` needs a correction. Record null-selection events with a null-safe mechanism while preserving assertions for selected item, clear-selection callback, candidate updates, and import handoff. No production behavior change is needed.

## Follow-up — ISS-feat-1015-002

After the null-safe collector fix, test runs failed to switch tabs when clicking nested label text with `withText`: one report showed Nearby still selected after the Search label click, and a later run left the location button invisible after the Nearby label click. The test should target the whole clickable `TabView` by its accessibility description and assert selection after each switch. This is an Espresso target-selection issue; no production failure is shown.

## Follow-up — ISS-feat-1015-003

Two focused emulator attempts failed before sheet assertions at the test's `DialogFragment.show()` call with `IllegalStateException: Can not perform this action after onSaveInstanceState`. The intervening attempt reached Nearby and Search UI assertions, so the failure is intermittent and occurs at ActivityScenario setup. The test currently assumes its activity's FragmentManager remains unsaved after launch. Explicitly move the scenario to `RESUMED` and check `isStateSaved` before showing the sheet. This remains a test-harness issue; no production failure is evidenced.
