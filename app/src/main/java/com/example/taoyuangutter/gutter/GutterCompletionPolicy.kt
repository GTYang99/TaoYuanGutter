package com.example.taoyuangutter.gutter

/**
 * Shared completion rules for a gutter waypoint.
 *
 * The same rules are used by form validation, submit validation and the
 * waypoint list status so that a partially filled item cannot look complete.
 */
internal object GutterCompletionPolicy {
    val detailRequiredKeys = listOf(
        "MAT_TYP",
        "COVER_DEP",
        "NODE_DEP",
        "NODE_WID",
        "IS_BROKEN",
        "IS_HANGING",
        "IS_SILT"
    )

    fun parseLooseBoolean(raw: String?): Boolean = when (raw?.trim()?.lowercase()) {
        "1", "true", "t", "y", "yes" -> true
        else -> false
    }

    fun isDetailExempt(data: Map<String, String>): Boolean =
        parseLooseBoolean(data["IS_CANTOPEN"]) ||
            parseLooseBoolean(data["IS_TIEINPOINT"])

    fun requiredBasicKeys(isVirtual: Boolean, requiresMeasureId: Boolean): List<String> {
        if (isVirtual) {
            return listOf("NODE_X", "NODE_Y") +
                if (requiresMeasureId) listOf("XY_NUM") else emptyList()
        }

        return listOf("NODE_TYP", "NODE_X", "NODE_Y") +
            if (requiresMeasureId) listOf("XY_NUM") else emptyList()
    }

    fun requiredPhotoSlots(isVirtual: Boolean, detailExempt: Boolean): List<Int> {
        if (isVirtual) return emptyList()
        return if (detailExempt) listOf(1) else listOf(1, 2, 3)
    }

    fun hasRequiredValues(data: Map<String, String>, keys: List<String>): Boolean =
        keys.all { !data[it].isNullOrBlank() }

    fun isComplete(
        data: Map<String, String>,
        isVirtual: Boolean,
        requiresMeasureId: Boolean,
        hasCoordinates: Boolean,
        photoUsable: (slot: Int) -> Boolean
    ): Boolean {
        val detailExempt = isDetailExempt(data)
        val requiredKeys = requiredBasicKeys(isVirtual, requiresMeasureId) +
            if (!isVirtual && !detailExempt) detailRequiredKeys else emptyList()
        return hasCoordinates &&
            hasRequiredValues(data, requiredKeys) &&
            requiredPhotoSlots(isVirtual, detailExempt).all(photoUsable)
    }
}
