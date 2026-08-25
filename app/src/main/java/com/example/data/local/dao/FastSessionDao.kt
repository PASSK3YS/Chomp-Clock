package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.FastSession
import kotlinx.coroutines.flow.Flow

@Dao
interface FastSessionDao {
    @Query("SELECT * FROM fast_sessions ORDER BY startTime DESC")
    fun getAllSessions(): Flow<List<FastSession>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: FastSession)
    
    @Query("DELETE FROM fast_sessions")
    suspend fun deleteAll()
}
