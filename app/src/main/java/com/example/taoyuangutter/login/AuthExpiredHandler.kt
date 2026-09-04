package com.example.taoyuangutter.login

import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.FragmentActivity
import com.example.taoyuangutter.api.ApiResult
import com.example.taoyuangutter.api.isAuthExpired
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class AuthExpiredHandler(
    private val activity: FragmentActivity,
    private val authNavigator: AuthNavigator = AuthNavigator(activity)
) {
    private var dialog: AlertDialog? = null
    private val guard = AuthExpiredOnceGuard()

    fun reset() {
        guard.reset()
        dialog?.dismiss()
        dialog = null
    }

    fun handleIfAuthExpired(
        error: ApiResult.Error,
        onSaveDraft: (() -> Unit)? = null
    ): Boolean {
        if (activity.isFinishing || activity.isDestroyed) return error.isAuthExpired()

        return guard.handleIfAuthExpired(
            error = error,
            onSaveDraft = onSaveDraft,
            onDraftSaveFailed = { t ->
                Log.e("AuthExpiredHandler", "save draft before logout failed", t)
            },
            onHandle = {
                val builder = MaterialAlertDialogBuilder(activity)
                    .setTitle("登入狀態已失效")
                    .setMessage("登入狀態已失效，請重新登入。系統即將登出。")
                    .setCancelable(false)
                    .setPositiveButton("確定") { _, _ ->
                        try {
                            authNavigator.clearAuthAndGoLogin()
                        } finally {
                            reset()
                        }
                    }

                dialog = builder.show().apply {
                    setCancelable(false)
                    setCanceledOnTouchOutside(false)
                    setOnDismissListener {
                        guard.reset()
                        dialog = null
                    }
                }
            }
        )
    }
}

internal class AuthExpiredOnceGuard {
    private var handling = false

    fun reset() {
        handling = false
    }

    fun handleIfAuthExpired(
        error: ApiResult.Error,
        onSaveDraft: (() -> Unit)? = null,
        onDraftSaveFailed: (Throwable) -> Unit = {},
        onHandle: () -> Unit
    ): Boolean {
        if (!error.isAuthExpired()) return false
        if (handling) return true

        handling = true
        try {
            onSaveDraft?.invoke()
        } catch (t: Throwable) {
            onDraftSaveFailed(t)
        }
        onHandle()
        return true
    }
}
