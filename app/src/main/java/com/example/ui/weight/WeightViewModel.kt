package com.example.ui.weight

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.entity.WeightEntry
import com.example.data.repository.UserPreferences
import com.example.data.repository.UserPreferencesRepository
import com.example.data.repository.WeighInFrequency
import com.example.data.repository.WeightUnit
import com.example.util.CalorieWeightCalculator
import com.example.util.WeeklyWeightProjection
import com.example.util.WeightReminderManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class WeightViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val dao = db.weightEntryDao()
    private val userPrefsRepo = UserPreferencesRepository(application)

    val userPreferences: StateFlow<UserPreferences?> = userPrefsRepo.userPreferencesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val weightEntries: StateFlow<List<WeightEntry>> = dao.getAllEntries()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addWeightEntry(weightKg: Float, waistCm: Float?, date: Long = System.currentTimeMillis()) {
        viewModelScope.launch {
            dao.insertEntry(
                WeightEntry(
                    weightKg = weightKg,
                    waistCm = waistCm,
                    date = date
                )
            )
        }
    }

    fun deleteWeightEntry(entry: WeightEntry) {
        viewModelScope.launch {
            dao.deleteEntry(entry)
        }
    }
    
    fun setGoalWeightKg(weightKg: Float?) {
        viewModelScope.launch {
            userPrefsRepo.updateGoalWeight(weightKg)
        }
    }
    
    fun calculateBmi(weightKg: Float, heightCm: Float): Float {
        if (heightCm <= 0) return 0f
        val heightM = heightCm / 100
        return weightKg / (heightM * heightM)
    }

    fun calculateDailyCalories(
        weightKg: Float,
        heightCm: Float,
        age: Int = 30,
        gender: String = "Male",
        waistCm: Float? = null
    ): Int {
        return CalorieWeightCalculator.calculateTdee(weightKg, heightCm, waistCm, age, gender)
    }

    fun calculateWeeklyWeightProjection(
        dailyBudget: Int,
        weightKg: Float,
        heightCm: Float,
        waistCm: Float? = null,
        age: Int = 30,
        gender: String = "Male",
        unit: WeightUnit = WeightUnit.KG
    ): WeeklyWeightProjection {
        return CalorieWeightCalculator.calculateWeeklyProjection(
            dailyBudget = dailyBudget,
            weightKg = weightKg,
            heightCm = heightCm,
            waistCm = waistCm,
            age = age,
            gender = gender,
            unit = unit
        )
    }

    fun updateWeighInReminder(
        enabled: Boolean,
        frequency: WeighInFrequency,
        dayOfWeek: Int,
        hour: Int,
        minute: Int
    ) {
        viewModelScope.launch {
            userPrefsRepo.updateWeighInReminder(
                enabled = enabled,
                frequency = frequency,
                dayOfWeek = dayOfWeek,
                hour = hour,
                minute = minute
            )
            WeightReminderManager.scheduleAlarm(
                context = getApplication(),
                enabled = enabled,
                frequency = frequency,
                dayOfWeek = dayOfWeek,
                hour = hour,
                minute = minute
            )
        }
    }

    fun setWeighInReminderEnabled(enabled: Boolean) {
        viewModelScope.launch {
            val current = userPrefsRepo.userPreferencesFlow.first()
            userPrefsRepo.updateWeighInReminderEnabled(enabled)
            WeightReminderManager.scheduleAlarm(
                context = getApplication(),
                enabled = enabled,
                frequency = current.weighInFrequency,
                dayOfWeek = current.weighInDayOfWeek,
                hour = current.weighInHour,
                minute = current.weighInMinute
            )
        }
    }

    fun sendTestReminder() {
        WeightReminderManager.showWeighInNotification(getApplication(), isTest = true)
    }
}

