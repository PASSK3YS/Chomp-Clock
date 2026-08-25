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

enum class HeightUnit(val displayName: String, val shortName: String) {
    CM("Centimeters", "cm"),
    FT_IN("Feet & Inches", "ft/in");

    companion object {
        fun fromString(value: String?): HeightUnit {
            return when (value?.uppercase()) {
                "FT_IN", "FEET_INCHES", "IMPERIAL" -> FT_IN
                else -> CM
            }
        }
    }
}

enum class ThemeMode(val displayName: String, val description: String) {
    DARK("Dark Mode", "High contrast AMOLED dark styling"),
    LIGHT("Light Mode", "Crisp, bright & high readability styling"),
    SYSTEM("System Default", "Follows device system appearance");

    companion object {
        fun fromString(value: String?): ThemeMode {
            return when (value?.uppercase()) {
                "LIGHT" -> LIGHT
                "SYSTEM" -> SYSTEM
                else -> DARK
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
            val waistCm = preferences[WAIST_CM]
            val gender = preferences[GENDER] ?: "Male"
            val weightUnitStr = preferences[WEIGHT_UNIT] ?: "KG"
            val weightUnit = WeightUnit.fromString(weightUnitStr)
            val heightUnitStr = preferences[HEIGHT_UNIT] ?: if (weightUnit != WeightUnit.KG) "FT_IN" else "CM"
            val heightUnit = HeightUnit.fromString(heightUnitStr)
            val useImperial = preferences[USE_IMPERIAL] ?: (weightUnit != WeightUnit.KG)
            
            val themeModeStr = preferences[THEME_MODE]
            val themeMode = if (themeModeStr != null) {
                ThemeMode.fromString(themeModeStr)
            } else {
                val oldDark = preferences[USE_DARK_THEME] ?: true
                if (oldDark) ThemeMode.DARK else ThemeMode.LIGHT
            }

            val soundsEnabled = preferences[SOUNDS_ENABLED] ?: true
            val profilePicUri = preferences[PROFILE_PIC_URI]
            val avatarId = preferences[AVATAR_ID] ?: "icon:🔥"
            val useCustomCalories = preferences[USE_CUSTOM_CALORIES] ?: false
            val customDailyCalories = preferences[CUSTOM_DAILY_CALORIES] ?: 2000
            
            UserPreferences(
                username = username,
                heightCm = heightCm,
                waistCm = waistCm,
                gender = gender,
                weightUnit = weightUnit,
                heightUnit = heightUnit,
                useImperial = useImperial,
                themeMode = themeMode,
                useDarkTheme = (themeMode == ThemeMode.DARK),
                soundsEnabled = soundsEnabled,
                profilePicUri = profilePicUri,
                avatarId = avatarId,
                useCustomCalories = useCustomCalories,
                customDailyCalories = customDailyCalories
            )
        }

    suspend fun updateUsername(username: String) {
        dataStore.edit { it[USERNAME] = username }
    }

    suspend fun updateHeight(heightCm: Float) {
        dataStore.edit { it[HEIGHT_CM] = heightCm }
    }

    suspend fun updateWaist(waistCm: Float?) {
        dataStore.edit { 
            if (waistCm != null && waistCm > 0f) {
                it[WAIST_CM] = waistCm
            } else {
                it.remove(WAIST_CM)
            }
        }
    }

    suspend fun updateHeightUnit(unit: HeightUnit) {
        dataStore.edit { it[HEIGHT_UNIT] = unit.name }
    }

    suspend fun updateGender(gender: String) {
        dataStore.edit { it[GENDER] = gender }
    }

    suspend fun updateUseCustomCalories(enabled: Boolean) {
        dataStore.edit { it[USE_CUSTOM_CALORIES] = enabled }
    }

    suspend fun updateCustomDailyCalories(calories: Int) {
        dataStore.edit { it[CUSTOM_DAILY_CALORIES] = calories }
    }

    suspend fun updateWeightUnit(unit: WeightUnit) {
        dataStore.edit { 
            it[WEIGHT_UNIT] = unit.name
            it[USE_IMPERIAL] = (unit != WeightUnit.KG)
        }
    }

    suspend fun updateThemeMode(themeMode: ThemeMode) {
        dataStore.edit { 
            it[THEME_MODE] = themeMode.name
            it[USE_DARK_THEME] = (themeMode == ThemeMode.DARK)
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
        updateThemeMode(if (useDarkTheme) ThemeMode.DARK else ThemeMode.LIGHT)
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
        val WAIST_CM = floatPreferencesKey("waist_cm")
        val GENDER = stringPreferencesKey("gender")
        val WEIGHT_UNIT = stringPreferencesKey("weight_unit")
        val HEIGHT_UNIT = stringPreferencesKey("height_unit")
        val USE_IMPERIAL = booleanPreferencesKey("use_imperial")
        val USE_DARK_THEME = booleanPreferencesKey("use_dark_theme")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val SOUNDS_ENABLED = booleanPreferencesKey("sounds_enabled")
        val PROFILE_PIC_URI = stringPreferencesKey("profile_pic_uri")
        val AVATAR_ID = stringPreferencesKey("avatar_id")
        val USE_CUSTOM_CALORIES = booleanPreferencesKey("use_custom_calories")
        val CUSTOM_DAILY_CALORIES = intPreferencesKey("custom_daily_calories")
    }
}

data class UserPreferences(
    val username: String,
    val heightCm: Float,
    val waistCm: Float? = null,
    val gender: String, // "Male" or "Female"
    val weightUnit: WeightUnit = WeightUnit.KG,
    val heightUnit: HeightUnit = HeightUnit.CM,
    val useImperial: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.DARK,
    val useDarkTheme: Boolean = true,
    val soundsEnabled: Boolean = true,
    val profilePicUri: String? = null,
    val avatarId: String = "icon:🔥",
    val useCustomCalories: Boolean = false,
    val customDailyCalories: Int = 2000
)
