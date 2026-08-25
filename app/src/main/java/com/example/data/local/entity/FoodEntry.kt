package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "food_entries")
data class FoodEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val servingSize: String,
    val calories: Int,
    val mealType: String,
    val date: Long,
    val barcode: String? = null
)
