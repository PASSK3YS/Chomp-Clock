package com.example.util

import com.example.data.local.entity.FastSession
import com.example.data.local.entity.FoodEntry
import com.example.data.local.entity.WeightEntry
import com.example.data.model.AchievementCategory
import com.example.data.model.AchievementRarity
import com.example.data.model.DetailedAchievement
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object AchievementEngine {

    fun computeAchievements(
        fastSessions: List<FastSession>,
        weightEntries: List<WeightEntry>,
        foodEntries: List<FoodEntry>,
        currentStreakDays: Int
    ): List<DetailedAchievement> {
        val totalFasts = fastSessions.size
        val maxDurationHours = fastSessions.maxOfOrNull { 
            (it.endTime - it.startTime) / (1000f * 3600f) 
        } ?: 0f

        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

        // Calculate food metrics
        val totalFoodLogs = foodEntries.size
        val barcodeScans = foodEntries.count { it.barcode != null && it.barcode.isNotBlank() }
        val ukSupermarketItems = foodEntries.count { 
            val n = it.name.lowercase()
            listOf("tesco", "m&s", "sainsbury", "asda", "morrisons", "aldi", "lidl", "waitrose", "co-op", "greggs", "pret", "warburtons", "cadbury", "walkers")
                .any { store -> n.contains(store) }
        }

        // Group foods by day to check multi-meal days
        val dayGroupFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val maxMealsInADay = foodEntries.groupBy { dayGroupFormat.format(Date(it.date)) }
            .values.maxOfOrNull { it.size } ?: 0

        // Weight metrics
        val totalWeightLogs = weightEntries.size
        val hasWaistLog = weightEntries.any { it.waistCm != null && it.waistCm > 0f }

        val list = mutableListOf<DetailedAchievement>()

        // --- Category 1: Fasting Mastery ---
        list.add(
            DetailedAchievement(
                id = "first_fast",
                title = "First Step",
                description = "Complete your very first fast",
                category = AchievementCategory.FASTING,
                iconEmoji = "🎯",
                rarity = AchievementRarity.COMMON,
                currentProgress = totalFasts.coerceAtMost(1),
                targetProgress = 1,
                isUnlocked = totalFasts >= 1,
                unlockedDate = fastSessions.lastOrNull()?.let { dateFormat.format(Date(it.endTime)) },
                tip = "Tap Start Fast on the home screen and conclude your first window."
            )
        )
        list.add(
            DetailedAchievement(
                id = "fast_3",
                title = "Fasting Apprentice",
                description = "Complete 3 fast sessions",
                category = AchievementCategory.FASTING,
                iconEmoji = "🔥",
                rarity = AchievementRarity.COMMON,
                currentProgress = totalFasts.coerceAtMost(3),
                targetProgress = 3,
                isUnlocked = totalFasts >= 3,
                tip = "Consistency is key to adapting your metabolic rhythm."
            )
        )
        list.add(
            DetailedAchievement(
                id = "fast_10",
                title = "Consistent Faster",
                description = "Complete 10 fast sessions",
                category = AchievementCategory.FASTING,
                iconEmoji = "⚡",
                rarity = AchievementRarity.RARE,
                currentProgress = totalFasts.coerceAtMost(10),
                targetProgress = 10,
                isUnlocked = totalFasts >= 10,
                tip = "At 10 fasts, your body seamlessly switches between glucose and ketones."
            )
        )
        list.add(
            DetailedAchievement(
                id = "fast_25",
                title = "Fasting Veteran",
                description = "Complete 25 fast sessions",
                category = AchievementCategory.FASTING,
                iconEmoji = "🛡️",
                rarity = AchievementRarity.EPIC,
                currentProgress = totalFasts.coerceAtMost(25),
                targetProgress = 25,
                isUnlocked = totalFasts >= 25,
                tip = "You have mastered intermittent fasting as a sustainable lifestyle."
            )
        )
        list.add(
            DetailedAchievement(
                id = "fast_50",
                title = "Master of the Fast",
                description = "Complete 50 fast sessions",
                category = AchievementCategory.FASTING,
                iconEmoji = "👑",
                rarity = AchievementRarity.LEGENDARY,
                currentProgress = totalFasts.coerceAtMost(50),
                targetProgress = 50,
                isUnlocked = totalFasts >= 50,
                tip = "True dedication to longevity, metabolic clarity, and vitality."
            )
        )

        // --- Category 2: Duration & Stages ---
        list.add(
            DetailedAchievement(
                id = "fast_16h",
                title = "16:8 Standard",
                description = "Complete a classic 16-hour Intermittent Fast",
                category = AchievementCategory.DURATION,
                iconEmoji = "⏱️",
                rarity = AchievementRarity.COMMON,
                currentProgress = if (maxDurationHours >= 16f) 1 else 0,
                targetProgress = 1,
                isUnlocked = maxDurationHours >= 16f,
                tip = "The gold standard for daily insulin sensitivity and fat oxidation."
            )
        )
        list.add(
            DetailedAchievement(
                id = "fast_18h",
                title = "18:6 Pro",
                description = "Complete an 18-hour fat burning fast",
                category = AchievementCategory.DURATION,
                iconEmoji = "🔥",
                rarity = AchievementRarity.RARE,
                currentProgress = if (maxDurationHours >= 18f) 1 else 0,
                targetProgress = 1,
                isUnlocked = maxDurationHours >= 18f,
                tip = "Accelerates autophagy and elevates natural ketone production."
            )
        )
        list.add(
            DetailedAchievement(
                id = "fast_20h",
                title = "Warrior 20:4",
                description = "Complete a 20-hour Warrior fast",
                category = AchievementCategory.DURATION,
                iconEmoji = "⚔️",
                rarity = AchievementRarity.RARE,
                currentProgress = if (maxDurationHours >= 20f) 1 else 0,
                targetProgress = 1,
                isUnlocked = maxDurationHours >= 20f,
                tip = "Ancient warrior protocol: 20 hours fasting with a 4-hour eating window."
            )
        )
        list.add(
            DetailedAchievement(
                id = "fast_24h",
                title = "OMAD Champion",
                description = "Complete a full 24-hour One Meal A Day fast",
                category = AchievementCategory.DURATION,
                iconEmoji = "🥇",
                rarity = AchievementRarity.EPIC,
                currentProgress = if (maxDurationHours >= 24f) 1 else 0,
                targetProgress = 1,
                isUnlocked = maxDurationHours >= 24f,
                tip = "A 24-hour fast triggers intense cellular recycling and deep glycogen clearance."
            )
        )
        list.add(
            DetailedAchievement(
                id = "fast_36h",
                title = "Autophagy Pioneer",
                description = "Reach deep cellular rejuvenation (36h fast)",
                category = AchievementCategory.DURATION,
                iconEmoji = "🧬",
                rarity = AchievementRarity.EPIC,
                currentProgress = if (maxDurationHours >= 36f) 1 else 0,
                targetProgress = 1,
                isUnlocked = maxDurationHours >= 36f,
                tip = "Promotes maximum mitochondrial biogenesis and cellular cleanup."
            )
        )
        list.add(
            DetailedAchievement(
                id = "fast_48h",
                title = "Monk Fast (48h)",
                description = "Complete a 48-hour extended cleanse",
                category = AchievementCategory.DURATION,
                iconEmoji = "🧘",
                rarity = AchievementRarity.LEGENDARY,
                currentProgress = if (maxDurationHours >= 48f) 1 else 0,
                targetProgress = 1,
                isUnlocked = maxDurationHours >= 48f,
                tip = "Full metabolic recalibration and enhanced mental clarity."
            )
        )

        // --- Category 3: Streaks & Consistency ---
        list.add(
            DetailedAchievement(
                id = "streak_3",
                title = "3-Day Spark",
                description = "Maintain a 3-day fasting streak",
                category = AchievementCategory.STREAKS,
                iconEmoji = "🏃",
                rarity = AchievementRarity.COMMON,
                currentProgress = currentStreakDays.coerceAtMost(3),
                targetProgress = 3,
                isUnlocked = currentStreakDays >= 3,
                tip = "Complete daily fasting sessions without skipping."
            )
        )
        list.add(
            DetailedAchievement(
                id = "streak_7",
                title = "Weekly Champion",
                description = "Maintain a 7-day uninterrupted streak",
                category = AchievementCategory.STREAKS,
                iconEmoji = "🔥",
                rarity = AchievementRarity.RARE,
                currentProgress = currentStreakDays.coerceAtMost(7),
                targetProgress = 7,
                isUnlocked = currentStreakDays >= 7,
                tip = "One whole week of discipline!"
            )
        )
        list.add(
            DetailedAchievement(
                id = "streak_14",
                title = "Fortnight Hero",
                description = "Maintain a 14-day fasting streak",
                category = AchievementCategory.STREAKS,
                iconEmoji = "⚡",
                rarity = AchievementRarity.EPIC,
                currentProgress = currentStreakDays.coerceAtMost(14),
                targetProgress = 14,
                isUnlocked = currentStreakDays >= 14,
                tip = "Two uninterrupted weeks of fasting habit mastery."
            )
        )
        list.add(
            DetailedAchievement(
                id = "streak_30",
                title = "Monthly Legend",
                description = "Maintain a 30-day consistent streak",
                category = AchievementCategory.STREAKS,
                iconEmoji = "🏆",
                rarity = AchievementRarity.LEGENDARY,
                currentProgress = currentStreakDays.coerceAtMost(30),
                targetProgress = 30,
                isUnlocked = currentStreakDays >= 30,
                tip = "30 consecutive days. You are an unstoppable force!"
            )
        )

        // --- Category 4: Nutrition & Food Tracking ---
        list.add(
            DetailedAchievement(
                id = "food_first",
                title = "First Fuel Log",
                description = "Log your first meal in Chomp Clock",
                category = AchievementCategory.NUTRITION,
                iconEmoji = "🥗",
                rarity = AchievementRarity.COMMON,
                currentProgress = totalFoodLogs.coerceAtMost(1),
                targetProgress = 1,
                isUnlocked = totalFoodLogs >= 1,
                tip = "Track your breakfast, lunch, dinner, or snacks to see your daily macros."
            )
        )
        list.add(
            DetailedAchievement(
                id = "food_day_full",
                title = "Balanced Day",
                description = "Log at least 3 meals in a single day",
                category = AchievementCategory.NUTRITION,
                iconEmoji = "📝",
                rarity = AchievementRarity.RARE,
                currentProgress = maxMealsInADay.coerceAtMost(3),
                targetProgress = 3,
                isUnlocked = maxMealsInADay >= 3,
                tip = "Log all your meals throughout the day for complete nutritional awareness."
            )
        )
        list.add(
            DetailedAchievement(
                id = "food_10_items",
                title = "Mindful Eater",
                description = "Log 10 food items total",
                category = AchievementCategory.NUTRITION,
                iconEmoji = "🥑",
                rarity = AchievementRarity.RARE,
                currentProgress = totalFoodLogs.coerceAtMost(10),
                targetProgress = 10,
                isUnlocked = totalFoodLogs >= 10,
                tip = "Consistently tracking your nutrition accelerates progress."
            )
        )
        list.add(
            DetailedAchievement(
                id = "food_barcode_scan",
                title = "Barcode Explorer",
                description = "Scan and log a product via barcode",
                category = AchievementCategory.NUTRITION,
                iconEmoji = "📷",
                rarity = AchievementRarity.COMMON,
                currentProgress = barcodeScans.coerceAtMost(1),
                targetProgress = 1,
                isUnlocked = barcodeScans >= 1,
                tip = "Use the barcode scanner icon in the Food tab to instantly fetch accurate info."
            )
        )
        list.add(
            DetailedAchievement(
                id = "food_uk_supermarket",
                title = "Great British Foodie",
                description = "Log 5 UK supermarket products (Tesco, M&S, etc.)",
                category = AchievementCategory.NUTRITION,
                iconEmoji = "🇬🇧",
                rarity = AchievementRarity.RARE,
                currentProgress = ukSupermarketItems.coerceAtMost(5),
                targetProgress = 5,
                isUnlocked = ukSupermarketItems >= 5,
                tip = "Search or scan items from major UK brands and supermarkets."
            )
        )

        // --- Category 5: Body & Metrics ---
        list.add(
            DetailedAchievement(
                id = "weight_first",
                title = "Scale Starter",
                description = "Record your baseline weight",
                category = AchievementCategory.BODY,
                iconEmoji = "⚖️",
                rarity = AchievementRarity.COMMON,
                currentProgress = totalWeightLogs.coerceAtMost(1),
                targetProgress = 1,
                isUnlocked = totalWeightLogs >= 1,
                tip = "Head over to the Weight tab to log your starting weigh-in."
            )
        )
        list.add(
            DetailedAchievement(
                id = "weight_5",
                title = "Habit Builder",
                description = "Log your weight 5 times",
                category = AchievementCategory.BODY,
                iconEmoji = "📈",
                rarity = AchievementRarity.RARE,
                currentProgress = totalWeightLogs.coerceAtMost(5),
                targetProgress = 5,
                isUnlocked = totalWeightLogs >= 5,
                tip = "Weighing in regularly tracks long-term composition trends."
            )
        )
        list.add(
            DetailedAchievement(
                id = "waist_logged",
                title = "Waist Watcher",
                description = "Record your waistline measurement",
                category = AchievementCategory.BODY,
                iconEmoji = "📐",
                rarity = AchievementRarity.COMMON,
                currentProgress = if (hasWaistLog) 1 else 0,
                targetProgress = 1,
                isUnlocked = hasWaistLog,
                tip = "Waist circumference is one of the best indicators of visceral fat reduction."
            )
        )

        return list
    }
}
