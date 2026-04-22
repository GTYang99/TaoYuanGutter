package com.example.taoyuangutter.gutter

/**
 * 讓 Fragment 可以要求 Activity 顯示「全頁」loading（覆蓋含 AppBar 的整個畫面）。
 * 主要用於拍照返回後，圖片解碼/草稿同步造成短暫卡頓時的 UX 緩衝。
 */
interface PhotoLoadingHost {
    fun setPhotoLoading(visible: Boolean)
}

