package com.example.taoyuangutter.api

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import java.lang.reflect.Type

/**
 * 容忍後端把 Int 欄位偶發回傳為空字串 "" 的情況（視為 null）。
 *
 * - JSON number → Int
 * - JSON string "123" → Int
 * - JSON string "" / "   " → null
 * - JSON null → null
 *
 * 其他非預期格式會丟出 [JsonParseException]，避免默默吞錯造成資料異常。
 */
class EmptyStringToNullIntAdapter : JsonDeserializer<Int?> {
    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): Int? {
        if (json == null || json.isJsonNull) return null

        if (!json.isJsonPrimitive) {
            throw JsonParseException("Expected int but was $json")
        }

        val primitive = json.asJsonPrimitive
        return when {
            primitive.isNumber -> primitive.asNumber.toInt()
            primitive.isString -> {
                val s = primitive.asString.trim()
                if (s.isEmpty()) null
                else s.toIntOrNull() ?: throw JsonParseException("Expected int but was string=$s")
            }
            else -> throw JsonParseException("Expected int but was $json")
        }
    }
}

