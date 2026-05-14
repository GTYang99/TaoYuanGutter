package com.example.taoyuangutter.gutter

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.bumptech.glide.Glide
import com.example.taoyuangutter.R
import com.example.taoyuangutter.databinding.DialogImageDetailBinding

class ImageDetailDialogFragment : DialogFragment() {

    private var _binding: DialogImageDetailBinding? = null
    private val binding get() = _binding!!

    companion object {
        private const val ARG_IMAGE_URL = "image_url"

        fun newInstance(imageUrl: String): ImageDetailDialogFragment {
            return ImageDetailDialogFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_IMAGE_URL, imageUrl)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = DialogImageDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val imageUrl = arguments?.getString(ARG_IMAGE_URL)
        if (!imageUrl.isNullOrBlank()) {
            Glide.with(this)
                .load(imageUrl)
                .into(binding.photoView)
        }

        binding.btnClose.setOnClickListener {
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
