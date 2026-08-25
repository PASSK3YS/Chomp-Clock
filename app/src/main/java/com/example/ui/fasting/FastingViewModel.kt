package com.example.ui.fasting

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.FastingService
import com.example.data.local.AppDatabase
import com.example.data.local.entity.FastSession
import com.example.data.model.DetailedAchievement
import com.example.util.AchievementEngine
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class FastingViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val dao = db.fastSessionDao()
    private val prefs = application.getSharedPreferences("fasting_prefs", Context.MODE_PRIVATE)

    private val _isFasting = MutableStateFlow(false)
    val isFasting = _isFasting.asStateFlow()
    
    private val _startTime = MutableStateFlow(0L)
    val startTime = _startTime.asStateFlow()

    private val _targetDurationMillis = MutableStateFlow(16L * 3600 * 1000)
    val targetDurationMillis = _targetDurationMillis.asStateFlow()

    private val _elapsedMillis = MutableStateFlow(0L)
    val elapsedMillis = _elapsedMillis.asStateFlow()
    
    val pastSessions: StateFlow<List<FastSession>> = dao.getAllSessions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val achievements = db.achievementDao().getAllAchievements()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentStreak = pastSessions.map { sessions ->
        if (sessions.isEmpty()) 0 else sessions.size
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val detailedAchievements: StateFlow<List<DetailedAchievement>> = combine(
        pastSessions,
        db.weightEntryDao().getAllEntries(),
        db.foodEntryDao().getAllEntries(),
        currentStreak
    ) { sessions, weights, foods, streak ->
        AchievementEngine.computeAchievements(
            fastSessions = sessions,
            weightEntries = weights,
            foodEntries = foods,
            currentStreakDays = streak
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Restore active fast if saved
        val savedIsFasting = prefs.getBoolean(KEY_IS_FASTING, false)
        if (savedIsFasting) {
            val savedStart = prefs.getLong(KEY_START_TIME, 0L)
            val savedTarget = prefs.getLong(KEY_TARGET_DURATION, 16L * 3600 * 1000)
            if (savedStart > 0L) {
                _startTime.value = savedStart
                _targetDurationMillis.value = savedTarget
                _elapsedMillis.value = System.currentTimeMillis() - savedStart
                _isFasting.value = true
                ensureServiceRunning(savedStart, savedTarget)
            }
        }

        viewModelScope.launch {
            while (true) {
                if (_isFasting.value) {
                    _elapsedMillis.value = System.currentTimeMillis() - _startTime.value
                }
                delay(1000)
            }
        }
    }

    private fun ensureServiceRunning(start: Long, target: Long) {
        val context = getApplication<Application>()
        try {
            val intent = Intent(context, FastingService::class.java).apply {
                action = FastingService.ACTION_START_FAST
                putExtra(FastingService.EXTRA_START_TIME, start)
                putExtra(FastingService.EXTRA_TARGET_TIME, target)
            }
            context.startForegroundService(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun startFast(durationMillis: Long) {
        val now = System.currentTimeMillis()
        _startTime.value = now
        _targetDurationMillis.value = durationMillis
        _elapsedMillis.value = 0L
        _isFasting.value = true

        prefs.edit()
            .putBoolean(KEY_IS_FASTING, true)
            .putLong(KEY_START_TIME, now)
            .putLong(KEY_TARGET_DURATION, durationMillis)
            .apply()

        ensureServiceRunning(now, durationMillis)
    }

    fun endFast() {
        if (!_isFasting.value) return
        val end = System.currentTimeMillis()
        val start = _startTime.value
        val target = _targetDurationMillis.value
        _isFasting.value = false

        prefs.edit().clear().apply()
        
        viewModelScope.launch {
            dao.insertSession(
                FastSession(
                    startTime = start,
                    endTime = end,
                    durationTargetMillis = target
                )
            )
        }
        
        val context = getApplication<Application>()
        try {
            val intent = Intent(context, FastingService::class.java).apply {
                action = FastingService.ACTION_STOP_FAST
            }
            context.startService(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    fun logPastFast(startTime: Long, endTime: Long, targetDurationMillis: Long) {
        viewModelScope.launch {
            dao.insertSession(
                FastSession(
                    startTime = startTime,
                    endTime = endTime,
                    durationTargetMillis = targetDurationMillis
                )
            )
        }
    }

    fun deleteSession(session: FastSession) {
        viewModelScope.launch {
            dao.deleteSession(session)
        }
    }

    companion object {
        private const val KEY_IS_FASTING = "is_fasting"
        private const val KEY_START_TIME = "start_time"
        private const val KEY_TARGET_DURATION = "target_duration"
    }
}
