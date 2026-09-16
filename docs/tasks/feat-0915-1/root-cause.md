# Root Cause Analysis

Task: feat-0915-1
Verification revision: `1acb329c237cec245cc6b68c86b55db989d3ec97`

## Classification

- Primary category: `implementation`
- Failed acceptance criterion: AC-002
- Related issues: ISS-0915-002, ISS-0915-004

## Root Cause 1: Duplicate tap removes the preset

The implementation interpreted “click again” as a toggle. In
`setupRemarkPresetChips()`, an existing exact comma-separated item is handled by
`filterNot { it == preset }`, so the second tap removes the preset from
`etRemarks`. The approved plan and verification rule require a duplicate tap to
leave the original remark unchanged; only the first tap may append the preset.

This is an implementation regression, not a requirement ambiguity. It can cause
user-entered content to be lost and directly fails AC-002.

## Root Cause 2: UI tests operate outside the viewport

The two new chip tests assert or click the remark chips immediately after
launching the form. The remarks section is below the initial emulator viewport,
so Espresso reports an empty global visible rectangle. The tests do not scroll
the `NestedScrollView` to the remarks section before interacting.

This prevents runtime evidence for AC-001 and AC-002 but does not indicate that
the chip views are absent.

## Affected files

- `app/src/main/java/com/example/taoyuangutter/gutter/GutterBasicInfoFragment.kt`
- `app/src/androidTest/java/com/example/taoyuangutter/GutterBasicInfoUiTest.kt`
- `docs/tasks/feat-0915-1/verification.md`
- `docs/tasks/feat-0915-1/issue-log.md`

## Minimum remediation

1. Make duplicate preset clicks a no-op, preserving the complete existing note.
2. Scroll the test `NestedScrollView` to the remarks area before chip assertions
   and clicks.
3. Rerun the targeted emulator tests and update verification evidence.
