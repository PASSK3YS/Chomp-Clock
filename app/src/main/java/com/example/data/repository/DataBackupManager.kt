package com.example.data.repository

import android.content.Context
import android.net.Uri
import com.example.BuildConfig
import com.example.data.local.AppDatabase
import com.example.data.local.entity.FastSession
import com.example.data.local.entity.FoodEntry
import com.example.data.local.entity.SavedFoodItem
import com.example.data.local.entity.WeightEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class BackupImportResult(
    val success: Boolean,
    val fastsCount: Int = 0,
    val foodCount: Int = 0,
    val weightCount: Int = 0,
    val savedFoodsCount: Int = 0,
    val message: String
)

class DataBackupManager(private val context: Context) {
    private val db = AppDatabase.getDatabase(context)
    private val userPrefsRepo = UserPreferencesRepository(context)
    private val isoDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.UK)

    suspend fun generateJsonBackup(): String = withContext(Dispatchers.IO) {
        val fasts = db.fastSessionDao().getAllDirect()
        val food = db.foodEntryDao().getAllDirect()
        val weights = db.weightEntryDao().getAllDirect()
        val savedFoods = db.savedFoodItemDao().getAllDirect()
        val prefs = userPrefsRepo.userPreferencesFlow.firstOrNull()

        val root = JSONObject()
        root.put("app", "ChompClock")
        root.put("schemaVersion", 3)
        root.put("appVersion", BuildConfig.VERSION_NAME)
        root.put("exportedAt", SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.UK).format(Date()))

        // Fasting Sessions
        val fastsArray = JSONArray()
        fasts.forEach { fast ->
            val obj = JSONObject()
            obj.put("id", fast.id)
            obj.put("startTime", fast.startTime)
            obj.put("endTime", fast.endTime)
            obj.put("durationTargetMillis", fast.durationTargetMillis)
            obj.put("startTimeFormatted", isoDateFormat.format(Date(fast.startTime)))
            obj.put("endTimeFormatted", isoDateFormat.format(Date(fast.endTime)))
            fastsArray.put(obj)
        }
        root.put("fastSessions", fastsArray)

        // Food Entries
        val foodArray = JSONArray()
        food.forEach { item ->
            val obj = JSONObject()
            obj.put("id", item.id)
            obj.put("name", item.name)
            obj.put("servingSize", item.servingSize)
            obj.put("calories", item.calories)
            obj.put("mealType", item.mealType)
            obj.put("date", item.date)
            obj.put("dateFormatted", isoDateFormat.format(Date(item.date)))
            if (item.barcode != null) {
                obj.put("barcode", item.barcode)
            }
            foodArray.put(obj)
        }
        root.put("foodEntries", foodArray)

        // Saved Food Items (Custom / Favorite products)
        val savedArray = JSONArray()
        savedFoods.forEach { item ->
            val obj = JSONObject()
            obj.put("id", item.id)
            obj.put("name", item.name)
            obj.put("servingSize", item.servingSize)
            obj.put("calories", item.calories)
            obj.put("defaultMealType", item.defaultMealType)
            obj.put("brandOrSupermarket", item.brandOrSupermarket)
            obj.put("createdAt", item.createdAt)
            if (item.barcode != null) {
                obj.put("barcode", item.barcode)
            }
            savedArray.put(obj)
        }
        root.put("savedFoodItems", savedArray)

        // Weight Entries
        val weightArray = JSONArray()
        weights.forEach { w ->
            val obj = JSONObject()
            obj.put("id", w.id)
            obj.put("weightKg", w.weightKg.toDouble())
            obj.put("date", w.date)
            obj.put("dateFormatted", isoDateFormat.format(Date(w.date)))
            if (w.waistCm != null) {
                obj.put("waistCm", w.waistCm.toDouble())
            }
            weightArray.put(obj)
        }
        root.put("weightEntries", weightArray)

        // User Preferences
        if (prefs != null) {
            val prefObj = JSONObject()
            prefObj.put("username", prefs.username)
            prefObj.put("heightCm", prefs.heightCm.toDouble())
            prefObj.put("gender", prefs.gender)
            prefObj.put("weightUnit", prefs.weightUnit.name)
            prefObj.put("useImperial", prefs.useImperial)
            prefObj.put("useDarkTheme", prefs.useDarkTheme)
            prefObj.put("soundsEnabled", prefs.soundsEnabled)
            prefObj.put("useCustomCalories", prefs.useCustomCalories)
            prefObj.put("customDailyCalories", prefs.customDailyCalories)
            if (prefs.avatarId != null) {
                prefObj.put("avatarId", prefs.avatarId)
            }
            root.put("userPreferences", prefObj)
        }

        root.toString(2)
    }

    suspend fun generateCsvBackup(): String = withContext(Dispatchers.IO) {
        val fasts = db.fastSessionDao().getAllDirect()
        val food = db.foodEntryDao().getAllDirect()
        val weights = db.weightEntryDao().getAllDirect()
        val savedFoods = db.savedFoodItemDao().getAllDirect()
        val prefs = userPrefsRepo.userPreferencesFlow.firstOrNull()

        val sb = StringBuilder()
        sb.append("# CHOMP CLOCK DATA EXPORT (SPREADSHEET CSV)\n")
        sb.append("# Exported: ${isoDateFormat.format(Date())}\n")
        sb.append("# App Version: ${BuildConfig.VERSION_NAME}\n\n")

        // 1. FASTING SESSIONS
        sb.append("--- FASTING SESSIONS ---\n")
        sb.append("Session ID,Start Time,End Time,Duration (Hours),Target (Hours),Completed Target\n")
        fasts.forEach { f ->
            val durationHours = String.format(Locale.UK, "%.2f", (f.endTime - f.startTime).coerceAtLeast(0L) / 3600000.0)
            val targetHours = String.format(Locale.UK, "%.2f", f.durationTargetMillis / 3600000.0)
            val completed = if ((f.endTime - f.startTime) >= f.durationTargetMillis) "YES" else "NO"
            sb.append("${f.id},\"${isoDateFormat.format(Date(f.startTime))}\",\"${isoDateFormat.format(Date(f.endTime))}\",$durationHours,$targetHours,$completed\n")
        }
        sb.append("\n")

        // 2. FOOD & CALORIE LOGS
        sb.append("--- FOOD & NUTRITION LOGS ---\n")
        sb.append("Entry ID,Date & Time,Meal Category,Food / Beverage Name,Portion / Serving,Calories (kcal),Barcode\n")
        food.forEach { item ->
            val cleanName = item.name.replace("\"", "\"\"")
            val cleanServing = item.servingSize.replace("\"", "\"\"")
            val barcodeStr = item.barcode ?: ""
            sb.append("${item.id},\"${isoDateFormat.format(Date(item.date))}\",\"${item.mealType}\",\"$cleanName\",\"$cleanServing\",${item.calories},\"$barcodeStr\"\n")
        }
        sb.append("\n")

        // 3. SAVED / FREQUENT FOOD ITEMS
        sb.append("--- SAVED & FAVORITE FOOD PRODUCTS ---\n")
        sb.append("Saved ID,Product Name,Portion / Serving,Calories (kcal),Default Meal,Brand,Barcode\n")
        savedFoods.forEach { item ->
            val cleanName = item.name.replace("\"", "\"\"")
            val cleanServing = item.servingSize.replace("\"", "\"\"")
            val cleanBrand = item.brandOrSupermarket.replace("\"", "\"\"")
            val barcodeStr = item.barcode ?: ""
            sb.append("${item.id},\"$cleanName\",\"$cleanServing\",${item.calories},\"${item.defaultMealType}\",\"$cleanBrand\",\"$barcodeStr\"\n")
        }
        sb.append("\n")

        // 4. WEIGHT & MEASUREMENTS
        sb.append("--- WEIGHT & BODY MEASUREMENTS ---\n")
        sb.append("Entry ID,Date & Time,Weight (kg),Weight (lbs),Weight (st & lbs),Waist (cm)\n")
        weights.forEach { w ->
            val totalLbs = w.weightKg * 2.20462f
            val stones = (totalLbs / 14).toInt()
            val remLbs = (totalLbs % 14).toInt()
            val stLbsStr = "${stones}st ${remLbs}lbs"
            val waistStr = w.waistCm?.let { String.format(Locale.UK, "%.1f", it) } ?: ""
            sb.append("${w.id},\"${isoDateFormat.format(Date(w.date))}\",${String.format(Locale.UK, "%.2f", w.weightKg)},${String.format(Locale.UK, "%.2f", totalLbs)},\"$stLbsStr\",$waistStr\n")
        }
        sb.append("\n")

        // 5. USER PROFILE & PREFERENCES
        if (prefs != null) {
            sb.append("--- USER PROFILE SUMMARY ---\n")
            sb.append("Username,Gender,Height (cm),Weight Unit,Daily Calorie Goal,Custom Goal Active\n")
            sb.append("\"${prefs.username}\",\"${prefs.gender}\",${prefs.heightCm},\"${prefs.weightUnit.name}\",${prefs.customDailyCalories},\"${prefs.useCustomCalories}\"\n")
        }

        sb.toString()
    }

    suspend fun writeContentToUri(uri: Uri, content: String): Boolean = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                OutputStreamWriter(outputStream).use { writer ->
                    writer.write(content)
                    writer.flush()
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun importFromJsonUri(uri: Uri, clearExisting: Boolean = false): BackupImportResult = withContext(Dispatchers.IO) {
        try {
            val jsonString = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    reader.readText()
                }
            } ?: return@withContext BackupImportResult(false, 0, 0, 0, 0, "Could not open selected file.")

            importFromJsonString(jsonString, clearExisting)
        } catch (e: Exception) {
            e.printStackTrace()
            BackupImportResult(false, 0, 0, 0, 0, "Import failed: ${e.localizedMessage}")
        }
    }

    suspend fun importFromJsonString(jsonString: String, clearExisting: Boolean = false): BackupImportResult = withContext(Dispatchers.IO) {
        try {
            val root = JSONObject(jsonString)

            if (clearExisting) {
                db.fastSessionDao().deleteAll()
                db.foodEntryDao().deleteAll()
                db.weightEntryDao().deleteAll()
                db.savedFoodItemDao().deleteAll()
            }

            var fastsImported = 0
            var foodImported = 0
            var weightImported = 0
            var savedFoodsImported = 0

            // Import Fasting Sessions
            if (root.has("fastSessions")) {
                val fastsArray = root.getJSONArray("fastSessions")
                val fastList = mutableListOf<FastSession>()
                for (i in 0 until fastsArray.length()) {
                    val obj = fastsArray.getJSONObject(i)
                    val startTime = obj.optLong("startTime", 0L)
                    val endTime = obj.optLong("endTime", 0L)
                    val durationTarget = obj.optLong("durationTargetMillis", 16 * 3600000L)
                    if (startTime > 0 && endTime > 0) {
                        fastList.add(
                            FastSession(
                                id = 0, // auto-generate new ID to avoid collisions
                                startTime = startTime,
                                endTime = endTime,
                                durationTargetMillis = durationTarget
                            )
                        )
                    }
                }
                if (fastList.isNotEmpty()) {
                    db.fastSessionDao().insertAll(fastList)
                    fastsImported = fastList.size
                }
            }

            // Import Food Entries
            if (root.has("foodEntries")) {
                val foodArray = root.getJSONArray("foodEntries")
                val foodList = mutableListOf<FoodEntry>()
                for (i in 0 until foodArray.length()) {
                    val obj = foodArray.getJSONObject(i)
                    val name = obj.optString("name", "Imported Food")
                    val serving = obj.optString("servingSize", "1 serving")
                    val calories = obj.optInt("calories", 0)
                    val mealType = obj.optString("mealType", "Breakfast")
                    val date = obj.optLong("date", System.currentTimeMillis())
                    val barcode = if (obj.has("barcode") && !obj.isNull("barcode")) obj.getString("barcode") else null

                    foodList.add(
                        FoodEntry(
                            id = 0,
                            name = name,
                            servingSize = serving,
                            calories = calories,
                            mealType = mealType,
                            date = date,
                            barcode = barcode
                        )
                    )
                }
                if (foodList.isNotEmpty()) {
                    db.foodEntryDao().insertAll(foodList)
                    foodImported = foodList.size
                }
            }

            // Import Saved / Frequent Food Items
            if (root.has("savedFoodItems")) {
                val savedArray = root.getJSONArray("savedFoodItems")
                val savedList = mutableListOf<SavedFoodItem>()
                for (i in 0 until savedArray.length()) {
                    val obj = savedArray.getJSONObject(i)
                    val name = obj.optString("name", "Saved Item")
                    val serving = obj.optString("servingSize", "1 serving")
                    val calories = obj.optInt("calories", 0)
                    val mealType = obj.optString("defaultMealType", "Snacks")
                    val brand = obj.optString("brandOrSupermarket", "Saved Food")
                    val createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                    val barcode = if (obj.has("barcode") && !obj.isNull("barcode")) obj.getString("barcode") else null

                    savedList.add(
                        SavedFoodItem(
                            id = 0,
                            name = name,
                            servingSize = serving,
                            calories = calories,
                            defaultMealType = mealType,
                            brandOrSupermarket = brand,
                            createdAt = createdAt,
                            barcode = barcode
                        )
                    )
                }
                if (savedList.isNotEmpty()) {
                    db.savedFoodItemDao().insertAll(savedList)
                    savedFoodsImported = savedList.size
                }
            }

            // Import Weight Entries
            if (root.has("weightEntries")) {
                val weightArray = root.getJSONArray("weightEntries")
                val weightList = mutableListOf<WeightEntry>()
                for (i in 0 until weightArray.length()) {
                    val obj = weightArray.getJSONObject(i)
                    val weightKg = obj.optDouble("weightKg", 0.0).toFloat()
                    val date = obj.optLong("date", System.currentTimeMillis())
                    val waistCm = if (obj.has("waistCm") && !obj.isNull("waistCm")) obj.getDouble("waistCm").toFloat() else null

                    if (weightKg > 0f) {
                        weightList.add(
                            WeightEntry(
                                id = 0,
                                weightKg = weightKg,
                                date = date,
                                waistCm = waistCm
                            )
                        )
                    }
                }
                if (weightList.isNotEmpty()) {
                    db.weightEntryDao().insertAll(weightList)
                    weightImported = weightList.size
                }
            }

            // Restore Preferences if present
            if (root.has("userPreferences")) {
                val prefObj = root.getJSONObject("userPreferences")
                if (prefObj.has("username")) userPrefsRepo.updateUsername(prefObj.getString("username"))
                if (prefObj.has("heightCm")) userPrefsRepo.updateHeight(prefObj.getDouble("heightCm").toFloat())
                if (prefObj.has("gender")) userPrefsRepo.updateGender(prefObj.getString("gender"))
                if (prefObj.has("useCustomCalories")) userPrefsRepo.updateUseCustomCalories(prefObj.getBoolean("useCustomCalories"))
                if (prefObj.has("customDailyCalories")) userPrefsRepo.updateCustomDailyCalories(prefObj.getInt("customDailyCalories"))
                if (prefObj.has("avatarId")) userPrefsRepo.updateAvatarId(prefObj.getString("avatarId"))
                if (prefObj.has("weightUnit")) {
                    val unitStr = prefObj.getString("weightUnit")
                    try {
                        userPrefsRepo.updateWeightUnit(WeightUnit.valueOf(unitStr))
                    } catch (e: Exception) {}
                }
            }

            val savedInfo = if (savedFoodsImported > 0) ", and $savedFoodsImported saved items" else ""
            BackupImportResult(
                success = true,
                fastsCount = fastsImported,
                foodCount = foodImported,
                weightCount = weightImported,
                savedFoodsCount = savedFoodsImported,
                message = "Successfully imported $fastsImported fasts, $foodImported food logs, $weightImported weight records$savedInfo."
            )
        } catch (e: Exception) {
            e.printStackTrace()
            BackupImportResult(false, 0, 0, 0, 0, "Invalid JSON backup file format: ${e.localizedMessage}")
        }
    }
}
