package com.example.taoyuangutter.gutter

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import com.example.taoyuangutter.pending.GutterSessionDraft
import com.example.taoyuangutter.pending.WaypointSnapshot
import com.google.gson.Gson

class CantOpenSessionSnapshotTest {
    private fun data() = mutableMapOf(
        "NODE_DEP" to "10", "photo2" to "content://two",
        "photo2CapturedAt" to "2026-09-10T10:00:00Z",
        "photo2UploadState" to "uploaded", "photo2ImgId" to "42",
        "photo2UploadError" to ""
    )

    @Test fun restoreKeepsFullPhotoMetadata() {
        val vm = CantOpenSessionViewModel()
        val original = data()
        val cleared = data().apply {
            this["NODE_DEP"] = ""
            this["photo2"] = ""
            this["photo2CapturedAt"] = ""
            this["photo2UploadState"] = "idle"
            this["photo2ImgId"] = ""
        }
        vm.capture(original)
        vm.markCleared(cleared)
        val restored = vm.restore(cleared)
        assertEquals(original["NODE_DEP"], restored["NODE_DEP"])
        assertEquals(original["photo2"], restored["photo2"])
    }

    @Test fun dirtyPhotoWinsOverSnapshot() {
        val vm = CantOpenSessionViewModel()
        val original = data()
        vm.capture(original)
        val cleared = data().apply { this["photo2"] = "content://new" }
        vm.markCleared(data().apply { this["photo2"] = "" })
        vm.markPhotoChanged(2)
        assertEquals("content://new", vm.restore(cleared)["photo2"])
        assertNull(vm.currentSnapshot())
    }

    @Test fun dirtyFieldWinsAndSnapshotIsConsumed() {
        val vm = CantOpenSessionViewModel()
        val original = data()
        vm.capture(original)
        val cleared = data().apply { this["NODE_DEP"] = "" }
        vm.markCleared(cleared)
        val current = cleared.toMutableMap().apply { this["NODE_DEP"] = "12" }
        vm.markFieldChanged("NODE_DEP")
        assertEquals("12", vm.restore(current)["NODE_DEP"])
        assertNull(vm.currentSnapshot())
    }

    @Test fun discardRemovesSessionSnapshotAndInvalidatesCapture() {
        val vm = CantOpenSessionViewModel()
        vm.capture(data())
        val token = vm.newCaptureToken(2)
        assertEquals(true, vm.acceptsCapture(2, token))
        vm.discard()
        assertNull(vm.currentSnapshot())
        assertEquals(false, vm.acceptsCapture(2, token))
    }

    @Test fun newerCaptureTokenRejectsLateResult() {
        val vm = CantOpenSessionViewModel()
        val oldToken = vm.newCaptureToken(2)
        val newToken = vm.newCaptureToken(2)
        assertEquals(false, vm.acceptsCapture(2, oldToken))
        assertEquals(true, vm.acceptsCapture(2, newToken))
    }

    @Test fun captureDoesNotMutateFormDataOrExposeSnapshotInData() {
        val vm = CantOpenSessionViewModel()
        val original = data()
        val before = original.toMap()
        vm.capture(original)
        assertEquals(before, original)
        assertEquals(false, original.keys.any { it.contains("snapshot", ignoreCase = true) })
    }

    @Test fun newSessionHolderDoesNotInheritPreviousSnapshot() {
        val previousSession = CantOpenSessionViewModel()
        previousSession.capture(data())
        val newSession = CantOpenSessionViewModel()
        assertNull(newSession.currentSnapshot())
    }

    @Test fun draftSerializationContainsCurrentDataOnly() {
        val vm = CantOpenSessionViewModel()
        val original = data()
        vm.capture(original)
        val cleared = data().apply { this["NODE_DEP"] = ""; this["photo2"] = "" }
        vm.markCleared(cleared)
        val draft = GutterSessionDraft(waypoints = listOf(WaypointSnapshot(basicData = HashMap(cleared))))
        val json = Gson().toJson(draft)
        assertEquals(false, json.contains("content://two"))
        assertEquals(false, json.contains("snapshot"))
    }
}
