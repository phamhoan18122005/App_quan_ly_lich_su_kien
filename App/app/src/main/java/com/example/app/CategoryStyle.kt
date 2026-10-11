package com.example.app

import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes

/** Màu vạch + nhãn theo maDanhMuc (1..6 là 6 danh mục mặc định). */
object CategoryStyle {
    @ColorRes fun color(categoryId: Long): Int = when (categoryId) {
        1L, 2L -> R.color.brand_primary
        3L -> R.color.warning
        4L -> R.color.danger
        5L -> R.color.success
        else -> R.color.text_secondary
    }

    @DrawableRes fun tagBg(categoryId: Long): Int = when (categoryId) {
        1L, 2L -> R.drawable.bg_tag_primary
        3L -> R.drawable.bg_tag_warning
        4L -> R.drawable.bg_tag_danger
        5L -> R.drawable.bg_tag_success
        else -> R.drawable.bg_tag_neutral
    }
}
