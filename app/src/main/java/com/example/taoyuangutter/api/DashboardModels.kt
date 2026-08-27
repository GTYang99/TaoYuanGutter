package com.example.taoyuangutter.api

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName
import java.lang.reflect.Type

data class DashboardQuery(
    val startDate: String? = null,
    val endDate: String? = null,
    val monthYear: String? = null,
    val month: String? = null
) {
    val isDateRangeSelected: Boolean
        get() = !startDate.isNullOrBlank() && !endDate.isNullOrBlank()

    val isMonthRangeSelected: Boolean
        get() = !monthYear.isNullOrBlank() && !month.isNullOrBlank()
}

data class DashboardResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String?,
    @SerializedName("data") val data: DashboardResponseData?,
    @SerializedName("errors") val errors: Map<String, List<String>>? = null
)

data class DashboardResponseData(
    @SerializedName("調查長度")
    val surveyLength: Map<String, DashboardLengthGroup> = emptyMap(),
    @SerializedName("調查進度")
    val surveyProgress: Map<String, DashboardProgressGroup> = emptyMap()
)

data class DashboardLengthGroup(
    @SerializedName("總長")
    val totalLength: String = "",
    val accounts: Map<String, String> = emptyMap()
)

data class DashboardProgressGroup(
    @SerializedName("總數")
    val totalCount: Int = 0,
    @SerializedName("待匯入")
    val pendingImport: Int = 0,
    @SerializedName("待繪製")
    val pendingDrawing: Int = 0,
    @SerializedName("待修正")
    val pendingFix: Int = 0,
    @SerializedName("已完成")
    val completed: Int = 0,
    @SerializedName("其他")
    val other: Map<String, Int> = emptyMap()
)

data class DashboardSlice(
    val label: String,
    val value: Int,
    val color: Int
)

data class DashboardProgressSummary(
    val totalCount: Int,
    val pendingImport: Int,
    val pendingDrawing: Int,
    val pendingFix: Int,
    val completed: Int,
    val other: Map<String, Int>
)

class DashboardLengthGroupDeserializer : JsonDeserializer<DashboardLengthGroup> {
    override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): DashboardLengthGroup {
        val obj = json?.asJsonObject ?: return DashboardLengthGroup()
        val accounts = linkedMapOf<String, String>()
        var totalLength = ""

        obj.entrySet().forEach { (key, value) ->
            if (key == "總長") {
                totalLength = value.asStringOrBlank()
            } else {
                accounts[key] = value.asStringOrBlank()
            }
        }
        return DashboardLengthGroup(totalLength = totalLength, accounts = accounts)
    }
}

class DashboardProgressGroupDeserializer : JsonDeserializer<DashboardProgressGroup> {
    override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): DashboardProgressGroup {
        val obj = json?.asJsonObject ?: return DashboardProgressGroup()
        var totalCount = 0
        var pendingImport = 0
        var pendingDrawing = 0
        var pendingFix = 0
        var completed = 0
        val other = linkedMapOf<String, Int>()

        obj.entrySet().forEach { (key, value) ->
            when (key) {
                "總數" -> totalCount = value.asIntOrZero()
                "待匯入" -> pendingImport = value.asIntOrZero()
                "待繪製" -> pendingDrawing = value.asIntOrZero()
                "待修正" -> pendingFix = value.asIntOrZero()
                "已完成" -> completed = value.asIntOrZero()
                "其他" -> {
                    val nested = value.asJsonObjectOrNull()
                    nested?.entrySet()?.forEach { (otherKey, otherValue) ->
                        other[otherKey] = otherValue.asIntOrZero()
                    }
                }
            }
        }

        return DashboardProgressGroup(
            totalCount = totalCount,
            pendingImport = pendingImport,
            pendingDrawing = pendingDrawing,
            pendingFix = pendingFix,
            completed = completed,
            other = other
        )
    }
}

private fun JsonElement.asStringOrBlank(): String {
    if (isJsonNull) return ""
    return when {
        isJsonPrimitive && asJsonPrimitive.isNumber -> {
            val number = asJsonPrimitive.asDouble
            if (number == number.toLong().toDouble()) number.toLong().toString() else number.toString()
        }
        else -> asString.trim()
    }.let { if (it == "null") "" else it }
}

private fun JsonElement.asIntOrZero(): Int {
    if (isJsonNull) return 0
    return runCatching {
        when {
            isJsonPrimitive && asJsonPrimitive.isNumber -> asInt
            isJsonPrimitive && asJsonPrimitive.isString -> asString.trim().toIntOrNull() ?: 0
            else -> asString.trim().toIntOrNull() ?: 0
        }
    }.getOrDefault(0)
}

private fun JsonElement.asJsonObjectOrNull(): JsonObject? = runCatching {
    if (isJsonObject) asJsonObject else null
}.getOrNull()
