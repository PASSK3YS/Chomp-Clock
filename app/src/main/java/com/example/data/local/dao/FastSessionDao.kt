package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.FastSession
import kotlinx.coroutines.flow.Flow

@Dao
interface FastSessionDao {
    @Query("SELECT * FROM fast_sessions ORDER BY startTime DESC")
    fun getAllSessions(): Flow<List<FastSession>>

    @Query("SELECT * FROM fast_sessions ORDER BY startTime DESC")
    suspend fun getAllDirect(): List<FastSession>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: FastSession)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(sessions: List<FastSession>)
    
    @Delete
    suspend fun deleteSession(session: FastSession)

    @Query("DELETE FROM fast_sessions WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("DELETE FROM fast_sessions")
    suspend fun deleteAll()
}
