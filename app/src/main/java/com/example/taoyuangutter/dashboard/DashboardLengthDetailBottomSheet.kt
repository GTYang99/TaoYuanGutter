package com.example.taoyuangutter.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.example.taoyuangutter.R
import com.example.taoyuangutter.databinding.SheetDashboardLengthDetailBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class DashboardLengthDetailBottomSheet : BottomSheetDialogFragment() {

    private var _binding: SheetDashboardLengthDetailBinding? = null
    private val binding get() = _binding!!

    override fun getTheme(): Int = R.style.TransparentBottomSheetDialog

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = SheetDashboardLengthDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnClose.setOnClickListener { dismissAllowingStateLoss() }

        val groupName = requireArguments().getString(ARG_GROUP_NAME).orEmpty()
        val accountsJson = requireArguments().getString(ARG_ACCOUNTS_JSON).orEmpty()
        binding.tvTitle.text = getString(R.string.dashboard_length_detail_group_title, groupName)

        val type = object : TypeToken<Map<String, String>>() {}.type
        val accounts: Map<String, String> = runCatching {
            Gson().fromJson<Map<String, String>>(accountsJson, type)
        }.getOrDefault(emptyMap())

        renderAccounts(accounts)
    }

    override fun onStart() {
        super.onStart()
        (dialog as? BottomSheetDialog)?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)?.let { sheet ->
            sheet.layoutParams = sheet.layoutParams.apply { height = ViewGroup.LayoutParams.WRAP_CONTENT }
            val behavior = BottomSheetBehavior.from(sheet)
            behavior.isFitToContents = true
            behavior.skipCollapsed = true
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun renderAccounts(accounts: Map<String, String>) {
        binding.accountContainer.removeAllViews()
        val ordered = accounts.entries.sortedBy { it.key }
        if (ordered.isEmpty()) {
            binding.accountContainer.addView(TextView(requireContext()).apply {
                text = getString(R.string.dashboard_no_account_data)
                setTextColor(ContextCompat.getColor(requireContext(), R.color.textColorSecondary))
                textSize = 14f
                setPadding(dp(4), dp(8), dp(4), dp(8))
            })
            return
        }

        ordered.forEachIndexed { index, entry ->
            val row = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(dp(4), dp(10), dp(4), dp(10))
            }
            row.addView(TextView(requireContext()).apply {
                text = entry.key
                setTextColor(ContextCompat.getColor(requireContext(), android.R.color.black))
                textSize = 16f
            }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            row.addView(TextView(requireContext()).apply {
                text = "${entry.value} ${getString(R.string.dashboard_km_unit)}"
                setTextColor(ContextCompat.getColor(requireContext(), R.color.dashboard_detail_label))
                textSize = 16f
            })
            binding.accountContainer.addView(row)
            if (index != ordered.lastIndex) {
                binding.accountContainer.addView(View(requireContext()).apply {
                    layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1)).apply {
                        topMargin = dp(4)
                        bottomMargin = dp(4)
                    }
                    setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.border_grey))
                })
            }
        }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    companion object {
        private const val ARG_GROUP_NAME = "group_name"
        private const val ARG_ACCOUNTS_JSON = "accounts_json"

        fun newInstance(groupName: String, accountsJson: String): DashboardLengthDetailBottomSheet {
            return DashboardLengthDetailBottomSheet().apply {
                arguments = Bundle().apply {
                    putString(ARG_GROUP_NAME, groupName)
                    putString(ARG_ACCOUNTS_JSON, accountsJson)
                }
            }
        }
    }
}
