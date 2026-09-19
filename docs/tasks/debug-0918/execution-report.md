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

### Fixed-revision rerun

- Fixed production revision: `a8c95208e46b7cb7142f7e28bf70c309bc9726c8`.
- Validation ran in a clean detached worktree at that revision. The main
  worktree's unrelated `strings.xml` and `.worktrees/` changes were preserved.
- The clean worktree required the existing local Android configuration to resolve
  the map key; it was copied locally for the run and was not committed or
  included in evidence.

| Check | Result | Evidence |
|---|---|---|
| `git diff --check` | PASS | No whitespace errors. |
| Focused unit tests | PASS | `:app:testDebugUnitTest --tests ...` completed successfully for the six debug-0918 targeted suites on the fixed revision. |
| Debug build | PASS | `:app:assembleDebug` completed successfully on the fixed revision. |
| Instrumentation test APK | PASS | `:app:assembleDebugAndroidTest` completed successfully on the fixed revision. |
| Physical UI/API runtime verification | NOT VERIFIED | `adb devices -l` returned no attached device/emulator; import, upload, layer-toggle, and rendered-title cases could not run. |
| CI | NOT VERIFIED | No CI workflow/result is available in the repository. |

## Limitations

- The local Gradle run required Android Studio's bundled JDK and escalated
  access to the Gradle cache outside the workspace.
- The unrelated working-tree change in `strings.xml` and the existing `.worktrees/`
  directory were preserved and are not part of this change.
