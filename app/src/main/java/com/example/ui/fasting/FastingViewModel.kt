package com.example.ui.fasting

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.FastingService
import com.example.data.local.AppDatabase
import com.example.data.local.entity.FastSession
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class FastingViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val dao = db.fastSessionDao()

    private val _isFasting = MutableStateFlow(false)
    val isFasting = _isFasting.asStateFlow()
    
    private val _startTime = MutableStateFlow(0L)
    private val _targetDurationMillis = MutableStateFlow(16L * 3600 * 1000) // 16h preset

    private val _elapsedMillis = MutableStateFlow(0L)
    val elapsedMillis = _elapsedMillis.asStateFlow()
    
    val pastSessions: StateFlow<List<FastSession>> = dao.getAllSessions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val achievements = db.achievementDao().getAllAchievements()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentStreak = pastSessions.map { sessions ->
        // Simple streak logic for demonstration
        if (sessions.isEmpty()) 0 else {
           // Basic logic: count consecutive days
           // For prototype, we'll just return the number of sessions
           sessions.size
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    init {
        viewModelScope.launch {
            while (true) {
                if (_isFasting.value) {
                    _elapsedMillis.value = System.currentTimeMillis() - _startTime.value
                }
                delay(1000)
            }
        }
    }

    fun startFast(durationMillis: Long) {
        val now = System.currentTimeMillis()
        _startTime.value = now
        _targetDurationMillis.value = durationMillis
        _elapsedMillis.value = 0L
        _isFasting.value = true

        val context = getApplication<Application>()
        val intent = Intent(context, FastingService::class.java).apply {
            action = FastingService.ACTION_START_FAST
            putExtra(FastingService.EXTRA_START_TIME, now)
            putExtra(FastingService.EXTRA_TARGET_TIME, durationMillis)
        }
        context.startForegroundService(intent)
    }

    fun endFast() {
        if (!_isFasting.value) return
        val end = System.currentTimeMillis()
        _isFasting.value = false
        
        viewModelScope.launch {
            dao.insertSession(
                FastSession(
                    startTime = _startTime.value,
                    endTime = end,
                    durationTargetMillis = _targetDurationMillis.value
                )
            )
        }
        
        val context = getApplication<Application>()
        val intent = Intent(context, FastingService::class.java).apply {
            action = FastingService.ACTION_STOP_FAST
        }
        context.startService(intent)
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
}
