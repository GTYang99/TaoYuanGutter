package com.example.taoyuangutter

import com.example.taoyuangutter.api.AppVersionRepository
import com.example.taoyuangutter.common.AppUpdateFileDownloader

/** Internal dependency seams for deterministic Android lifecycle and update-flow tests. */
internal object AppVersionUpdateTestHooks {
    @Volatile var repositoryFactory: (() -> AppVersionRepository)? = null
    @Volatile var downloaderFactory: (() -> AppUpdateFileDownloader)? = null
}
