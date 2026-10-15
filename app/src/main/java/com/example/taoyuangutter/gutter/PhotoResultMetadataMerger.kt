package com.example.taoyuangutter.gutter

/**
 * Merges form data into a waypoint without losing the current photo transition.
 * A changed or deleted URI invalidates old metadata, while metadata supplied by
 * the current form result remains authoritative.
 */
object PhotoResultMetadataMerger {
    private fun metadataKeys(slot: Int): List<String> = listOf(
        "photo${slot}CapturedAt",
        "photo${slot}ImgId",
        "photo${slot}UploadState",
        "photo${slot}UploadError"
    )

    fun merge(
        existing: Map<String, String>,
        incoming: Map<String, String>
    ): HashMap<String, String> {
        val merged = HashMap(existing).apply { putAll(incoming) }

        (1..3).forEach { slot ->
            val photoKey = "photo$slot"
            if (!incoming.containsKey(photoKey)) return@forEach

            val incomingPhoto = incoming[photoKey]?.trim().orEmpty()
            val existingPhoto = existing[photoKey]?.trim().orEmpty()
            when {
                incomingPhoto.isEmpty() -> {
                    // A returned empty URI means deletion. Do not leave the
                    // deleted server ID available to a later submit.
                    metadataKeys(slot).forEach(merged::remove)
                }

                incomingPhoto != existingPhoto -> {
                    // The old metadata is invalid for a new URI, but metadata
                    // from this form result (success, uploading, capturedAt,
                    // or error) is still current and must be retained.
                    metadataKeys(slot).forEach { key ->
                        merged.remove(key)
                        incoming[key]?.takeIf { it.isNotBlank() }?.let { value ->
                            merged[key] = value
                        }
                    }
                }

                else -> {
                    // Same photo: preserve existing server metadata when the
                    // result omitted an optional field.
                    metadataKeys(slot).forEach { key ->
                        val existingValue = existing[key]
                        if (!existingValue.isNullOrBlank() && merged[key].isNullOrBlank()) {
                            merged[key] = existingValue
                        }
                    }
                }
            }
        }
        return merged
    }
}
