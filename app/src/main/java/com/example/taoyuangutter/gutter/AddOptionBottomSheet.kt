package com.example.taoyuangutter.gutter

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.taoyuangutter.R
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton

/**
 * 選項彈窗 BottomSheet：提供「新增側溝」與「新增曲線」兩個入口。
 *
 * 使用方式：
 *   AddOptionBottomSheet().show(supportFragmentManager, AddOptionBottomSheet.TAG)
 */
class AddOptionBottomSheet : BottomSheetDialogFragment() {

    /** 點選「新增側溝」的回調 */
    var onAddGutterClicked: (() -> Unit)? = null

    /** 點選「新增曲線」的回調 */
    var onAddCurveClicked: (() -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.bottom_sheet_add_option, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<MaterialButton>(R.id.btnAddGutter).setOnClickListener {
            dismiss()
            onAddGutterClicked?.invoke()
        }

        view.findViewById<MaterialButton>(R.id.btnAddCurve).setOnClickListener {
            dismiss()
            onAddCurveClicked?.invoke()
        }
    }

    companion object {
        const val TAG = "AddOptionBottomSheet"
    }
}
