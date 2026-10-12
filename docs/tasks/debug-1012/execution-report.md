# Debug Execution Report

## Reviewed Revision

- Source revision: ac6c9dd
- Branch: codex/debug-1012
- Worktree started clean before task artifacts were added.

## Static Review

- Traced AddGutterBottomSheet edit and create submission error flows.
- Traced storeDitch HTTP error conversion and UploadFailureClassifier routing for non-network status codes.
- Traced MainActivity, MapWorkspaceFragment, and MainBlockingUiController loading callbacks and animation lifecycle.
- Used source line inspection and git blame to compare the loading activation and failure cleanup paths.

Result: static source review identifies a stale loading state in the existing-gutter update failure path after the save-draft choice. The same missing cleanup is present when a generic exception takes the same decision-dialog path.

## Validation

- Automated tests: NOT RUN; implementation changes were not requested in this task turn.
- Build/CI: NOT RUN; no implementation was changed and no task-specific CI result was available.
- Physical device reproduction: NOT VERIFIED; no device/session trace was provided.
- Draft persistence after dismissal: NOT VERIFIED for each host/session mode.

## Blockers

- No task-specific requirement.md, plan.md, or verification.md was present, so formal failed-AC mapping is pending.
- No request/response log or device evidence was available to confirm whether the reported 403 came from storeDitch rather than a photo endpoint.


## Extended Cross-Path Review

- Confirmed MainShellActivity defaults to MapWorkspaceFragment and single-gutter mode.
- Confirmed the single-gutter draft-confirm callback returns false; the multi-gutter callback clears loading and persists before returning true.
- Confirmed host dismissal recovery does not clear inspect loading and exits while the controller reports a blocking state.
- Confirmed no matching callback/loading lifecycle tests are present under app/src/test.
- Inspected the draft coordinator: without a current draft ID or a matching existing draft for the SPI_NUM, autosave returns without saving. The save-draft dialog callback itself does not call persistence in the single-gutter host.
- Attempted read-only device discovery with adb devices -l. ADB daemon startup failed with Operation not permitted, so there is no device evidence.

Confidence in the source-level spinner finding: 97%. This does not claim a device reproduction or prove the save-draft item is persisted in every session state.
