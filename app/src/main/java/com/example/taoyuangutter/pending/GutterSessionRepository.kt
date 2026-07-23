package com.example.taoyuangutter.pending

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.UUID

/**
 * 待上傳側溝草稿的本機儲存庫。
 *
 * 使用 SharedPreferences + Gson 序列化，儲存帶有完整 waypoints 的 [GutterSessionDraft]。
 * 現行流程已統一為整條草稿；`isSinglePoint` 僅保留作舊資料相容。
 */
class GutterSessionRepository(context: Context) {

    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(LEGACY_PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()
    private val draftDao = GutterDraftDatabase.getInstance(appContext).draftDao()

    companion object {
        private const val LEGACY_PREFS_NAME = "gutter_session_drafts"
        private const val LEGACY_KEY_DRAFTS = "session_drafts_json"
        private const val KEY_LEGACY_MIGRATED = "legacy_drafts_migrated"
    }

    init {
        migrateLegacyDraftsIfNeeded()
    }

    // ── 讀取 ──────────────────────────────────────────────────────────────

    /** 取得所有草稿，依儲存時間降冪排列（最新的在最前面）。 */
    fun getAll(): List<GutterSessionDraft> {
        return draftDao.getAll().mapNotNull(::entityToDraft)
    }

    /** 依 id 取得單一草稿，找不到時回傳 null。 */
    fun getById(id: Long): GutterSessionDraft? = draftDao.getById(id)?.let(::entityToDraft)

    // ── 寫入 ──────────────────────────────────────────────────────────────

    /**
     * 儲存（新增或更新）一筆草稿。
     * 若 [draft.id] 已存在，則以新內容覆蓋；否則新增。
     */
    fun save(draft: GutterSessionDraft) {
        draftDao.upsert(draft.toEntity())
    }

    /** 依 id 刪除一筆草稿。 */
    fun delete(id: Long) {
        draftDao.deleteById(id)
    }

    // ── 私有工具 ──────────────────────────────────────────────────────────

    private fun migrateLegacyDraftsIfNeeded() {
        if (prefs.getBoolean(KEY_LEGACY_MIGRATED, false)) return
        if (draftDao.count() > 0) {
            prefs.edit().putBoolean(KEY_LEGACY_MIGRATED, true).apply()
            return
        }

        val legacyJson = prefs.getString(LEGACY_KEY_DRAFTS, null)
        val legacyDrafts = parseLegacyDrafts(legacyJson)
        if (legacyDrafts.isNotEmpty()) {
            legacyDrafts.forEach { draftDao.upsert(it.toEntity()) }
        }

        prefs.edit()
            .remove(LEGACY_KEY_DRAFTS)
            .putBoolean(KEY_LEGACY_MIGRATED, true)
            .apply()
    }

    private fun parseLegacyDrafts(json: String?): List<GutterSessionDraft> {
        if (json.isNullOrBlank()) return emptyList()
        return try {
            val type = object : TypeToken<List<GutterSessionDraft>>() {}.type
            gson.fromJson<List<GutterSessionDraft>>(json, type).orEmpty()
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun entityToDraft(entity: DraftEntity): GutterSessionDraft? {
        val waypoints = try {
            val type = object : TypeToken<List<WaypointSnapshot>>() {}.type
            gson.fromJson<List<WaypointSnapshot>>(entity.waypointsJson, type).orEmpty()
        } catch (_: Exception) {
            return null
        }
        val normalizedWaypoints = waypoints.map { snapshot ->
            if (snapshot.uid.isNotBlank()) snapshot else snapshot.copy(uid = UUID.randomUUID().toString())
        }
        return GutterSessionDraft(
            id = entity.id,
            savedAt = entity.savedAt,
            kind = entity.kind,
            isOffline = entity.isOffline,
            isSinglePoint = entity.isSinglePoint,
            waypoints = normalizedWaypoints
        )
    }

    private fun GutterSessionDraft.toEntity(): DraftEntity {
        return DraftEntity(
            id = id,
            savedAt = savedAt,
            kind = kind,
            isOffline = isOffline,
            isSinglePoint = isSinglePoint,
            waypointsJson = gson.toJson(waypoints)
        )
    }
}
