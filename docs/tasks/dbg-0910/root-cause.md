# Root Cause Investigation

## Status

The supplied XY_NUM response has now confirmed `node_img` with a category-1 URL, so the earlier `url`/`node_img` mapping mismatch hypothesis is disproved for this case. Reproduction evidence further shows that normal cases display imported photos, while `IS_CANTOPEN=1` does not display the photo on first entry and does display it after navigating away and back. The remaining failure point is therefore concentrated in the cant-open initialization/lifecycle render timing or a cant-open-specific state synchronization overwrite, with URL download/local URI usability still requiring direct runtime proof.

## Symptom Mapping

| Issue | Current evidence | Working hypothesis | Required proof |
|---|---|---|---|
| Review errors expose raw field keys | `validateWaypointPhotosAndFieldsOrAlert()` builds `missingFields` directly from `MAT_TYP`, `IS_BROKEN`, `IS_HANGING`, `IS_SILT`. | Presentation layer has no key-to-Chinese label mapping. | Test each missing field and capture exact dialog text. |
| Bottom sheet too low | `setupBottomSheetBehavior()` sets height and peek height to `displayMetrics.heightPixels / 2`. | Fixed half-screen sizing is the direct cause; six-tenths is the intended correction. | Device/emulator measurement plus viewport and gesture regression. |
| Cant-open import photo missing / confirm disabled until reopen | `handleImportedNodeDetails()` performs field prefill synchronously, downloads photo asynchronously, then calls `prefillPhotos()` and Activity state updates; `GutterBasicInfoFragment.onViewCreated()` performs its initial `prefillData()`/render before that asynchronous completion. Normal imports work; only cant-open first entry fails, and reopening displays the photo. The cant-open helper currently clears slots 2 and 3 only. | Leading root-cause direction: the imported slot-1 URI is delivered after the initial render, but the cant-open initialization or a queued draft synchronization can leave the first view stale; recreating the Fragment on return invokes the render path with persisted state. | Capture first-open values at download completion, `prefillPhotos`, `renderStoredPhotoSlots`, `updateCurrentFormPhotos`, `syncPersistedPhotoState`, and confirmation validation; compare reopen values. |

## Supplied API Evidence: Mapping Mismatch Disproved for A0910pt52

- `NodeDetails` declares photos only as `@SerializedName("node_img") val nodeImg: List<NodeImg>` in `GutterApiModels.kt:366–367`.
- `GutterFormActivity.handleImportedNodeDetails()` reads only `nodeDetails.nodeImg` at `GutterFormActivity.kt:573–575` and `622–633`.
- `DitchNode` declares the photo list as `@SerializedName("url") val url: List<NodeImageUrl>`; `GutterInspectActivity` explicitly falls back from `node.url` to `nodeDetails.nodeImg` at `GutterInspectActivity.kt:417–419` and `472–480`.
- Existing API examples in `docs/tasks/feat-0905/requirement.md` show node photo arrays under `"url": [...]`, not `"node_img"`.

### Confirmed Runtime Evidence

The connected emulator log for the first-open flow provides the following state transition:

- `23:47:40.730` — `GutterBasicInfoFragment.syncPersistedPhotoState`: `p1(photo=false)`.
- `23:47:40.748` — `GutterFormActivity.updateCurrentFormPhotos.beforeSync`: `p1(photo=true)`.
- `23:47:40.750` — `GutterBasicInfoFragment.updatePhotoUploadStatus.slot1`: `p1(photo=false)`.
- `23:48:58.434` after leaving/re-entering — `GutterBasicInfoFragment.syncPersistedPhotoState`: `p1(photo=true)`.

The same process therefore holds the imported photo in Activity/session state while the Fragment visible on first entry still holds an empty slot. Re-entry repairs the visual state by calling the persisted-state/render path. This is direct evidence of a first-open Activity-to-Fragment synchronization gap, not an image-download failure.

### Root Cause

The supplied response for `XY_NUM=A0910pt52` contains `IS_CANTOPEN="1"`, one `node_img` item, `fileCategory="1"`, and a valid HTTPS URL. This matches the current `NodeDetails.nodeImg` model, so the `url`/`node_img` mapping mismatch is not the cause for this reported case. The response has no image `id`, but the current display and `validateAllPhotos()` paths require a usable photo URI, not an image ID, so missing `id` alone does not explain the missing display.

The confirmed defect is that the asynchronous import path updates `currentFormData` in `GutterFormActivity`, but the active `GutterBasicInfoFragment` does not retain/render the same slot-1 URI during the first-open lifecycle. The subsequent upload-state callback also observes an empty Fragment slot. Re-entering the page invokes `syncPersistedPhotoState()` and `renderStoredPhotoSlots()`, which is why the photo appears only after navigation.

### Confidence

High that the response-shape mismatch and download failure are not the cause for `A0910pt52`; high that the remaining defect is a cant-open first-open Activity-to-Fragment state synchronization gap.

## Affected Files

- `app/src/main/java/com/example/taoyuangutter/gutter/AddGutterBottomSheet.kt`
- `app/src/main/java/com/example/taoyuangutter/gutter/GutterFormActivity.kt`
- `app/src/main/java/com/example/taoyuangutter/gutter/GutterBasicInfoFragment.kt`
- `app/src/main/java/com/example/taoyuangutter/gutter/GutterFormContract.kt` only if evidence shows contract propagation is incomplete; do not change keys by assumption.
- Relevant Android UI/unit tests under `app/src/androidTest` and `app/src/test`.

## Investigation Steps

1. Add or use temporary diagnostic evidence at the import boundary to compare API `fileCategory=1`, local download URI, Fragment slot 1, Activity `currentFormData[photo1]`, and validation result. **Completed:** runtime logs show Activity `p1=true` while Fragment remains `p1=false` on first open.
2. Reproduce cant-open import on a clean form and after reopening, recording lifecycle order and whether the first confirmation attempt happens before download completion.
3. Deserialize `evidence/node_details_A0910pt52.json` and confirm `nodeImg[0].fileCategory == "1"`, then compare that value with the runtime `nodeDetails` object at import.
4. Inspect all callers of `prefillPhotos()`, `renderStoredPhotoSlots()`, `updateCurrentFormPhotos()`, `validateAllPhotos()`, and the submit/confirm enabled-state path for stale or overwritten state. **Completed:** the first-open divergence is confirmed; the minimum fix must make the persisted Activity state the source for the active Fragment render.
5. Confirm whether slot 1 is cleared or only fails to render on first entry; `clearMeasurementPhotosForCantOpen()` currently clears slots 2 and 3 only.
5. Verify the required-key mapping against current XML labels and ensure only display text changes.
6. Verify sheet measured height and map viewport inset before selecting the minimum sizing fix.

## Failed / At-Risk Acceptance Criteria

- AC-001: current raw-key display is inconsistent with requested Chinese labels.
- AC-002: current implementation is 50%, not approximately 60%.
- AC-003: `A0910pt52` payload contains the expected category-1 image; remaining root cause is not yet confirmed and must be isolated across download, URI validation, rendering, and state synchronization.
- AC-004: regression risk exists across normal import, slot metadata, cant-open validation, and reopen behavior.

## Regression Risk

- Mapping labels could accidentally change request keys or validation rules.
- Sheet sizing could clip the RecyclerView or alter map touch routing and viewport inset.
- Import repair could overwrite existing slot metadata, trigger duplicate downloads, or enable confirmation before slot 1 is usable.
- Changes may overlap the unresolved `feat-0910-1` source/document mismatch; implementation must stop if the approved behavior cannot be identified.
