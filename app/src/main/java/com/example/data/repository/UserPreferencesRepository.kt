package com.example.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class UserPreferencesRepository(private val context: Context) {

    private val dataStore = context.dataStore

    val userPreferencesFlow: Flow<UserPreferences> = dataStore.data
        .map { preferences ->
            val username = preferences[USERNAME] ?: "User"
            val heightCm = preferences[HEIGHT_CM] ?: 170f
            val gender = preferences[GENDER] ?: "Male"
            val useImperial = preferences[USE_IMPERIAL] ?: false
            val useDarkTheme = preferences[USE_DARK_THEME] ?: true
            val soundsEnabled = preferences[SOUNDS_ENABLED] ?: true
            val profilePicUri = preferences[PROFILE_PIC_URI]
            
            UserPreferences(
                username = username,
                heightCm = heightCm,
                gender = gender,
                useImperial = useImperial,
                useDarkTheme = useDarkTheme,
                soundsEnabled = soundsEnabled,
                profilePicUri = profilePicUri
            )
        }

    suspend fun updateUsername(username: String) {
        dataStore.edit { it[USERNAME] = username }
    }

    suspend fun updateHeight(heightCm: Float) {
        dataStore.edit { it[HEIGHT_CM] = heightCm }
    }

    suspend fun updateGender(gender: String) {
        dataStore.edit { it[GENDER] = gender }
    }

    suspend fun updateUseImperial(useImperial: Boolean) {
        dataStore.edit { it[USE_IMPERIAL] = useImperial }
    }
    
    suspend fun updateUseDarkTheme(useDarkTheme: Boolean) {
        dataStore.edit { it[USE_DARK_THEME] = useDarkTheme }
    }
    
    suspend fun updateSoundsEnabled(soundsEnabled: Boolean) {
        dataStore.edit { it[SOUNDS_ENABLED] = soundsEnabled }
    }

    suspend fun updateProfilePicUri(uri: String?) {
        dataStore.edit { prefs ->
            if (uri != null) {
                prefs[PROFILE_PIC_URI] = uri
            } else {
                prefs.remove(PROFILE_PIC_URI)
            }
        }
    }

    companion object {
        val USERNAME = stringPreferencesKey("username")
        val HEIGHT_CM = floatPreferencesKey("height_cm")
        val GENDER = stringPreferencesKey("gender")
        val USE_IMPERIAL = booleanPreferencesKey("use_imperial")
        val USE_DARK_THEME = booleanPreferencesKey("use_dark_theme")
        val SOUNDS_ENABLED = booleanPreferencesKey("sounds_enabled")
        val PROFILE_PIC_URI = stringPreferencesKey("profile_pic_uri")
    }
}

data class UserPreferences(
    val username: String,
    val heightCm: Float,
    val gender: String, // "Male" or "Female"
    val useImperial: Boolean,
    val useDarkTheme: Boolean,
    val soundsEnabled: Boolean,
    val profilePicUri: String?
)
