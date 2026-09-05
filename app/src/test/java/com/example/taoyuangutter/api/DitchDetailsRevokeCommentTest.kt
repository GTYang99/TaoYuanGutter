package com.example.taoyuangutter.api

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DitchDetailsRevokeCommentTest {

    private val gson = Gson()

    @Test
    fun parsesRevokeCommentFromDitchDetailsResponse() {
        val json = """
            {
              "success": true,
              "message": "查詢成功",
              "data": {
                "ditch_id": 1306,
                "SPI_NUM": "3202-ZZ-00979",
                "SPI_STATE": "2",
                "XY_NUM": {
                  "起點": "A0624pt46",
                  "節點": ["A0624pt45", "A0624pt44"],
                  "終點": "A0624pt43"
                },
                "SPI_TYP": "1",
                "STR_X": "268081.743",
                "STR_Y": "2766981.756",
                "END_X": "267985.071",
                "END_Y": "2767042.770",
                "STR_LE": "",
                "END_LE": "",
                "NODE_XY": "268051.061,2766980.608_268023.674,2767006.712",
                "STR_DEP": 90,
                "END_DEP": 63,
                "STR_WID": 59,
                "END_WID": 62,
                "LENG": "121.36",
                "SLOP": "0.00000",
                "NOTE": "",
                "nodes": [],
                "revokeComment": "終點，若現場淤泥、土石或雜物垃圾厚度未達溝體淨深一半，請協助調整"
              }
            }
        """.trimIndent()

        val response = gson.fromJson(json, DitchDetailsResponse::class.java)

        assertEquals(1306, response.data?.ditchId)
        assertEquals("A0624pt46", response.data?.xyNum?.start)
        assertEquals(2, response.data?.xyNum?.nodes?.size)
        assertEquals(emptyList<DitchNode>(), response.data?.nodes)
        assertEquals("終點，若現場淤泥、土石或雜物垃圾厚度未達溝體淨深一半，請協助調整", response.data?.revokeComment)
    }

    @Test
    fun keepsRevokeCommentThroughJsonRoundTrip() {
        val comment = "第一行退回原因\n第二行包含中文、標點，並保留長文字。"
        val original = createDitchDetails(spiState = "2", revokeComment = comment)

        val restored = gson.fromJson(gson.toJson(original), DitchDetails::class.java)

        assertEquals(comment, restored.revokeComment)
        assertEquals(original.ditchId, restored.ditchId)
        assertEquals(original.spiNum, restored.spiNum)
    }

    @Test
    fun parsesEmptyBlankNullAndMissingRevokeCommentSafely() {
        assertEquals("", parseRevokeCommentValue("\"\""))
        assertEquals("   ", parseRevokeCommentValue("\"   \""))
        assertNull(parseRevokeCommentValue("null"))

        val missingJson = baseResponseJson(revokeCommentEntry = "")
        val missing = gson.fromJson(missingJson, DitchDetailsResponse::class.java)

        assertNull(missing.data?.revokeComment)
    }

    private fun parseRevokeCommentValue(valueJson: String): String? {
        val response = gson.fromJson(
            baseResponseJson(revokeCommentEntry = ""","revokeComment": $valueJson"""),
            DitchDetailsResponse::class.java
        )
        return response.data?.revokeComment
    }

    private fun baseResponseJson(revokeCommentEntry: String): String = """
        {
          "success": true,
          "message": "查詢成功",
          "data": {
            "ditch_id": 1,
            "SPI_NUM": "SPI-001",
            "SPI_STATE": "2",
            "SPI_TYP": "1",
            "STR_X": "",
            "STR_Y": "",
            "END_X": "",
            "END_Y": "",
            "STR_LE": "",
            "END_LE": "",
            "NODE_XY": "",
            "STR_DEP": "",
            "END_DEP": "",
            "STR_WID": "",
            "END_WID": "",
            "LENG": "",
            "SLOP": "",
            "NOTE": "",
            "nodes": []
            $revokeCommentEntry
          }
        }
    """.trimIndent()

    private fun createDitchDetails(spiState: String?, revokeComment: String?): DitchDetails {
        return DitchDetails(
            ditchId = 1,
            spiNum = "SPI-001",
            spiState = spiState,
            isCurve = "0",
            isPendingDeploy = "0",
            isVirtual = "0",
            spiTyp = "1",
            xyNum = DitchXyNum(start = "A", nodes = listOf("B"), end = "C"),
            strX = "1",
            strY = "2",
            endX = "3",
            endY = "4",
            strLe = "",
            endLe = "",
            nodeXy = "",
            strDep = "90",
            endDep = "63",
            strWid = "59",
            endWid = "62",
            leng = "121.36",
            slop = "0.00000",
            note = "",
            nodes = emptyList(),
            revokeComment = revokeComment
        )
    }
}
