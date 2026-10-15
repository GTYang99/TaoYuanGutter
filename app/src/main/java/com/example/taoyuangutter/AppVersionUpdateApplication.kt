package com.example.taoyuangutter

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.taoyuangutter.api.AppVersion
import com.example.taoyuangutter.api.AppVersionRepository
import com.example.taoyuangutter.api.AvailableAppUpdate
import com.example.taoyuangutter.common.AppUpdateDownloader
import com.example.taoyuangutter.common.AppUpdateFileDownloader
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.lang.ref.WeakReference

class AppVersionUpdateApplication : Application(), Application.ActivityLifecycleCallbacks {
    private val mainHandler = Handler(Looper.getMainLooper())
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val repository by lazy { AppVersionRepository() }
    private val downloader: AppUpdateFileDownloader by lazy { AppUpdateDownloader() }
    private var startedActivities = 0
    private var wasForeground = false
    private var foregroundEntryPending = false
    private var activeActivity = WeakReference<Activity>(null)
    private var updateDialogVisible = false
    private var downloadJob: Job? = null
    private val prefs by lazy { getSharedPreferences(PREFS_NAME, MODE_PRIVATE) }
    private val backgroundRunnable = Runnable {
        if (startedActivities == 0) wasForeground = false
    }

    override fun onCreate() {
        super.onCreate()
        registerActivityLifecycleCallbacks(this)
    }

    override fun onActivityStarted(activity: Activity) {
        if (startedActivities++ == 0) {
            mainHandler.removeCallbacks(backgroundRunnable)
            if (!wasForeground) foregroundEntryPending = true
            wasForeground = true
        }
    }

