package com.example.taoyuangutter.gutter

import android.content.Intent

/**
 * 集中 GutterFormActivity 的表單資料打包/解包邏輯，
 * 先減少重複 mapping，後續再逐步收斂常數與整體 args/result 模型。
 */
object GutterFormContract {

    fun putFormDataExtras(intent: Intent, data: Map<String, String>) {
        intent.putExtra(GutterFormActivity.EXTRA_DATA_GUTTER_ID, data["SPI_NUM"] ?: data["gutterId"] ?: "")
        intent.putExtra(GutterFormActivity.EXTRA_DATA_GUTTER_TYPE, data["NODE_TYP"] ?: data["gutterType"] ?: "")
        intent.putExtra(GutterFormActivity.EXTRA_DATA_MAT_TYP, data["MAT_TYP"] ?: data["matTyp"] ?: "")
        intent.putExtra(GutterFormActivity.EXTRA_DATA_COORD_X, data["NODE_X"] ?: data["coordX"] ?: "")
        intent.putExtra(GutterFormActivity.EXTRA_DATA_COORD_Y, data["NODE_Y"] ?: data["coordY"] ?: "")
        intent.putExtra(GutterFormActivity.EXTRA_DATA_COORD_Z, data["NODE_LE"] ?: data["coordZ"] ?: "")
        intent.putExtra(GutterFormActivity.EXTRA_DATA_MEASURE_ID, data["XY_NUM"] ?: data["xyNum"] ?: "")
        
        val coverDep = data["COVER_DEP"] ?: ""
        intent.putExtra(GutterFormActivity.EXTRA_DATA_COVER_DEP, coverDep)
        
        intent.putExtra(GutterFormActivity.EXTRA_DATA_DEPTH, data["NODE_DEP"] ?: data["depth"] ?: "")
        intent.putExtra(GutterFormActivity.EXTRA_DATA_TOP_WIDTH, data["NODE_WID"] ?: data["topWidth"] ?: "")
        intent.putExtra(GutterFormActivity.EXTRA_DATA_IS_BROKEN, data["IS_BROKEN"] ?: data["isBroken"] ?: "")
        intent.putExtra(GutterFormActivity.EXTRA_DATA_IS_HANGING, data["IS_HANGING"] ?: data["isHanging"] ?: "")
        intent.putExtra(GutterFormActivity.EXTRA_DATA_IS_SILT, data["IS_SILT"] ?: data["isSilt"] ?: "")
        intent.putExtra(GutterFormActivity.EXTRA_DATA_IS_CANTOPEN, data["IS_CANTOPEN"] ?: data["isCantOpen"] ?: "")
        intent.putExtra(
            GutterFormActivity.EXTRA_DATA_IS_PENDING_DEPLOY,
            data["IS_PENDING_DEPLOY"] ?: data["is_pendingDeploy"] ?: data["isPendingDeploy"] ?: ""
        )
        intent.putExtra(GutterFormActivity.EXTRA_DATA_REMARKS, data["NODE_NOTE"] ?: data["remarks"] ?: "")
        
        intent.putExtra(GutterFormActivity.EXTRA_DATA_PHOTO_1, data["photo1"] ?: "")
        intent.putExtra(GutterFormActivity.EXTRA_DATA_PHOTO_2, data["photo2"] ?: "")
        intent.putExtra(GutterFormActivity.EXTRA_DATA_PHOTO_3, data["photo3"] ?: "")
        intent.putExtra(GutterFormActivity.EXTRA_DATA_PHOTO_1_ID, data["photo1_id"] ?: "")
        intent.putExtra(GutterFormActivity.EXTRA_DATA_PHOTO_2_ID, data["photo2_id"] ?: "")
        intent.putExtra(GutterFormActivity.EXTRA_DATA_PHOTO_3_ID, data["photo3_id"] ?: "")
        intent.putExtra(GutterFormActivity.EXTRA_DATA_XY_NUM, data["XY_NUM"] ?: data["xyNum"] ?: "")
        intent.putExtra(GutterFormActivity.EXTRA_DATA_NODE_ID, data["_nodeId"] ?: "")
    }

