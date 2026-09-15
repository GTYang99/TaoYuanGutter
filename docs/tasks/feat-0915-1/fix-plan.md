# Debug Fix Plan

Task: feat-0915-1

## Scope

- Resolve ISS-0915-002 / AC-002 by changing duplicate preset handling to a
  no-op.
- Resolve ISS-0915-004 by scrolling to the remarks section in the two chip UI
  tests before interaction.

## Changes

1. In `GutterBasicInfoFragment.setupRemarkPresetChips()`, return without
   changing `etRemarks` when the exact preset already exists as a comma-separated
   item.
2. Keep first-time append behavior, Chinese comma separation, and manual text
   unchanged.
3. Add a scroll action targeting `tilRemarks` or the remarks chip group before
   each chip test's visibility and click assertions.
4. Run `git diff --check`, compile, and the targeted connected UI test.

## Acceptance mapping

| Fix | Acceptance criterion | Evidence |
|---|---|---|
| Duplicate click no-op | AC-002 | UI test expects unchanged note after second tap |
| Scroll before interaction | AC-001, AC-002 | Emulator test can observe and click chips |

## Constraints

- Do not change API fields, draft schema, `NODE_NOTE`, or unrelated form modes.
- Do not begin re-implementation until this debug record is approved by the
  workflow state.
