package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.FoodEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface FoodEntryDao {
    @Query("SELECT * FROM food_entries ORDER BY date DESC")
    fun getAllEntries(): Flow<List<FoodEntry>>

    @Query("SELECT * FROM food_entries ORDER BY date DESC")
    suspend fun getAllDirect(): List<FoodEntry>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: FoodEntry)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<FoodEntry>)

    @Delete
    suspend fun deleteEntry(entry: FoodEntry)

    @Query("DELETE FROM food_entries WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("DELETE FROM food_entries")
    suspend fun deleteAll()
}
