# Verification Report

Task: FEAT-0905
Date: 2026-09-05
Branch: feat/退回原因
Verified revision: 34bb01e (implementation: 18c66c7)

## Result

PASS

## Acceptance Criteria

| Criteria | Result | Evidence |
|---|---|---|
| AC-001: 原因區塊位於側溝座標編號上方 | PASS | `fragment_inspect_basic.xml` places `revokeCommentContainer` before `tvSpiNum`; `GutterInspectRevokeCommentTest.showsRevokeCommentAboveXyNumAndKeepsItAfterRecreate` asserts `container.top < xyNum.top`. |
| AC-002: 紅框、指定底色、紅色粗體標題「退回原因」 | PASS | `bg_inspect_revoke_comment.xml` uses the named background and red colors with a 1dp stroke; layout uses the title string, red color, and bold style; Android test asserts title text, color, and bold typeface. |
| AC-003: 顯示完整 revokeComment，支援中文、多行、標點與換行 | PASS | `DitchDetailsRevokeCommentTest` verifies API parsing and Gson round-trip including Chinese text and newline; Android test verifies the exact displayed multiline value and retention after Activity recreation. |
| AC-004: 僅 SPI_STATE=2 且原因非空白時顯示 | PASS | `GutterInspectBasicFragment` checks `spiState == "2" && revokeComment.isNotBlank()`; Android test covers empty, blank, states 1/3, missing, and unknown state and asserts `GONE`. |

## Implementation Review

- `DitchDetails` adds nullable `@SerializedName("revokeComment")` with a default value, preserving compatibility with missing or null response fields.
- `GutterInspectActivity` already serializes and restores `DitchDetails` through Gson; the new field reaches `GutterInspectBasicFragment` through the existing pager path.
- The new UI is scoped to the inspect basic-info layout and uses centralized string/color resources.
- Implementation changes match the approved `plan.md`; no requirement or unrelated production changes were found.

## Test Evidence

- `JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' ./gradlew :app:assembleDebug :app:testDebugUnitTest` — PASS, `BUILD SUCCESSFUL`.
- `JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' ./gradlew :app:assembleDebugAndroidTest :app:connectedDebugAndroidTest` — PASS, `BUILD SUCCESSFUL`.
- Connected device: `Medium_Phone(AVD) - 14`.
- `git diff --check 18c66c7^ 18c66c7` found no whitespace errors in the implementation commit. Existing trailing whitespace in the requirement document is unrelated.

## Regression Review

- Full debug unit-test task and connected Android-test task passed, including the new API and inspect UI tests.
- Existing coordinate rendering remains in the same binding path; the new container is `GONE` unless its condition is met, so non-qualifying records do not reserve layout space.
- JSON round-trip and Activity recreation coverage reduce the risk of losing the field across the existing Intent/ViewPager flow.

## Coverage Note

The manual TEST-005 checks for narrow screens, enlarged font size, and visual comparison against the reference image were not executed in this verification run. Automated layout, color, text, ordering, multiline, and scrolling-related structural checks are present; manual visual confirmation remains recommended before release.

## Final Decision

All acceptance criteria passed and required automated validation passed. Verification is complete; proceed to release, with the manual visual check noted above as residual coverage.
