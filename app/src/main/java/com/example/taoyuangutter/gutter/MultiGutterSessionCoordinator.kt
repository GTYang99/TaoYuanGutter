package com.example.taoyuangutter.gutter

import com.example.taoyuangutter.pending.GutterSessionDraft
import com.example.taoyuangutter.pending.GutterSessionRepository
import com.example.taoyuangutter.pending.WORKFLOW_MULTI_GUTTER

/** Owns the ordered draft identities for one add-gutter list session. */
class MultiGutterSessionCoordinator(repository: GutterSessionRepository) {
    data class Item(val draftId: Long, val createdAt: Long)

    private val repo = repository
    private val items = mutableListOf<Item>()

    fun addItem(): Item {
        val item = Item(repo.allocateDraftId(), System.currentTimeMillis())
        items += item
        return item
    }

    fun restoreItem(draft: GutterSessionDraft) {
        if (draft.workflowOwnership == WORKFLOW_MULTI_GUTTER && items.none { it.draftId == draft.id }) {
            items += Item(draft.id, draft.createdAt)
        }
    }

    /** Restores only the IDs belonging to the active add-list session. */
    fun restoreItems(draftIds: List<Long>) {
        draftIds.forEach { draftId ->
            repo.getById(draftId)
                ?.takeIf { it.workflowOwnership == WORKFLOW_MULTI_GUTTER }
                ?.let(::restoreItem)
        }
    }

    fun items(): List<Item> = items.toList()

    fun drafts(): List<GutterSessionDraft> = items.mapNotNull { repo.getById(it.draftId) }

    fun remove(draftId: Long) {
        items.removeAll { it.draftId == draftId }
    }

    /** Ends the current add-list session without deleting the drafts just finalized. */
    fun clearSession() {
        items.clear()
    }
}