    fun readFormData(intent: Intent): HashMap<String, String> {
        return hashMapOf(
            "SPI_NUM" to (intent.getStringExtra(GutterFormActivity.EXTRA_DATA_GUTTER_ID) ?: ""),
            "NODE_TYP" to (intent.getStringExtra(GutterFormActivity.EXTRA_DATA_GUTTER_TYPE) ?: ""),
            "MAT_TYP" to (intent.getStringExtra(GutterFormActivity.EXTRA_DATA_MAT_TYP) ?: ""),
            "NODE_X" to (intent.getStringExtra(GutterFormActivity.EXTRA_DATA_COORD_X) ?: ""),
            "NODE_Y" to (intent.getStringExtra(GutterFormActivity.EXTRA_DATA_COORD_Y) ?: ""),
            "NODE_LE" to (intent.getStringExtra(GutterFormActivity.EXTRA_DATA_COORD_Z) ?: ""),
            "XY_NUM" to (intent.getStringExtra(GutterFormActivity.EXTRA_DATA_XY_NUM)
                ?: intent.getStringExtra(GutterFormActivity.EXTRA_DATA_MEASURE_ID)
                ?: ""),
            "COVER_DEP" to (intent.getStringExtra(GutterFormActivity.EXTRA_DATA_COVER_DEP) ?: ""),
            "NODE_DEP" to (intent.getStringExtra(GutterFormActivity.EXTRA_DATA_DEPTH) ?: ""),
            "NODE_WID" to (intent.getStringExtra(GutterFormActivity.EXTRA_DATA_TOP_WIDTH) ?: ""),
            "IS_BROKEN" to (intent.getStringExtra(GutterFormActivity.EXTRA_DATA_IS_BROKEN) ?: ""),
            "IS_HANGING" to (intent.getStringExtra(GutterFormActivity.EXTRA_DATA_IS_HANGING) ?: ""),
            "IS_SILT" to (intent.getStringExtra(GutterFormActivity.EXTRA_DATA_IS_SILT) ?: ""),
            "IS_CANTOPEN" to (intent.getStringExtra(GutterFormActivity.EXTRA_DATA_IS_CANTOPEN) ?: ""),
            "IS_PENDING_DEPLOY" to (intent.getStringExtra(GutterFormActivity.EXTRA_DATA_IS_PENDING_DEPLOY) ?: ""),
            "NODE_NOTE" to (intent.getStringExtra(GutterFormActivity.EXTRA_DATA_REMARKS) ?: ""),
            "photo1" to (intent.getStringExtra(GutterFormActivity.EXTRA_DATA_PHOTO_1) ?: ""),
            "photo2" to (intent.getStringExtra(GutterFormActivity.EXTRA_DATA_PHOTO_2) ?: ""),
            "photo3" to (intent.getStringExtra(GutterFormActivity.EXTRA_DATA_PHOTO_3) ?: ""),
            "photo1_id" to (intent.getStringExtra(GutterFormActivity.EXTRA_DATA_PHOTO_1_ID) ?: ""),
            "photo2_id" to (intent.getStringExtra(GutterFormActivity.EXTRA_DATA_PHOTO_2_ID) ?: ""),
            "photo3_id" to (intent.getStringExtra(GutterFormActivity.EXTRA_DATA_PHOTO_3_ID) ?: "")
        )
    }

    fun putResultData(
        intent: Intent,
        waypointIndex: Int,
        latitude: Double? = null,
        longitude: Double? = null,
        basicData: Map<String, String>,
        photo1: String?,
        photo2: String?,
        photo3: String?,
        photo1Id: String? = null,
        photo2Id: String? = null,
        photo3Id: String? = null,
        includeSpiNum: Boolean
    ) {
        latitude?.let { intent.putExtra(GutterFormActivity.RESULT_LATITUDE, it) }
        longitude?.let { intent.putExtra(GutterFormActivity.RESULT_LONGITUDE, it) }
        intent.putExtra(GutterFormActivity.RESULT_WAYPOINT_INDEX, waypointIndex)
        if (includeSpiNum) {
            intent.putExtra(GutterFormActivity.RESULT_DATA_GUTTER_ID, basicData["SPI_NUM"] ?: "")
        }
        intent.putExtra(GutterFormActivity.RESULT_DATA_GUTTER_TYPE, basicData["NODE_TYP"] ?: "")
        intent.putExtra(GutterFormActivity.RESULT_DATA_MAT_TYP, basicData["MAT_TYP"] ?: "")
        intent.putExtra(GutterFormActivity.RESULT_DATA_COORD_X, basicData["NODE_X"] ?: "")
        intent.putExtra(GutterFormActivity.RESULT_DATA_COORD_Y, basicData["NODE_Y"] ?: "")
        intent.putExtra(GutterFormActivity.RESULT_DATA_COORD_Z, basicData["NODE_LE"] ?: "")
        intent.putExtra(GutterFormActivity.RESULT_DATA_MEASURE_ID, basicData["XY_NUM"] ?: "")
        
        val coverDep = basicData["COVER_DEP"] ?: ""
        intent.putExtra(GutterFormActivity.RESULT_DATA_COVER_DEP, coverDep)
        
        intent.putExtra(GutterFormActivity.RESULT_DATA_DEPTH, basicData["NODE_DEP"] ?: "")
        intent.putExtra(GutterFormActivity.RESULT_DATA_TOP_WIDTH, basicData["NODE_WID"] ?: "")
        intent.putExtra(GutterFormActivity.RESULT_DATA_IS_BROKEN, basicData["IS_BROKEN"] ?: "")
        intent.putExtra(GutterFormActivity.RESULT_DATA_IS_HANGING, basicData["IS_HANGING"] ?: "")
        intent.putExtra(GutterFormActivity.RESULT_DATA_IS_SILT, basicData["IS_SILT"] ?: "")
        intent.putExtra(GutterFormActivity.RESULT_DATA_IS_CANTOPEN, basicData["IS_CANTOPEN"] ?: "")
        intent.putExtra(GutterFormActivity.RESULT_DATA_IS_PENDING_DEPLOY, basicData["IS_PENDING_DEPLOY"] ?: "")
        intent.putExtra(GutterFormActivity.RESULT_DATA_REMARKS, basicData["NODE_NOTE"] ?: "")
        
        intent.putExtra(GutterFormActivity.RESULT_DATA_PHOTO_1, photo1 ?: "")
        intent.putExtra(GutterFormActivity.RESULT_DATA_PHOTO_2, photo2 ?: "")
        intent.putExtra(GutterFormActivity.RESULT_DATA_PHOTO_3, photo3 ?: "")
        intent.putExtra(GutterFormActivity.RESULT_DATA_PHOTO_1_ID, photo1Id ?: "")
        intent.putExtra(GutterFormActivity.RESULT_DATA_PHOTO_2_ID, photo2Id ?: "")
        intent.putExtra(GutterFormActivity.RESULT_DATA_PHOTO_3_ID, photo3Id ?: "")
    }

