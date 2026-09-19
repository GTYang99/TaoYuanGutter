# Verification

## Revision Under Review

- Branch: `fix/debug-0919-1-UI流程`
- Implementation commit: `07e3b5a`
- Current handoff revision: `d204443`
- Package: `com.example.taoyuangutter`
- APK: `app/build/outputs/apk/debug/app-debug.apk`

## Automated Validation

| Check | Result | Evidence |
|---|---|---|
| Targeted unit tests | PASS | `:app:testDebugUnitTest`; Gradle `BUILD SUCCESSFUL`. |
| Debug build | PASS | `:app:assembleDebug`; Gradle `BUILD SUCCESSFUL`. |
| `GutterBasicInfoUiTest` | PASS | `:app:connectedDebugAndroidTest` with runner class filter; `BUILD SUCCESSFUL` on `Medium_Phone` / Android 14. |
| `GutterCantOpenUiTest` | PASS | `:app:connectedDebugAndroidTest` with runner class filter; `BUILD SUCCESSFUL` on `Medium_Phone` / Android 14. |
| `Debug0919ImportedWaypointUiTest` | PASS | `:app:connectedDebugAndroidTest` with runner class filter; `BUILD SUCCESSFUL` on `Medium_Phone` / Android 14. |
| `Debug0919WaypointAdapterUiTest` | PASS | `:app:connectedDebugAndroidTest` with runner class filter; `BUILD SUCCESSFUL` on `Medium_Phone` / Android 14. |
| Committed diff whitespace check | PASS | `git show --check 07e3b5a`. |

## Acceptance Criteria

The four acceptance criteria are UI and flow behaviors. Runtime evidence below was collected on the available Android emulator; incomplete cases remain explicitly marked.

| AC | Result | Evidence / remaining gap |
|---|---|---|
| AC-001 銜接點共同免填規則 | PASS | On `emulator-5554`, selecting `cbConnectPoint` disabled depth photo slots 2/3, depth, cover thickness, top width, broken/hanging/silt groups and connect-pipe group; slot 1 remained enabled. `GutterBasicInfoUiTest` and `GutterCantOpenUiTest` also passed. |
| AC-002 完整上傳條件才顯示已填寫資料 | PASS | `Debug0919WaypointAdapterUiTest` verified blank and partial rows show `暫無資料`; a row with all required values, coordinates and three usable photo slots shows `已填寫資料`. |
| AC-003 匯入後不可編輯定位 | PASS (post-import state fixture) | `_isImported=1` state fixture kept `btnPickLocation` disabled in preview, edit and recreation. The real API fetch itself was not exercised. |
| AC-004 匯入後不可切換虛擬點 | PASS (post-import state fixture) | `_isImported=1` + `is_virtual=1` preserved checked state and kept `cbIsVirtual` disabled in preview, edit and recreation. The real API fetch itself was not exercised. |

## Environment Limitation

Runtime evidence was collected on `emulator-5554`. The scoped debug tests passed. One full-suite run had an unrelated Espresso window-focus timeout in `MainShellActivityTest`; its immediate isolated rerun passed without a code change. It is recorded as environment issue `ISS-debug-0919-1-001`. The real backend fetch was not exercised, but the post-import state boundary is covered by a controlled fixture test.

## Verification Decision

`PASS for scoped acceptance criteria` — all four acceptance criteria have runtime or controlled-fixture evidence. Full-suite stability, CI and release approval remain pending outside this verification result.
