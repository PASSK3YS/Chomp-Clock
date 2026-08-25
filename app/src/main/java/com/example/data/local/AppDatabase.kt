package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.dao.AchievementDao
import com.example.data.local.dao.FastSessionDao
import com.example.data.local.dao.FoodEntryDao
import com.example.data.local.dao.SavedFoodItemDao
import com.example.data.local.dao.WeightEntryDao
import com.example.data.local.entity.Achievement
import com.example.data.local.entity.FastSession
import com.example.data.local.entity.FoodEntry
import com.example.data.local.entity.SavedFoodItem
import com.example.data.local.entity.WeightEntry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [FastSession::class, WeightEntry::class, FoodEntry::class, Achievement::class, SavedFoodItem::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun fastSessionDao(): FastSessionDao
    abstract fun weightEntryDao(): WeightEntryDao
    abstract fun foodEntryDao(): FoodEntryDao
    abstract fun achievementDao(): AchievementDao
    abstract fun savedFoodItemDao(): SavedFoodItemDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "chomp_clock_database"
                )
                .fallbackToDestructiveMigration()
                .addCallback(DatabaseCallback())
                .build()
                INSTANCE = instance
                instance
            }
        }
        
        private class DatabaseCallback : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        populateDatabase(database.achievementDao())
                    }
                }
            }
            
            suspend fun populateDatabase(dao: AchievementDao) {
                dao.insertAll(
                    listOf(
                        Achievement("1", "First Fast", "Complete your first fast", false),
                        Achievement("2", "3-Day Streak", "Fast for 3 consecutive days", false),
                        Achievement("3", "Weight Logger", "Log your weight 5 times", false)
                    )
                )
            }
        }
    }
}
