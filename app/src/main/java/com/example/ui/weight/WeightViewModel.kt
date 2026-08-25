package com.example.ui.weight

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.entity.WeightEntry
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

class WeightViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val dao = db.weightEntryDao()

    val weightEntries: StateFlow<List<WeightEntry>> = dao.getAllEntries()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addWeightEntry(weightKg: Float, waistCm: Float?) {
        viewModelScope.launch {
            dao.insertEntry(
                WeightEntry(
                    weightKg = weightKg,
                    waistCm = waistCm,
                    date = System.currentTimeMillis()
                )
            )
        }
    }
    
    fun calculateBmi(weightKg: Float, heightCm: Float): Float {
        if (heightCm <= 0) return 0f
        val heightM = heightCm / 100
        return weightKg / (heightM * heightM)
    }

    fun calculateDailyCalories(weightKg: Float, heightCm: Float, age: Int = 30, gender: String = "Male"): Int {
        // Mifflin-St Jeor Equation
        val bmr = (10 * weightKg) + (6.25f * heightCm) - (5 * age)
        return if (gender.equals("Male", ignoreCase = true)) {
            (bmr + 5).toInt()
        } else {
            (bmr - 161).toInt()
        }
    }
}
