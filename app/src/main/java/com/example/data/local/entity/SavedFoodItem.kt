package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_food_items")
data class SavedFoodItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val servingSize: String,
    val calories: Int,
    val defaultMealType: String = "Snacks",
    val barcode: String? = null,
    val brandOrSupermarket: String = "Saved Food",
    val createdAt: Long = System.currentTimeMillis()
)
