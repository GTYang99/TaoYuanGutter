package com.example.taoyuangutter.gutter

internal object GutterRequiredFieldLabels {
    fun labelFor(key: String): String = when (key) {
        "NODE_TYP" -> "側溝形式"
        "XY_NUM" -> "測量座標編號"
        "COVER_DEP" -> "溝蓋板厚度"
        "NODE_DEP" -> "側溝測量深度"
        "NODE_WID" -> "側溝頂寬度"
        "MAT_TYP" -> "側溝材質"
        "IS_BROKEN" -> "溝體結構受損"
        "IS_HANGING" -> "附掛或過路管線"
        "IS_SILT" -> "淤積程度"
        else -> key
    }
}
