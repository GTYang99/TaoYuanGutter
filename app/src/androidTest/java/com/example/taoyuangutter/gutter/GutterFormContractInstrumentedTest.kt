package com.example.taoyuangutter.gutter

import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GutterFormContractInstrumentedTest {

    @Test
    fun missingConnectionValueStaysEmptyAcrossFormExtras() {
        val intent = Intent()

        GutterFormContract.putFormDataExtras(
            intent,
            mapOf("is_virtual" to "0", "IS_TIEINPOINT" to "1")
        )

        assertEquals("", intent.getStringExtra(GutterFormActivity.EXTRA_DATA_IS_CONNECTING))
        assertEquals("", GutterFormContract.readFormData(intent)["IS_CONNECTING"])
    }

    @Test
    fun explicitConnectionValueRemainsZero() {
        val intent = Intent()

        GutterFormContract.putFormDataExtras(
            intent,
            mapOf("is_virtual" to "0", "IS_CONNECTING" to "0")
        )

        assertEquals("0", intent.getStringExtra(GutterFormActivity.EXTRA_DATA_IS_CONNECTING))
        assertEquals("0", GutterFormContract.readFormData(intent)["IS_CONNECTING"])
    }
}
