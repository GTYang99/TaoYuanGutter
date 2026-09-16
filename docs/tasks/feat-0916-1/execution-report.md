# Execution Report

## Implementation
- Removed the former measurement proxy buttons from the list and editor sheets; the existing main-map button is now the only entry point.
- Added reversible list-sheet hide/show behavior without dismissing the fragment.
- Added source-aware measurement state in `MapWorkspaceFragment`.
- List-source measurement preserves the working layer so existing segments and nodes remain visible; exit reconciles scope visibility from the current `showPlan` preference.
- List opening now hides all rendered scope polylines immediately; closing the list restores the normal main-map control policy.
- Editor-source measurement preserves the working layer and restores the same editor sheet.
- Added a view-lifecycle Android Back callback and reinstalls the normal map-click listener after measurement exits.
- Measurement starts only after the source sheet hide animation completes, so the dialog no longer intercepts map input.

## Developer Validation
- `git diff --check`: PASS.
- XML well-formedness check with `xmllint --noout` on both modified layouts: PASS.
- `JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' ./gradlew :app:compileDebugKotlin :app:compileDebugAndroidTestKotlin`: PASS.
- `JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' ./gradlew :app:testDebugUnitTest :app:assembleDebug`: PASS.
- After the sole-main-button revision, `JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' ANDROID_SERIAL=emulator-5554 ./gradlew :app:connectedDebugAndroidTest`: PASS — Android Emulator `Medium_Phone` (Android 14); the connected suite completed successfully.
- Debug APK: `app/build/outputs/apk/debug/app-debug.apk`.
- Root cause: the list Dialog did not install external touch routing until `hideForMeasure()` was called, so the first tap on the Activity's map button was consumed by the Dialog; additionally, list entry cleared the working layer contrary to the requirement to preserve existing segments/nodes.
- Fix: install list Dialog touch routing during `onStart()` and preserve the list working layer when entering measurement.
- Emulator manual smoke on `emulator-5554`: in the offline editor sheet, the first tap on the visible main-map measurement button opened the measurement panel; closing it restored the same `離線草稿` sheet. The Dialog window was resized to the sheet height so the button remains in the Activity touch region.
- Emulator smoke evidence: on `Medium_Phone` / `emulator-5554` in offline edit flow, the editor measurement entry was visible and clickable; system Back during measurement restored the same editor sheet. This provides partial evidence for AC-001, AC-003, and AC-004; map-point measurement and layer visibility remain NOT VERIFIED.
- Emulator main-map measurement smoke: on `Medium_Phone` / `emulator-5554`, the main measurement button opened the panel, a map tap displayed `11446.69 公里`, reset cleared the start point, and close removed the measurement panel. This is PASS evidence for the existing measurement behavior in AC-004.
- Login-dependent multi-gutter list smoke test: NOT VERIFIED — no test credentials were used; emulator-only validation remains limited to offline flow and instrumentation coverage.
- AC-003 clarification: opening `AddGutterListBottomSheet` hides all scope polylines immediately; list-source measurement preserves the working layer and its existing segments/nodes.
- Focused validation after AC-003 fix: `:app:compileDebugKotlin :app:compileDebugAndroidTestKotlin`, `MainShellActivityTest`, and `GutterFormExitUiTest` all PASS on `emulator-5554`.
- Debug fix `00c2943` validation: `:app:compileDebugKotlin :app:compileDebugAndroidTestKotlin`, `MainShellActivityTest`, and `GutterFormExitUiTest` PASS on `emulator-5554`; formal Verification has not been restarted.

## Scope
- Changed only the approved implementation files and the focused Android test host.
- No new dependency, network request, permission, or data format was introduced.
