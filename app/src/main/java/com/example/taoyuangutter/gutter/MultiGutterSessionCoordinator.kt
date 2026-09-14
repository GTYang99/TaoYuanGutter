package com.example.taoyuangutter.gutter

import com.example.taoyuangutter.pending.GutterSessionDraft
import com.example.taoyuangutter.pending.GutterSessionRepository
import com.example.taoyuangutter.pending.WORKFLOW_MULTI_GUTTER

/** Owns the ordered draft identities for one add-gutter list session. */
class MultiGutterSessionCoordinator(repository: GutterSessionRepository) {
    data class Item(val draftId: Long, val createdAt: Long)

    private val repo = repository
    private val items = mutableListOf<Item>().apply {
        repo.getAll()
            .filter { it.workflowOwnership == WORKFLOW_MULTI_GUTTER }
            .sortedBy { it.createdAt }
            .forEach { add(Item(it.id, it.createdAt)) }
    }

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

    fun items(): List<Item> = items.toList()

    fun drafts(): List<GutterSessionDraft> = items.mapNotNull { repo.getById(it.draftId) }

    fun remove(draftId: Long) {
        items.removeAll { it.draftId == draftId }
    }
}
