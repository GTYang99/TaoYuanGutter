# Fix Plan

## Issue

ISS-DBG-1021-2-001 — Closing the unfinished multi-gutter flow leaves the main map buttons hidden or disabled.

## Approved Behavior

User decision: after a swipe dismisses the list, Confirm closes the add-list session and saves its drafts; Cancel reopens the add-list sheet with its existing drafts so the user can continue. When the close icon opens the same prompt without dismissing the sheet, Cancel simply leaves that sheet open.

## Minimum Fix Scope

- Capture the `Host` before the list sheet is removed by bottom-sheet cancellation; use that captured reference for the later alert selection.
- On Confirm, keep the existing host cleanup path: finalize drafts, clear the active multi-gutter session, and restore the map UI.
- On Cancel after a swipe dismissal, reopen the list from the still-active multi-gutter coordinator without finalizing or clearing its drafts.
- Keep close-icon Cancel behavior as-is: dismiss only the alert and leave the existing list sheet visible.
- Keep changes limited to the list-sheet close lifecycle and its host cleanup. Do not change form validation, upload ordering, API behavior, or draft serialization.

## Validation

- Focused emulator test for Cancel after BottomSheet cancellation: prompt closes, list is re-opened, and its draft contents remain equal.
- Focused emulator test for Confirm after BottomSheet cancellation: fragment is detached before selection, but the host close callback is delivered.
- Existing close-icon test covers Cancel and Confirm while the list remains attached.
- Full physical swipe through `GutterFormActivity` and the production map screen remains a separate verification item if unavailable in the time-box.
