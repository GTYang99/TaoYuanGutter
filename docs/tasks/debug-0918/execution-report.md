# Implementation and Validation Report

## Implementation

- Changed the initial `0910刪除資料` overlay state to disabled in the map
  controller, layer bottom sheet defaults, and layer checkbox XML.
- Changed the existing-waypoint import header to a `FrameLayout` so the title
  is centered against the full header row while the back and optional location
  controls retain their 48dp hit areas.
- Preserved the `storeDitch` response contract: `data.nodes[].url[].id` is
  mapped to the corresponding `photo{slot}ImgId` by `fileCategory`.
- Added an import handoff fallback: a returned detail-response ID takes
  precedence, while a missing detail-response ID preserves the existing
  `photo{slot}ImgId` instead of clearing it. Downloaded unchanged photos are
  still not re-uploaded; replacement photos still upload and carry the
  returned ID through the existing-node `storeDitch` update.
- Updated `NodeDetails` photo parsing to accept both `node_img[]` and the
  `url[]` response shape shown by the supplied `storeDitch` payload. Import,
  edit preload, and inspect preload now use the same ID-aware resolver.
- Preserved `url[].id` when `NodeDetails` is converted into main-map inspect
  waypoints where an ID is actually present, while keeping URL-only imported
  photos in the unchanged `success` state so they are not re-uploaded. Photo
  replacement clears the state and remains eligible for `nodeImage`.

## Validation

| Check | Result | Evidence |
|---|---|---|
| `git diff --check` | PASS | No whitespace errors. |
| Focused unit tests | PASS | Targeted photo-ID/import tests passed, including `NodeImgDeserializationTest`, `PhotoImgIdResolverTest`, and upload-candidate tests for ID-present vs ID-missing slots. |
| Full unit test suite | PASS | `:app:testDebugUnitTest` completed successfully. |
| Debug build | PASS | `:app:assembleDebug` completed successfully. |
| Instrumentation test APK | PASS | `:app:assembleDebugAndroidTest` completed successfully; test runtime was not executed because no device was connected. |
| Physical UI verification | NOT VERIFIED | No device screenshot/manual run was performed in this pass. |
| CI | NOT VERIFIED | No CI workflow/result is available in the repository. |

## Limitations

- The local Gradle run required Android Studio's bundled JDK and escalated
  access to the Gradle cache outside the workspace.
- The unrelated working-tree change in `GutterApiService.kt` and the existing
  `.worktrees/` directory were preserved and are not part of this change.