    override fun onActivityResumed(activity: Activity) {
        activeActivity = WeakReference(activity)
        if (prefs.getBoolean(KEY_SUPPRESS_NEXT_FOREGROUND_CHECK, false)) {
            prefs.edit().remove(KEY_SUPPRESS_NEXT_FOREGROUND_CHECK).apply()
            if (prefs.getBoolean(KEY_PENDING_INSTALL, false)) {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || packageManager.canRequestPackageInstalls()) {
                    launchPendingInstaller(activity)
                } else {
                    Toast.makeText(activity, R.string.update_install_permission_not_granted, Toast.LENGTH_LONG).show()
                }
            } else {
                updateFile().delete()
            }
            foregroundEntryPending = false
            return
        }
        if (foregroundEntryPending) {
            foregroundEntryPending = false
            checkForUpdate(activity)
        }
    }

    private fun checkForUpdate(activity: Activity) {
        if (activity.isFinishing || activity.isDestroyed || updateDialogVisible) return
        appScope.launch {
            val update = withContext(Dispatchers.IO) {
                (AppVersionUpdateTestHooks.repositoryFactory?.invoke() ?: repository).getAvailableAndroidUpdate()
            } ?: return@launch
            val installed = AppVersion.parse(BuildConfig.VERSION_NAME) ?: return@launch
            if (update.version <= installed) return@launch
            val current = activeActivity.get()?.takeIf { !it.isFinishing && !it.isDestroyed } ?: return@launch
            showUpdatePrompt(current, update)
        }
    }

    private fun showUpdatePrompt(activity: Activity, update: AvailableAppUpdate) {
        if (updateDialogVisible || activity.isFinishing || activity.isDestroyed) return
        updateDialogVisible = true
        MaterialAlertDialogBuilder(activity)
            .setTitle(getString(R.string.update_available_title, update.version.toString()))
            .setMessage(R.string.update_available_message)
            .setNegativeButton(R.string.update_continue) { dialog, _ -> dialog.dismiss() }
            .setPositiveButton(R.string.update_go) { dialog, _ ->
                dialog.dismiss()
                startDownload(activity, update)
            }
            .setOnDismissListener { updateDialogVisible = false }
            .show()
    }

    private fun startDownload(activity: Activity, update: AvailableAppUpdate) {
        val progress = ProgressBar(activity, null, android.R.attr.progressBarStyleHorizontal).apply {
            isIndeterminate = true
            max = 100
        }
        val message = TextView(activity).apply {
            text = getString(R.string.update_download_starting)
            setPadding(0, 0, 0, dp(activity, 12))
        }
        val content = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(activity, 24), dp(activity, 8), dp(activity, 24), 0)
            addView(message, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            addView(progress, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        val dialog = MaterialAlertDialogBuilder(activity)
            .setTitle(R.string.update_download_title)
            .setView(content)
            .setNegativeButton(R.string.update_cancel, null)
            .create()
        dialog.setOnShowListener { dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEGATIVE).setOnClickListener { downloadJob?.cancel() } }
        dialog.setOnCancelListener { downloadJob?.cancel() }
        dialog.show()
        dialog.window?.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
        downloadJob = appScope.launch {
            try {
                val activeDownloader = AppVersionUpdateTestHooks.downloaderFactory?.invoke() ?: downloader
                val file = activeDownloader.download(update.apkUrl, updateFile()) { received, total ->
                    mainHandler.post {
                        if (!dialog.isShowing) return@post
                        if (total > 0) {
                            progress.isIndeterminate = false
                            progress.progress = ((received * 100) / total).coerceIn(0, 100).toInt()
                            message.text = getString(R.string.update_download_progress, progress.progress)
                        } else {
                            message.text = getString(R.string.update_download_bytes, received)
                        }
                    }
                }
                if (dialog.isShowing) dialog.dismiss()
                setPendingInstaller(file)
                openInstallerOrSettings(activity, file)
            } catch (_: CancellationException) {
                if (dialog.isShowing) dialog.dismiss()
                updateFile().delete()
            } catch (_: Exception) {
                if (dialog.isShowing) dialog.dismiss()
                showDownloadFailure(activity, update)
            }
        }
    }

    private fun showDownloadFailure(activity: Activity, update: AvailableAppUpdate) {
        if (activity.isFinishing || activity.isDestroyed) return
        MaterialAlertDialogBuilder(activity)
            .setTitle(R.string.update_download_failed_title)
            .setMessage(R.string.update_download_failed_message)
            .setNegativeButton(R.string.update_continue, null)
            .setPositiveButton(R.string.update_retry) { _, _ -> startDownload(activity, update) }
            .show()
    }

    private fun openInstallerOrSettings(activity: Activity, file: File) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !packageManager.canRequestPackageInstalls()) {
            prefs.edit().putBoolean(KEY_SUPPRESS_NEXT_FOREGROUND_CHECK, true).putBoolean(KEY_PENDING_INSTALL, true).apply()
            MaterialAlertDialogBuilder(activity)
                .setTitle(R.string.update_install_permission_title)
                .setMessage(R.string.update_install_permission_message)
                .setPositiveButton(R.string.update_open_settings) { _, _ ->
                    activity.startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:$packageName")))
                }
                .setNegativeButton(R.string.update_continue) { _, _ ->
                    prefs.edit().remove(KEY_SUPPRESS_NEXT_FOREGROUND_CHECK).remove(KEY_PENDING_INSTALL).apply()
                    file.delete()
                }
                .show()
            return
        }
        launchInstaller(activity, file)
    }

    private fun setPendingInstaller(file: File) {
        prefs.edit().putString(KEY_PENDING_APK, file.absolutePath).putBoolean(KEY_PENDING_INSTALL, true).apply()
    }

    private fun launchPendingInstaller(activity: Activity) {
        val file = prefs.getString(KEY_PENDING_APK, null)?.let(::File)
        if (file == null || !file.isFile) {
            prefs.edit().remove(KEY_PENDING_INSTALL).remove(KEY_PENDING_APK).apply()
            return
        }
        launchInstaller(activity, file)
    }

    private fun launchInstaller(activity: Activity, file: File) {
        val uri = FileProvider.getUriForFile(activity, "$packageName.fileprovider", file)
        prefs.edit().putBoolean(KEY_SUPPRESS_NEXT_FOREGROUND_CHECK, true).putBoolean(KEY_PENDING_INSTALL, false).apply()
        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, APK_MIME_TYPE)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        activity.startActivity(intent)
    }

    private fun updateFile(): File = File(File(cacheDir, UPDATE_CACHE_DIR), APK_FILE_NAME)

    private fun dp(activity: Activity, value: Int): Int =
        (value * activity.resources.displayMetrics.density).toInt()

    override fun onActivityStopped(activity: Activity) {
        startedActivities = (startedActivities - 1).coerceAtLeast(0)
        if (startedActivities == 0) mainHandler.postDelayed(backgroundRunnable, BACKGROUND_DEBOUNCE_MS)
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
    override fun onActivityPaused(activity: Activity) = Unit
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
    override fun onActivityDestroyed(activity: Activity) {
        if (activeActivity.get() === activity) activeActivity.clear()
    }

    override fun onTerminate() {
        appScope.cancel()
        mainHandler.removeCallbacks(backgroundRunnable)
        super.onTerminate()
    }

    companion object {
        private const val PREFS_NAME = "app_update_state"
        private const val KEY_SUPPRESS_NEXT_FOREGROUND_CHECK = "suppress_next_foreground_check"
        private const val KEY_PENDING_INSTALL = "pending_install"
        private const val KEY_PENDING_APK = "pending_apk"
        private const val UPDATE_CACHE_DIR = "app-update"
        private const val APK_FILE_NAME = "update.apk"
        private const val APK_MIME_TYPE = "application/vnd.android.package-archive"
        private const val BACKGROUND_DEBOUNCE_MS = 700L
    }
}
