package com.example.taoyuangutter.gutter

/** Pure form-flow decisions shared by the gutter form UI. */
internal object GutterFormExitRules {
    fun shouldConfirmCantOpenClear(
        coverThickness: String,
        depth: String,
        topWidth: String,
        materialSelected: Boolean,
        brokenSelected: Boolean,
        hangingSelected: Boolean,
        siltSelected: Boolean,
        hasPhoto2: Boolean,
        hasPhoto3: Boolean
    ): Boolean = listOf(
        coverThickness,
        depth,
        topWidth
    ).any { it.isNotBlank() } || materialSelected || brokenSelected || hangingSelected ||
        siltSelected || hasPhoto2 || hasPhoto3

    fun shouldShowIncompleteExitWarning(
        basicFieldsInvalid: Boolean,
        isVirtual: Boolean,
        photosInvalid: Boolean
    ): Boolean = basicFieldsInvalid || (!isVirtual && photosInvalid)
}
