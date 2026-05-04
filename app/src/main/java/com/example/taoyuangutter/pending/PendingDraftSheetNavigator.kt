package com.example.taoyuangutter.pending

import androidx.fragment.app.FragmentManager

class PendingDraftSheetNavigator(
    private val fragmentManager: FragmentManager
) {
    fun show(onResumeDraft: (GutterSessionDraft) -> Unit) {
        val sheet = PendingDraftsBottomSheet.newInstance()
        sheet.onResumeDraft = onResumeDraft
        sheet.show(fragmentManager, PendingDraftsBottomSheet.TAG)
    }

    fun dismissIfVisible() {
        (fragmentManager.findFragmentByTag(PendingDraftsBottomSheet.TAG)
            as? PendingDraftsBottomSheet)
            ?.dismissAllowingStateLoss()
    }
}
