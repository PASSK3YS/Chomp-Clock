package com.example.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.repository.UserPreferences
import com.example.data.repository.UserPreferencesRepository
import com.example.data.repository.WeightUnit
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = UserPreferencesRepository(application)
    private val db = AppDatabase.getDatabase(application)
    
    val userPrefs: StateFlow<UserPreferences?> = repository.userPreferencesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun updateUsername(name: String) = viewModelScope.launch { repository.updateUsername(name) }
    fun updateHeight(cm: Float) = viewModelScope.launch { repository.updateHeight(cm) }
    fun updateGender(gender: String) = viewModelScope.launch { repository.updateGender(gender) }
    fun updateWeightUnit(unit: WeightUnit) = viewModelScope.launch { repository.updateWeightUnit(unit) }
    fun updateUseImperial(useImperial: Boolean) = viewModelScope.launch { repository.updateUseImperial(useImperial) }
    fun updateUseDarkTheme(darkTheme: Boolean) = viewModelScope.launch { repository.updateUseDarkTheme(darkTheme) }
    fun updateSoundsEnabled(sounds: Boolean) = viewModelScope.launch { repository.updateSoundsEnabled(sounds) }
    fun updateProfilePicUri(uri: String?) = viewModelScope.launch { repository.updateProfilePicUri(uri) }
    fun updateAvatarId(avatarId: String) = viewModelScope.launch { repository.updateAvatarId(avatarId) }
    
    fun deleteDeviceData(onComplete: () -> Unit = {}) = viewModelScope.launch {
        db.fastSessionDao().deleteAll()
        db.weightEntryDao().deleteAll()
        db.foodEntryDao().deleteAll()
        repository.resetAllPreferences()
        onComplete()
    }
}
