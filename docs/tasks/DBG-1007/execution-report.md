# Execution Report

## Implementation

- Task: `DBG-1007` — center the form map on the current location only when editing an existing point without saved coordinates.
- Branch: `codex/DBG-1007-minimap-location`
- Production changes: the form requests fine and coarse foreground location permission together, accepts either grant, offers one retry after recoverable denial, and uses one bounded location reacquisition after a failed first fix. Permanent denial offers app Settings. Returning from Settings with permission granted starts one acquisition attempt.
- Privacy and behavior: a usable fix moves only the form map camera. It does not update `Waypoint`, `currentLat`/`currentLng`, form coordinate fields, drafts, or a My Location marker. A manual map gesture prevents a late callback from moving the camera. The existing Taoyuan fallback remains available.
- New files: `EditMapLocationPolicy.kt` and `EditMapLocationPolicyTest.kt`.

## Developer Validation

| Command / check | Result | Evidence / impact |
|---|---|---|
| `:app:testDebugUnitTest --tests 'com.example.taoyuangutter.gutter.EditMapLocationPolicyTest'` | PASS | Targeted policy tests passed. |
| `:app:assembleDebug` | PASS | Debug APK produced at `app/build/outputs/apk/debug/app-debug.apk`; variant: `debug`; package: `com.example.taoyuangutter`. |
| `git diff --check` | PASS | No whitespace errors. |
| Android device availability | NOT VERIFIED | An Android emulator is attached (`emulator-5554`), but no permission/location UI cases were executed. The build used a compile-only Maps placeholder, so this APK is not suitable for map runtime verification. AC-001–AC-007 and AC-009–AC-011 remain pending device verification; AC-008 is covered by source/policy gating and the targeted unit tests. |
| CI | NOT VERIFIED | No CI run was available from this worktree. |

The first Gradle attempt could not locate Java through the shell PATH. Validation then ran with Android Studio's bundled JBR. The first manifest-processing attempt lacked `MAPS_API_KEY`; subsequent validation used a temporary, non-secret `MAPS_API_KEY=compile-only-placeholder` in this isolated worktree. `local.properties` was removed afterward; no real key was read or copied.

## Remaining Verification Scope

Use a fixed committed revision and a runtime-capable Maps configuration to verify AC-001 through AC-007 and AC-009 through AC-011. Test saved-coordinate edit and new/draft behavior for AC-002 and AC-008 regressions, including coarse-only permission, recoverable/permanent denial, Settings return, location timeout, and manual pan during an outstanding request. Device/emulator preconditions and exact test steps are in `plan.md`.
