package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.SavedFoodItem
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedFoodItemDao {
    @Query("SELECT * FROM saved_food_items ORDER BY createdAt DESC")
    fun getAllSavedItems(): Flow<List<SavedFoodItem>>

    @Query("SELECT * FROM saved_food_items ORDER BY createdAt DESC")
    suspend fun getAllDirect(): List<SavedFoodItem>

    @Query("SELECT * FROM saved_food_items WHERE barcode = :barcode LIMIT 1")
    suspend fun getByBarcode(barcode: String): SavedFoodItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: SavedFoodItem): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<SavedFoodItem>)

    @Delete
    suspend fun deleteItem(item: SavedFoodItem)

    @Query("DELETE FROM saved_food_items WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("DELETE FROM saved_food_items")
    suspend fun deleteAll()
}
