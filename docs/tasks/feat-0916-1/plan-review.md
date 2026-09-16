# Plan Review

## Review Scope
- Task: `feat-0916-1`
- Branch / baseline: `feat/表單合併測距功能` / `ac1eacb`
- Reviewed inputs: `AGENTS.md`, `requirement.md`, `analysis.md`, `plan.md`, `state.yaml`, and `ai/plan-critic-rules.md`.

## Checklist
| Item | Result | Notes |
|---|---|---|
| Requirements and acceptance criteria | Pass | OQ-001 to OQ-003 have recorded product decisions. |
| Repository and architecture analysis | Pass with gaps | Correctly identifies the dialog-window constraint, shared working layer, and `DistanceMeasureManager`; it does not trace listener restoration or layer-preference restoration to a concrete plan. |
| Affected modules and dependencies | Pass with gaps | Main host, both sheets, scope controller, and measuring manager dependencies are identified; the proxy-entry layouts and back-dispatch owner need explicit treatment. |
| Implementation steps actionable | Fail | Three required behaviours lack an implementation owner and ordered transition. |
| Test and regression plan | Fail | It does not cover post-exit main-map click restoration, original/changed `showPlan` state, or Android system-back event consumption. |
| Scope, task size, rollback, privacy | Pass | Scope is bounded, rollback is a standalone commit, and no new data/security concern is introduced. |

## Findings

### Finding 1
Severity: Major
Category: Regression Plan
Description: The plan does not require restoration of the main map click listener after measuring exits. `DistanceMeasureManager.exit()` clears the listener, while `MapWorkspaceFragment` installs `handleMainMapTap` as the normal listener. This creates a direct AC-004/AC-005 regression risk.
Recommendation: Define the listener owner and exit order: let measurement clean its overlays/listeners, then restore the normal map-click listener (and any required camera listener contract) before making the source sheet interactive. Add a focused test that verifies a normal map tap is handled after exit.
Status: Open

### Finding 2
Severity: Major
Category: State Restoration
Description: The list-source flow says scope lines are restored after measuring, but does not preserve the user's existing `showPlan` setting or define what happens if it changes during measuring. `ScopeGutterPolylineController.setVisible()` directly changes visibility, so unconditional restoration would override user state.
Recommendation: Temporarily hide scope lines on list-source entry, then reconcile visibility from the current overlay state on every restoration path. Test both an initially hidden plan layer and a toggle while measuring.
Status: Open

### Finding 3
Severity: Major
Category: Back Navigation
Description: OQ-003 requires Android Back to exit measurement and restore its source sheet, but the plan only names a common exit flow. It does not specify the lifecycle-bound back callback, its enabled window, or event consumption. Existing bottom-sheet cancel/dismiss paths have destructive workflow side effects.
Recommendation: Add a lifecycle-owned callback enabled only during source measurement; it must invoke the single restore path, consume Back, and be disabled after restoration. Cover this with an integration/UI test for each source.
Status: Open

### Finding 4
Severity: Minor
Category: Testability
Description: The plan names generic unit/UI tests but does not identify the proxy-entry layouts or a testable transition boundary for source, sheet visibility, and layer policy.
Recommendation: Name the two bottom-sheet layout changes and introduce/identify a small host-owned state boundary that can be unit-tested without a device; retain device smoke for real Google Maps interaction.
Status: Open

## Round 2 Review

### Resolution of prior findings
- Findings 1–3: Resolved. Steps 5, 6, and 8 now define the normal map-click reinstallation, scope visibility reconciliation from the current `showPlan` preference, and a lifecycle-bound Back callback with an explicit event-consumption rule.
- Finding 4: Resolved. The revised affected-file list identifies both proxy-entry layouts, and the test plan requires a testable host state boundary.

### Finding 5
Severity: Major
Category: Implementation Plan
Description: Step 3 incorrectly treats `hideSelf()` as an existing capability of both bottom sheets. Only `AddGutterBottomSheet` has `hideSelf()`/`showSelf()`; `AddGutterListBottomSheet` has no non-dismiss, reversible hide/show API. The plan therefore lacks the concrete contract needed to preserve the same list fragment and avoid `onCancel()`/close-confirmation side effects.
Recommendation: Amend Step 3 to create an explicit reversible list-sheet API (for example, `hideForMeasure()` and `showAfterMeasure()`), using the same dialog-window invisibility approach as the editor sheet without calling cancel or dismiss. Specify that `MapWorkspaceFragment` retains and restores that same fragment instance, and add a focused test confirming restore does not invoke the close confirmation.
Status: Open

## Round 3 Review

### Resolution of prior finding
- Finding 5: Resolved. The revised plan now names `AddGutterListBottomSheet.hideForMeasure()` and `showAfterMeasure()`, prohibits cancel/dismiss/fragment replacement, retains the same list fragment in the host, and adds focused coverage for state restoration without close side effects.

### Improvement suggestion
Severity: Suggestion
Category: Animation sequencing
Description: The implementation should begin map interaction only after the hide animation has made the source dialog window non-interactive. This is an implementation detail, not a planning blocker.
Recommendation: Invoke the host's measurement-start continuation from the reversible hide API's animation completion callback, or otherwise guarantee the dialog no longer intercepts touch before entering measurement.
Status: Open

## Decision

APPROVED

The plan now covers all acceptance criteria with actionable ownership, state transitions, rollback, and proportionate unit, UI/integration, and device validation. Implementation may begin on the recorded task branch.
