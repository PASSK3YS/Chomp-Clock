package com.example.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

enum class WeightUnit(val displayName: String, val shortName: String) {
    KG("Kilograms", "kg"),
    LBS("Pounds", "lbs"),
    STONE_LBS("Stone & Pounds", "st & lbs");

    companion object {
        fun fromString(value: String?): WeightUnit {
            return when (value?.uppercase()) {
                "LBS", "POUNDS" -> LBS
                "STONE_LBS", "ST_LBS", "STONE", "ST" -> STONE_LBS
                else -> KG
            }
        }
    }
}

class UserPreferencesRepository(private val context: Context) {

    private val dataStore = context.dataStore

    val userPreferencesFlow: Flow<UserPreferences> = dataStore.data
        .map { preferences ->
            val username = preferences[USERNAME] ?: "User"
            val heightCm = preferences[HEIGHT_CM] ?: 170f
            val gender = preferences[GENDER] ?: "Male"
            val weightUnitStr = preferences[WEIGHT_UNIT] ?: "KG"
            val weightUnit = WeightUnit.fromString(weightUnitStr)
            val useImperial = preferences[USE_IMPERIAL] ?: (weightUnit != WeightUnit.KG)
            val useDarkTheme = preferences[USE_DARK_THEME] ?: true
            val soundsEnabled = preferences[SOUNDS_ENABLED] ?: true
            val profilePicUri = preferences[PROFILE_PIC_URI]
            val avatarId = preferences[AVATAR_ID] ?: "icon:🔥"
            
            UserPreferences(
                username = username,
                heightCm = heightCm,
                gender = gender,
                weightUnit = weightUnit,
                useImperial = useImperial,
                useDarkTheme = useDarkTheme,
                soundsEnabled = soundsEnabled,
                profilePicUri = profilePicUri,
                avatarId = avatarId
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

    suspend fun updateWeightUnit(unit: WeightUnit) {
        dataStore.edit { 
            it[WEIGHT_UNIT] = unit.name
            it[USE_IMPERIAL] = (unit != WeightUnit.KG)
        }
    }

    suspend fun updateUseImperial(useImperial: Boolean) {
        dataStore.edit { 
            it[USE_IMPERIAL] = useImperial
            if (useImperial && it[WEIGHT_UNIT] == WeightUnit.KG.name) {
                it[WEIGHT_UNIT] = WeightUnit.LBS.name
            } else if (!useImperial) {
                it[WEIGHT_UNIT] = WeightUnit.KG.name
            }
        }
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
                prefs[AVATAR_ID] = "uri:$uri"
            } else {
                prefs.remove(PROFILE_PIC_URI)
            }
        }
    }

    suspend fun updateAvatarId(avatarId: String) {
        dataStore.edit { prefs ->
            prefs[AVATAR_ID] = avatarId
            if (avatarId.startsWith("uri:")) {
                prefs[PROFILE_PIC_URI] = avatarId.removePrefix("uri:")
            } else {
                prefs.remove(PROFILE_PIC_URI)
            }
        }
    }

    suspend fun resetAllPreferences() {
        dataStore.edit { it.clear() }
    }

    companion object {
        val USERNAME = stringPreferencesKey("username")
        val HEIGHT_CM = floatPreferencesKey("height_cm")
        val GENDER = stringPreferencesKey("gender")
        val WEIGHT_UNIT = stringPreferencesKey("weight_unit")
        val USE_IMPERIAL = booleanPreferencesKey("use_imperial")
        val USE_DARK_THEME = booleanPreferencesKey("use_dark_theme")
        val SOUNDS_ENABLED = booleanPreferencesKey("sounds_enabled")
        val PROFILE_PIC_URI = stringPreferencesKey("profile_pic_uri")
        val AVATAR_ID = stringPreferencesKey("avatar_id")
    }
}

data class UserPreferences(
    val username: String,
    val heightCm: Float,
    val gender: String, // "Male" or "Female"
    val weightUnit: WeightUnit = WeightUnit.KG,
    val useImperial: Boolean = false,
    val useDarkTheme: Boolean = true,
    val soundsEnabled: Boolean = true,
    val profilePicUri: String? = null,
    val avatarId: String = "icon:🔥"
)
