# Root Cause Analysis

## Classification

- Task type remains `feature`.
- This document records the planning root cause identified by Plan Critic; it does not reclassify the task as `debug` and does not authorize production-code changes during planning.

## Problem

The plan could mark a draft as `hasSubmittedStoreDitch=true` from a UI Host callback before the actual `GutterRepository.storeDitch(...)` method was entered. That did not precisely implement the requirement that only a request that truly enters the `storeDitch` call is considered submitted.

## Evidence

- `AddGutterBottomSheet` invokes `onGutterSubmitted(...)` immediately before `submitNewGutterRequest(...)` in the add path.
- `AddGutterBottomSheet.performEditSubmit()` invokes `onGutterSubmitting()` before calling `repository.storeDitch(...)` in the edit path.
- The current `GutterRepository.storeDitch(...)` method is the shared owner of the actual API call, but the original plan did not include it as an affected file or define a callback at its method-entry boundary.
- Draft persistence is owned by `GutterDraftCoordinator`／`GutterSessionRepository`; the original plan did not define the behavior when the Host has no current draft id or when the expected Room row is missing.
- `GutterDraftDatabase` uses `exportSchema = false`, and current test dependencies do not provide a Room migration helper; the original plan named a migration test without specifying a reproducible fixture.
- Existing tests cover Gson draft serialization but do not bind `PendingDraftAdapter` or assert the pending-list tag layout.

## Causal Chain

1. The submission marker was assigned to a UI callback rather than the shared repository call boundary.
2. Add and edit flows therefore had two semantically similar but independently ordered paths.
3. A process stop between the Host callback and `GutterRepository.storeDitch` entry could make an unentered request appear submitted.
4. Without a defined missing-draft policy, the marker could also be silently lost or create an untracked state.
5. Without explicit migration and adapter test seams, the plan could not produce deterministic evidence for AC-003 to AC-005.

## Planning Correction

- Define `GutterRepository.storeDitch(...)` method entry as the single shared submission boundary for both add and edit calls.
- Invoke a `suspend onRequestEntered` callback at the beginning of that repository method, before the Retrofit service call; the callback resolves the current draft id and persists the marker.
- For an existing add/resumed draft id, ensure the Room row exists before marking. For direct inspect-edit with no pending draft id, keep the existing API flow and do not create a tagged pending draft; there is no list item to classify.
- Preserve `hasSubmittedStoreDitch=true` in every later auto-save and retry path.
- Add a repository ordering test with a fake API, an explicit Room 3→4 instrumentation migration fixture, and a pending-adapter instrumentation test with exact tag assertions.

## Residual Limitation

No persistence-plus-network operation can make a local database write and a remote HTTP request physically atomic. The corrected contract defines the observable boundary as entry into `GutterRepository.storeDitch`; the marker is persisted synchronously at that method boundary before the Retrofit request is issued, and all remaining outcomes (success, error, timeout, cancellation after entry) retain the submitted state.
