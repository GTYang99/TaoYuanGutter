# Issue Log

| Issue ID | Category | Priority | Status | Description |
|---|---|---:|---|---|
| ISS-DBG-0910-001 | implementation_regression | P1 | fixed_pending_verification | `A0910pt52` has a category-1 `node_img` URL; runtime evidence identifies and the implementation addresses a first-open Activity-to-Fragment photo state synchronization gap in the cant-open flow. |
| ISS-DBG-0910-002 | implementation_regression | P1 | resolved_user_confirmed | User manually adjusted the BottomSheet and confirmed AC-002 on a real device. |
| ISS-DBG-0910-003 | environment | P1 | resolved_user_confirmed | User confirmed authenticated real-device validation for AC-003. Detailed log/screenshot was not supplied in this thread. |
| ISS-DBG-0910-004 | environment | P1 | open | No remote CI workflow is configured. Local full unit test, `assembleDebug`, and targeted direct 0910 instrumentation pass; the full direct package run hits `NoActivityResumedException`, while the Gradle connected task fails with `INSTRUMENTATION_FAILED` and package cleanup returns `DELETE_FAILED_INTERNAL_ERROR`. |

## Evidence

- Supplied response `A0910pt52` contains `node_img[0].fileCategory="1"` and a valid HTTPS URL.
- `NodeDetails` maps `node_img` correctly; response-shape mismatch is therefore ruled out for this case.
- `NodeImg.id` is absent, but current photo display/validation does not require an ID.
- Normal import displays photos correctly. Only `IS_CANTOPEN=1` fails to display on first entry; leaving and re-entering the page displays the photo. This raises cant-open lifecycle/render timing or a state overwrite above response mapping as the leading direction.
- Runtime proof: first-open Logcat shows Activity `p1(photo=true)` at `23:47:40.748`, while Fragment remains `p1(photo=false)` at `23:47:40.750`; after re-entry Fragment becomes `p1(photo=true)` at `23:48:58.434`.

## Completed Confirmation

- Fixture confirms category `1` is present and parsed by the intended `node_img` model path.
- First-open runtime trace confirms Activity `p1(photo=true)` while Fragment remains `p1(photo=false)`.
- Re-entry runtime trace confirms Fragment later receives `p1(photo=true)` through persisted-state synchronization.

## Fix Evidence

- `GutterFormActivity` now calls `GutterBasicInfoFragment.syncPersistedPhotoState(...)` after the import photo state has been applied to the Activity, including slot 1 metadata and upload state.
- The fix is deliberately presentation/state synchronization only; API mapping, URL handling, slot/category mapping, and cant-open slot 2/3 clearing remain unchanged.
- Import download progress no longer assumes three photos. It counts non-empty category URLs and reports the current downloaded URL as `1/1` for the supplied one-photo response.

## Next Action

- Remote CI is unavailable because no workflow is configured; full connected-test environment evidence remains unresolved before Release. Targeted direct 0910 instrumentation passed 6/6 tests.

## Environment Evidence

- Android SDK AVD `Medium_Phone` is available.
- ADB device check passed with `emulator-5554 device` after starting the local ADB daemon with the required host permission.
- Gradle and direct ADB instrumentation were executed with Android Studio's bundled JDK. The direct 0910 instrumentation passed 6/6 tests; the Gradle connected task failed during instrumentation/package cleanup.