    fun readResultData(intent: Intent?): HashMap<String, String> {
        return hashMapOf(
            "SPI_NUM" to (intent?.getStringExtra(GutterFormActivity.RESULT_DATA_GUTTER_ID) ?: ""),
            "NODE_TYP" to (intent?.getStringExtra(GutterFormActivity.RESULT_DATA_GUTTER_TYPE) ?: ""),
            "MAT_TYP" to (intent?.getStringExtra(GutterFormActivity.RESULT_DATA_MAT_TYP) ?: ""),
            "NODE_X" to (intent?.getStringExtra(GutterFormActivity.RESULT_DATA_COORD_X) ?: ""),
            "NODE_Y" to (intent?.getStringExtra(GutterFormActivity.RESULT_DATA_COORD_Y) ?: ""),
            "NODE_LE" to (intent?.getStringExtra(GutterFormActivity.RESULT_DATA_COORD_Z) ?: ""),
            "XY_NUM" to (intent?.getStringExtra(GutterFormActivity.RESULT_DATA_MEASURE_ID) ?: ""),
            "COVER_DEP" to (intent?.getStringExtra(GutterFormActivity.RESULT_DATA_COVER_DEP) ?: ""),
            "NODE_DEP" to (intent?.getStringExtra(GutterFormActivity.RESULT_DATA_DEPTH) ?: ""),
            "NODE_WID" to (intent?.getStringExtra(GutterFormActivity.RESULT_DATA_TOP_WIDTH) ?: ""),
            "IS_BROKEN" to (intent?.getStringExtra(GutterFormActivity.RESULT_DATA_IS_BROKEN) ?: ""),
            "IS_HANGING" to (intent?.getStringExtra(GutterFormActivity.RESULT_DATA_IS_HANGING) ?: ""),
            "IS_SILT" to (intent?.getStringExtra(GutterFormActivity.RESULT_DATA_IS_SILT) ?: ""),
            "IS_CANTOPEN" to (intent?.getStringExtra(GutterFormActivity.RESULT_DATA_IS_CANTOPEN) ?: ""),
            "IS_PENDING_DEPLOY" to (intent?.getStringExtra(GutterFormActivity.RESULT_DATA_IS_PENDING_DEPLOY) ?: ""),
            "NODE_NOTE" to (intent?.getStringExtra(GutterFormActivity.RESULT_DATA_REMARKS) ?: ""),
            "photo1" to (intent?.getStringExtra(GutterFormActivity.RESULT_DATA_PHOTO_1) ?: ""),
            "photo2" to (intent?.getStringExtra(GutterFormActivity.RESULT_DATA_PHOTO_2) ?: ""),
            "photo3" to (intent?.getStringExtra(GutterFormActivity.RESULT_DATA_PHOTO_3) ?: ""),
            "photo1_id" to (intent?.getStringExtra(GutterFormActivity.RESULT_DATA_PHOTO_1_ID) ?: ""),
            "photo2_id" to (intent?.getStringExtra(GutterFormActivity.RESULT_DATA_PHOTO_2_ID) ?: ""),
            "photo3_id" to (intent?.getStringExtra(GutterFormActivity.RESULT_DATA_PHOTO_3_ID) ?: "")
        )
    }
}
