package com.example.taoyuangutter.gutter

import android.content.Intent
import android.os.Handler
import android.os.Looper
import com.example.taoyuangutter.pending.GutterSessionDraft
import com.example.taoyuangutter.pending.PendingDraftSheetNavigator

class GutterSessionUiCoordinator(
    private val flowCoordinator: GutterSessionFlowCoordinator,
    private val pendingDraftSheetNavigator: PendingDraftSheetNavigator
) {
    data class Hooks(
        val isHostFinishing: () -> Boolean,
        val prepareForNewMapSession: () -> Unit,
        val prepareForResumedMapSession: () -> Unit,
        val bindSheet: (sheet: AddGutterBottomSheet, initialWaypointCount: Int) -> Unit,
        val showSheet: (AddGutterBottomSheet) -> Unit,
        val onMapSessionReady: (draftId: Long?, isOffline: Boolean, sheet: AddGutterBottomSheet) -> Unit,
        val onResumedWaypointsReady: (List<Waypoint>) -> Unit,
        val onRefitRequested: (List<Waypoint>) -> Unit,
        val onReloadRequested: () -> Unit,
        val launchCurve: (Intent) -> Unit,
        val launchOfflineForm: (Intent) -> Unit
    )

    fun showPendingDrafts(onResumeDraft: (GutterSessionDraft) -> Unit) {
        pendingDraftSheetNavigator.show(onResumeDraft)
    }

    fun startAddSession(
        isOfflineMainMode: Boolean,
        hooks: Hooks
    ) {
        hooks.prepareForNewMapSession()
        val start = flowCoordinator.createAddSessionStart(isOfflineMainMode)
        val sheet = start.sheet
        hooks.onMapSessionReady(start.draftId, start.isOffline, sheet)
        hooks.bindSheet(sheet, 0)
        hooks.showSheet(sheet)
    }

    fun resumeDraft(
        draft: GutterSessionDraft,
        context: android.content.Context,
        isOfflineMainMode: Boolean,
        hooks: Hooks
    ) {
        pendingDraftSheetNavigator.dismissIfVisible()
        hooks.prepareForResumedMapSession()

        val resumeAction = flowCoordinator.createResumeAction(
            context = context,
            draft = draft,
            isOfflineMainMode = isOfflineMainMode
        )

        Handler(Looper.getMainLooper()).postDelayed({
            if (hooks.isHostFinishing()) return@postDelayed

            when (resumeAction) {
                is GutterSessionFlowCoordinator.ResumeAction.OpenCurve -> {
                    hooks.launchCurve(resumeAction.intent)
                }
                is GutterSessionFlowCoordinator.ResumeAction.OpenOfflineForm -> {
                    hooks.launchOfflineForm(resumeAction.intent)
                }
                is GutterSessionFlowCoordinator.ResumeAction.ResumeMapSheet -> {
                    val sheet = resumeAction.sheet
                    hooks.onMapSessionReady(resumeAction.draftId, resumeAction.isOffline, sheet)
                    hooks.bindSheet(sheet, resumeAction.initialWaypointCount)
                    hooks.onResumedWaypointsReady(resumeAction.waypoints)
                    hooks.onRefitRequested(resumeAction.waypoints)
                    hooks.showSheet(sheet)
                    if (!isOfflineMainMode) {
                        hooks.onReloadRequested()
                    }
                }
            }
        }, 300L)
    }
}
