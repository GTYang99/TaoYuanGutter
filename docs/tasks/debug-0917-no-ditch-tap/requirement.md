# Requirement

## Background

- On some devices, users report that selecting a map position is difficult after enabling the `btnReportNoDitch` report mode.

## Goal

- Identify and correct the cause of unreliable main-map point selection in no-ditch reporting mode without changing normal map interaction or report submission behavior.

## Functional Requirements

- Entering no-ditch mode must leave a clear, tappable map area for selecting the first report location.
- The selected location, reset behavior, note entry, and submission must retain their current intended behavior.
- Normal map clicks and no-ditch-point note inspection must continue to work outside reporting mode.

## Non-functional Requirements

- Adapt correctly across affected screen sizes, system insets, and Android versions.

## Acceptance Criteria

- AC-001: On each reproduced affected device profile, a user can select a no-ditch report location with one intentional tap in the unobscured map area after entering report mode.
- AC-002: Reset enables one new location selection, and submission still sends the selected coordinate and note.
- AC-003: Leaving report mode restores normal map clicks and no-ditch-point inspection behavior.

## Constraints

- Requirement source: `/Users/a10362/Desktop/markdown file/ty_refctor_0917.md`.
- Location-speed work and WMS work are separate tasks.

## Open Questions

- Affected device model, Android version, display size, navigation mode, and exact reproduction steps are not yet available.
