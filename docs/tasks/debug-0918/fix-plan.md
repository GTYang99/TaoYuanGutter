# Fix Plan

This is the minimum implementation scope after root-cause analysis.

1. Enforce the clarified contract: an unchanged URL-only photo imported from
   `nodeDetails` is displayed and excluded from `nodeImage`; only a newly
   captured or replaced photo is uploaded. Do not merge `ditchDetails` and
   `nodeDetails` records across unrelated API results.
2. Preserve the existing replacement behavior: replacing or deleting a photo
   clears the old association and makes the new local photo eligible where
   required.
3. Preserve an existing `photo{slot}ImgId` when a later imported detail
   response omits `nodeImg[].id`; use a newly returned response ID when present.
4. Change only the deleted-area initial defaults to `false` across the
   controller, bottom-sheet argument fallback/default, and checkbox XML. Keep
   explicit state propagation unchanged.
5. Update the import-sheet header layout so the title is centered against the
   full header width, independent of whether the right location button is
   visible. Preserve the existing 48dp control hit areas.
6. Run targeted unit/UI validation, `git diff --check`, the relevant debug
   build, and a source/device smoke check for initial layer state and title
   placement.
