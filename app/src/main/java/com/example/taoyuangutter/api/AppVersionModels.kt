package com.example.taoyuangutter.api

import com.google.gson.annotations.SerializedName
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/** Wire response returned by GET api/appVersion. */
data class AppVersionResponse(
    @SerializedName("success") val success: Boolean?,
    @SerializedName("data") val data: List<AppVersionRecord?>?
)

data class AppVersionRecord(
    @SerializedName("type") val type: String?,
    @SerializedName("version") val version: String?,
    @SerializedName("url") val url: String?
)

data class AvailableAppUpdate(
    val version: AppVersion,
    val apkUrl: HttpUrl
)

/** A strict, numeric three-component app version. */
class AppVersion private constructor(
    val major: Int,
    val minor: Int,
    val patch: Int
) : Comparable<AppVersion> {
    override fun compareTo(other: AppVersion): Int =
        compareValuesBy(this, other, AppVersion::major, AppVersion::minor, AppVersion::patch)

    override fun toString(): String = "$major.$minor.$patch"

    companion object {
        private val FORMAT = Regex("^[0-9]+\\.[0-9]+\\.[0-9]+$")

        fun parse(value: String): AppVersion? {
            if (!FORMAT.matches(value)) return null
            val parts = value.split('.')
            val numbers = parts.map { it.toIntOrNull() ?: return null }
            return AppVersion(numbers[0], numbers[1], numbers[2])
        }
    }
}

fun AppVersionResponse.toAvailableAndroidUpdateOrNull(): AvailableAppUpdate? {
    if (success != true) return null
    val records = data ?: return null
    if (records.size != 1) return null
    val record = records.single() ?: return null
    if (record.type != "android") return null
    val version = AppVersion.parse(record.version ?: return null) ?: return null
    val url = (record.url ?: return null).toHttpUrlOrNull() ?: return null
    if (url.isHttps.not() || url.host.isBlank() || url.username.isNotEmpty() || url.password.isNotEmpty()) return null
    return AvailableAppUpdate(version, url)
}
