package com.example.taoyuangutter.pending

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * 待上傳側溝草稿的本機儲存庫。
 *
 * 使用 SharedPreferences + Gson 序列化，儲存帶有完整 waypoints 的 [GutterSessionDraft]。
 * 同時作為離線單點草稿（isOffline=true, isSinglePoint=true）的唯一儲存後端。
 */
class GutterSessionRepository(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson  = Gson()

    companion object {
        private const val PREFS_NAME = "gutter_session_drafts"
        private const val KEY_DRAFTS = "session_drafts_json"
    }

    // ── 讀取 ──────────────────────────────────────────────────────────────

    /** 取得所有草稿，依儲存時間降冪排列（最新的在最前面）。 */
    fun getAll(): List<GutterSessionDraft> {
        val json = prefs.getString(KEY_DRAFTS, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<GutterSessionDraft>>() {}.type
            val list: List<GutterSessionDraft> = gson.fromJson(json, type)
            list.sortedByDescending { it.savedAt }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /** 依 id 取得單一草稿，找不到時回傳 null。 */
    fun getById(id: Long): GutterSessionDraft? = getAll().firstOrNull { it.id == id }

    // ── 寫入 ──────────────────────────────────────────────────────────────

    /**
     * 儲存（新增或更新）一筆草稿。
     * 若 [draft.id] 已存在，則以新內容覆蓋；否則新增。
     */
    fun save(draft: GutterSessionDraft) {
        val current = getAll().toMutableList()
        val idx = current.indexOfFirst { it.id == draft.id }
        if (idx >= 0) current[idx] = draft else current.add(0, draft)
        persist(current)
    }

    /** 依 id 刪除一筆草稿。 */
    fun delete(id: Long) {
        val updated = getAll().filter { it.id != id }
        persist(updated)
    }

    // ── 私有工具 ──────────────────────────────────────────────────────────

    private fun persist(drafts: List<GutterSessionDraft>) {
        prefs.edit().putString(KEY_DRAFTS, gson.toJson(drafts)).apply()
    }
}
