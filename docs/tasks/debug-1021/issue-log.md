# Issue Log

## ISS-DBG-1021-001

- task_id: debug-1021
- phase: debug
- category: implementation_regression
- priority: P2
- title: 新增模式資料/照片不足 Dialog 關閉後送出按鈕未恢復
- status: verified
- impact: 新增表單在一次必填驗證失敗後無法再透過送出按鈕重試；按鈕持續呈現 loading 狀態。
- repro_steps:
  - 開啟新增側溝表單。
  - 填入起點/終點及側溝類型，但保留必要欄位或照片不足。
  - 點擊「新增側溝」。
  - 在「資料/照片不足」Dialog 按「確定」。
- expected: Dialog 關閉後，按鈕顯示「新增側溝」且可點擊。
- actual: 按鈕仍顯示「上傳中…」且 disabled。
- evidence:
  - app/src/main/java/com/example/taoyuangutter/gutter/AddGutterBottomSheet.kt:1100-1108
  - app/src/main/java/com/example/taoyuangutter/gutter/AddGutterBottomSheet.kt:1893-1895
  - app/src/main/java/com/example/taoyuangutter/gutter/AddGutterBottomSheet.kt:2010-2018
- resolution: Commit f9d033d changed the validation-failure cleanup to setSubmitLoading(false); independent AC-001 emulator test passed on commit 2cfa7c8.

## ISS-DBG-1021-002

- task_id: debug-1021
- phase: infrastructure
- category: environment
- priority: P2
- title: CI has no result for the task branch
- status: open
- impact: Release readiness cannot be approved until CI reports the required build and unit-test checks for the committed task revision.
- evidence:
  - `plan.md` and an approved retrospective `plan-review.md` are now recorded; the review preserves the original timing deviation.
  - GitHub combined commit status for `604aa34ff9e468e9c27375aa87f20bf5bf40b22d` returned an empty status list.
  - `.github/workflows/android-ci.yml` defines build and unit-test checks, but neither has reported for the revision under verification.
- next_action: infrastructure

## ISS-DBG-1021-003

- task_id: debug-1021
- phase: infrastructure
- category: environment
- priority: P2
- title: Independent API 34 verification cannot access the emulator
- status: resolved
- impact: Before remediation, AC-001 could not be independently verified on the committed revision.
- evidence:
  - Android Studio Device Manager listed only `Medium Phone` (API 36); API 34 system image exists but no API 34 AVD was registered.
  - Sandboxed `adb devices -l` failed to bind the ADB smart socket with `Operation not permitted`; the single elevated retry timed out in automatic review.
  - Android Studio UI actions failed with `noWindowsAvailable` after the Mac was unlocked.
  - Direct `emulator -avd Medium_Phone -no-window -no-audio` exited with code 132.
  - The API 34 `Medium Phone 2` AVD was launched in Android Studio and the focused instrumentation suite passed all three cases on revision `604aa34ff9e468e9c27375aa87f20bf5bf40b22d`; see `verification.md` and `emulator-results/AC-001-verification-604aa34.xml`.
- resolution: Registered/launched an API 34 AVD and ran the focused connected instrumentation test from the clean committed snapshot; 3 tests passed with 0 failures.
- next_action: infrastructure

