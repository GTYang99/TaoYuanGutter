# Requirement

## Background

- Users report that application location updates are slower than Google Maps.

## Goal

- Improve location responsiveness on every current map screen by immediately centering on an available recent location, then correcting to a newer high-accuracy location when it arrives.

## Functional Requirements

- Apply the fast-first-location then high-accuracy-correction behavior to the main workspace map, standalone map point picker, and form/import location flows.
- Preserve current permission, unavailable-location, and timeout handling.
- Do not start indefinite background location tracking solely for this interaction.

## Non-functional Requirements

- Avoid unnecessary battery use and preserve user location privacy.

## Acceptance Criteria

- AC-001: With a usable recent device location, each applicable map screen visibly centers without waiting for a new GPS fix.
- AC-002: When a newer high-accuracy location arrives, the screen updates its location state and corrects the map position only when the new result is meaningfully better.
- AC-003: Permission denial, unavailable recent location, and high-accuracy timeout retain a clear existing-equivalent user outcome and do not leave an active callback.

## Constraints

- Requirement source: `/Users/a10362/Desktop/markdown file/ty_refctor_0917.md` plus user confirmation on 2026-09-17.
- WMS work is tracked separately in `refactor-0917-wms`.

## Open Questions

- None.
