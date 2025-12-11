package com.example.petshop

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Product(
    val id: String = "",
    val name: String = "",
    val price: Double = 0.0,
    val description: String? = null,
    val imageUrl: String = "",
    val category: String = "" // Thêm trường để lưu tên danh mục
) : Parcelable