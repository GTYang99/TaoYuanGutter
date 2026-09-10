# Debug Fix Plan

## Gate

Do not begin implementation until the supplied `A0910pt52` response is deserialized successfully and runtime evidence identifies whether the failure is in image download/local-URI creation or Fragment/Activity state synchronization. The plan review must also confirm the source/revision conflict is safe.

## Minimum Fix Candidates

1. Add a presentation-only Chinese label map for the four required validation keys, preserving raw keys in `basicData`, request construction, and API contracts.
2. Change the BottomSheet sizing constant from 50% to 60% in the existing behavior setup, then verify `peekHeight`, expanded state, viewport inset callbacks, RecyclerView scrolling, and bottom buttons.
3. Use the supplied fixture to verify `NodeDetails.nodeImg` deserialization and category-1 URL selection; treat `url` compatibility as a separate non-regression check, not as the confirmed cause for `A0910pt52`.
4. If the download returns no readable local URI, fix the narrow download/error handling path. If the URI is readable but the first-open screen is empty, fix the single authoritative state update and lifecycle rendering path so slot 1 is retained and rendered before confirmation validation; preserve captured time/image ID and keep slot 2/3 optional.
5. Add regression coverage for first-open import, reopen import, normal import, missing image, response-shape compatibility, and metadata preservation before/after confirmation.

## Candidate Validation

- Unit test raw-key-to-Chinese-label mapping and unknown-key fallback.
- Unit test `NodeDetails` deserialization for the supplied `node_img` fixture; retain a compatibility test for the legacy `url` photo array if that contract is still supported.
- Android UI test for all four missing-field labels and no raw API key leakage.
- Android UI test or manual measurement for 60% sheet height and viewport behavior.
- Android UI test for cant-open import: one usable slot-1 photo visible, confirmation enabled after download, no reopen required.
- Regression test normal three-photo import and existing slot image ID/upload state.
- Verify the supplied `node_img` category-1 response imports slot 1 and enables confirmation after a readable local URI is available; separately verify legacy `url` compatibility if required.
- Run `git diff --check`, targeted tests, unit tests, debug build, and connected/manual validation where available. Every unavailable check is `NOT VERIFIED`.

## Rollback

Revert the isolated implementation commit after preserving task evidence; do not revert unrelated `feat-0910-1` or `feat-0910-2` revisions.

## Open Questions

- Confirm exact Chinese labels and whether “一張照片” means fixed slot 1/fileCategory 1.
- Confirm the intended source revision for cant-open toggle behavior because task documents and current source disagree.
