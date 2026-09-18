# Fix Plan

This is the minimum implementation scope after root-cause analysis; no
production code is changed in this debug pass.

1. Record the clarified contract: unchanged imported/downloaded photos are
   intentionally excluded from `nodeImage`; only replaced/new photos upload,
   and their returned IDs reach existing-node `storeDitch`.
2. Preserve the existing replacement behavior: replacing or deleting a photo
   clears the old association and makes the new local photo eligible where
   required.
3. Change only the deleted-area initial defaults to `false` across the
   controller, bottom-sheet argument fallback/default, and checkbox XML. Keep
   explicit state propagation unchanged.
4. Update the import-sheet header layout so the title is centered against the
   full header width, independent of whether the right location button is
   visible. Preserve the existing 48dp control hit areas.
5. Run targeted unit/UI validation, `git diff --check`, the relevant debug
   build, and a source/device smoke check for initial layer state and title
   placement.
