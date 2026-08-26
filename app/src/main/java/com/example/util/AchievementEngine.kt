package com.example.util

import com.example.data.local.entity.FastSession
import com.example.data.local.entity.FoodEntry
import com.example.data.local.entity.WeightEntry
import com.example.data.model.AchievementCategory
import com.example.data.model.AchievementRarity
import com.example.data.model.DayStreakItem
import com.example.data.model.DetailedAchievement
import com.example.data.model.StreakDetails
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object AchievementEngine {

    fun computeStreakDetails(
        fastSessions: List<FastSession>,
        isFastActiveNow: Boolean = false
    ): StreakDetails {
        val dayFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dayNameFormat = SimpleDateFormat("EEE", Locale.getDefault())
        val dayMonthFormat = SimpleDateFormat("d MMM", Locale.getDefault())

        val completedSessions = fastSessions.filter { it.endTime > it.startTime }
        val completedDateStrings = completedSessions.map { dayFormat.format(Date(it.endTime)) }.toSet()

        val todayCal = Calendar.getInstance()
        val todayStr = dayFormat.format(todayCal.time)
        val isTodayCompleted = completedDateStrings.contains(todayStr)

        // Streak calculation
        val yesterdayCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
        val yesterdayStr = dayFormat.format(yesterdayCal.time)
        val isYesterdayCompleted = completedDateStrings.contains(yesterdayStr)

        var streak = 0
        val checkCalendar = Calendar.getInstance()

        if (isTodayCompleted) {
            streak = 1
            checkCalendar.add(Calendar.DAY_OF_YEAR, -1)
            while (completedDateStrings.contains(dayFormat.format(checkCalendar.time))) {
                streak++
                checkCalendar.add(Calendar.DAY_OF_YEAR, -1)
            }
        } else if (isYesterdayCompleted || isFastActiveNow) {
            if (isYesterdayCompleted) {
                streak = 1
                checkCalendar.time = yesterdayCal.time
                checkCalendar.add(Calendar.DAY_OF_YEAR, -1)
                while (completedDateStrings.contains(dayFormat.format(checkCalendar.time))) {
                    streak++
                    checkCalendar.add(Calendar.DAY_OF_YEAR, -1)
                }
            } else if (isFastActiveNow && completedSessions.isNotEmpty()) {
                streak = 0
            }
        } else {
            streak = 0
        }

        // Longest streak
        val sortedDates = completedDateStrings.mapNotNull {
            try { dayFormat.parse(it) } catch (e: Exception) { null }
        }.sorted()

        var longest = 0
        var currentConsecutive = 0
        var lastDate: Date? = null

        for (date in sortedDates) {
            if (lastDate == null) {
                currentConsecutive = 1
            } else {
                val diffDays = ((date.time - lastDate.time) / (1000 * 60 * 60 * 24)).toInt()
                if (diffDays == 1) {
                    currentConsecutive++
                } else if (diffDays > 1) {
                    currentConsecutive = 1
                }
            }
            lastDate = date
            if (currentConsecutive > longest) {
                longest = currentConsecutive
            }
        }
        longest = maxOf(longest, streak)

        val totalFastingDays = completedDateStrings.size
        val totalFastingHours = completedSessions.sumOf { (it.endTime - it.startTime).toDouble() }.toFloat() / (1000f * 3600f)

        // Recent 7 Days
        val recent7Days = (6 downTo 0).map { daysAgo ->
            val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -daysAgo) }
            val dStr = dayFormat.format(cal.time)
            DayStreakItem(
                dayName = if (daysAgo == 0) "Today" else dayNameFormat.format(cal.time),
                dateLabel = dayMonthFormat.format(cal.time),
                isCompleted = completedDateStrings.contains(dStr),
                isToday = daysAgo == 0,
                dayTimestamp = cal.timeInMillis
            )
        }

        // Next Milestone
        val milestones = listOf(2, 3, 5, 7, 10, 14, 21, 30, 50, 75, 100)
        val nextMilestone = milestones.firstOrNull { it > streak } ?: (streak + 25)
        val daysToNextMilestone = maxOf(1, nextMilestone - streak)
        val milestoneProgress = (streak.toFloat() / nextMilestone).coerceIn(0f, 1f)

        val (tierTitle, tierEmoji, motivationalText) = when {
            streak >= 100 -> Triple("Centennial Legend", "👑", "100+ days of unwavering discipline. You have reached absolute metabolic mastery!")
            streak >= 60 -> Triple("Iron Will Titan", "⚡", "60+ days unbroken! Your metabolic flexibility and consistency are elite.")
            streak >= 30 -> Triple("Monthly Inferno", "🏆", "A full month of continuous dedication. Intermittent fasting is your lifestyle superpower!")
            streak >= 21 -> Triple("Habit Champion", "🧠", "21+ days! Fasting is now a second-nature automatic habit wired into your brain.")
            streak >= 14 -> Triple("Fortnight Blaze", "🛡️", "Two weeks of consistent fasting. Your mental clarity and autophagy are peaking.")
            streak >= 7 -> Triple("Weekly Flame", "🔥", "A full 7-day streak! Fantastic discipline—keep pushing for the fortnight.")
            streak >= 5 -> Triple("Five-Day Surge", "⚡", "5 consecutive days! Your body is adapting seamlessly to daily fat oxidation.")
            streak >= 3 -> Triple("Ignition Spark", "🏃", "3 days in a row! You've successfully built initial habit momentum.")
            streak >= 1 -> Triple("Active Ember", "🌱", "Streak ignited! Complete your fast today to keep the flame growing.")
            else -> Triple("Spark Needed", "✨", "Complete a fast today to ignite your streak and build your fasting habit chain!")
        }

        return StreakDetails(
            currentStreak = streak,
            longestStreak = longest,
            totalFastingDays = totalFastingDays,
            totalFastingHours = totalFastingHours,
            isTodayCompleted = isTodayCompleted,
            isFastActiveNow = isFastActiveNow,
            nextMilestone = nextMilestone,
            daysToNextMilestone = daysToNextMilestone,
            milestoneProgress = milestoneProgress,
            streakTierTitle = tierTitle,
            streakTierEmoji = tierEmoji,
            motivationalText = motivationalText,
            recent7Days = recent7Days
        )
    }

    fun computeAchievements(
        fastSessions: List<FastSession>,
        weightEntries: List<WeightEntry>,
        foodEntries: List<FoodEntry>,
        currentStreakDays: Int,
        longestStreakDays: Int = currentStreakDays,
        hasTargetWeightSet: Boolean = false,
        userHeightCm: Float? = null
    ): List<DetailedAchievement> {
        val completedFasts = fastSessions.filter { it.endTime > it.startTime }
        val totalFasts = completedFasts.size
        val maxDurationHours = completedFasts.maxOfOrNull { 
            (it.endTime - it.startTime) / (1000f * 3600f) 
        } ?: 0f

        val totalFastingHours = completedFasts.sumOf { (it.endTime - it.startTime).toDouble() }.toFloat() / (1000f * 3600f)
        val targetReachedCount = completedFasts.count { (it.endTime - it.startTime) >= it.durationTargetMillis }
        val overtimeCount = completedFasts.count { 
            (it.endTime - it.startTime) >= (it.durationTargetMillis + (2L * 3600 * 1000)) 
        }

        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

        // Time of day checks
        val morningFinishes = completedFasts.count { fast ->
            val cal = Calendar.getInstance().apply { timeInMillis = fast.endTime }
            val hour = cal.get(Calendar.HOUR_OF_DAY)
            hour in 5..10
        }

        val nightStarts = completedFasts.count { fast ->
            val cal = Calendar.getInstance().apply { timeInMillis = fast.startTime }
            val hour = cal.get(Calendar.HOUR_OF_DAY)
            hour >= 20 || hour <= 2
        }

        val weekendFastsCount = completedFasts.count { fast ->
            val cal = Calendar.getInstance().apply { timeInMillis = fast.endTime }
            val dow = cal.get(Calendar.DAY_OF_WEEK)
            dow == Calendar.SATURDAY || dow == Calendar.SUNDAY
        }

        // Calculate food metrics
        val totalFoodLogs = foodEntries.size
        val barcodeScans = foodEntries.count { it.barcode != null && it.barcode.isNotBlank() }
        val ukSupermarketItems = foodEntries.count { 
            val n = it.name.lowercase()
            listOf("tesco", "m&s", "marks & spencer", "sainsbury", "asda", "morrisons", "aldi", "lidl", "waitrose", "co-op", "greggs", "pret", "warburtons", "cadbury", "walkers", "hovis", "muller", "innocent", "alpro", "birds eye", "quorn", "heinz", "yorkshire tea", "pg tips")
                .any { store -> n.contains(store) }
        }

        // Food macros and drinks
        val substantialMealCount = foodEntries.count { it.calories >= 250 }
        val drinksLoggedCount = foodEntries.count { it.mealType.equals("Drinks", true) || it.mealType.equals("Drink", true) }

        // Group foods by day to check multi-meal days
        val dayGroupFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val maxMealsInADay = foodEntries.groupBy { dayGroupFormat.format(Date(it.date)) }
            .values.maxOfOrNull { it.size } ?: 0

        // Weight metrics
        val totalWeightLogs = weightEntries.size
        val waistLogsCount = weightEntries.count { it.waistCm != null && it.waistCm > 0f }
        val hasWaistLog = waistLogsCount > 0
        val hasCompleteProfile = totalWeightLogs > 0 && hasWaistLog && (userHeightCm != null && userHeightCm > 0f)

        val list = mutableListOf<DetailedAchievement>()

        // ==========================================
        // --- Category 1: Fasting Mastery (FASTING)
        // ==========================================
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
                unlockedDate = completedFasts.firstOrNull()?.let { dateFormat.format(Date(it.endTime)) },
                tip = "Tap Start Fast on the timer screen and conclude your first fasting window."
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
        list.add(
            DetailedAchievement(
                id = "fast_100",
                title = "Centurion of Fasting",
                description = "Complete 100 fast sessions",
                category = AchievementCategory.FASTING,
                iconEmoji = "🏛️",
                rarity = AchievementRarity.LEGENDARY,
                currentProgress = totalFasts.coerceAtMost(100),
                targetProgress = 100,
                isUnlocked = totalFasts >= 100,
                tip = "100 fasts completed! You belong to the absolute elite of fasting practitioners."
            )
        )
        list.add(
            DetailedAchievement(
                id = "hours_50",
                title = "50-Hour Club",
                description = "Accumulate 50 total hours of fasting",
                category = AchievementCategory.FASTING,
                iconEmoji = "⏳",
                rarity = AchievementRarity.COMMON,
                currentProgress = totalFastingHours.toInt().coerceAtMost(50),
                targetProgress = 50,
                isUnlocked = totalFastingHours >= 50f,
                tip = "Every hour in a fasted state promotes insulin sensitivity and fat oxidation."
            )
        )
        list.add(
            DetailedAchievement(
                id = "hours_100",
                title = "Century of Hours",
                description = "Accumulate 100 total hours of fasting",
                category = AchievementCategory.FASTING,
                iconEmoji = "⌛",
                rarity = AchievementRarity.RARE,
                currentProgress = totalFastingHours.toInt().coerceAtMost(100),
                targetProgress = 100,
                isUnlocked = totalFastingHours >= 100f,
                tip = "100 hours spent rejuvenating your body from the inside out."
            )
        )
        list.add(
            DetailedAchievement(
                id = "hours_500",
                title = "Half-Millennium",
                description = "Accumulate 500 total hours of fasting",
                category = AchievementCategory.FASTING,
                iconEmoji = "🌌",
                rarity = AchievementRarity.EPIC,
                currentProgress = totalFastingHours.toInt().coerceAtMost(500),
                targetProgress = 500,
                isUnlocked = totalFastingHours >= 500f,
                tip = "500 hours fasted represents deep metabolic reprogramming."
            )
        )
        list.add(
            DetailedAchievement(
                id = "hours_1000",
                title = "Millennium Titan",
                description = "Accumulate 1,000 total hours of fasting",
                category = AchievementCategory.FASTING,
                iconEmoji = "🪐",
                rarity = AchievementRarity.LEGENDARY,
                currentProgress = totalFastingHours.toInt().coerceAtMost(1000),
                targetProgress = 1000,
                isUnlocked = totalFastingHours >= 1000f,
                tip = "An incredible milestone: 1,000 hours of dedication to metabolic health!"
            )
        )
        list.add(
            DetailedAchievement(
                id = "goal_crusher_5",
                title = "Target Locked",
                description = "Reach or exceed your fasting target 5 times",
                category = AchievementCategory.FASTING,
                iconEmoji = "🎯",
                rarity = AchievementRarity.COMMON,
                currentProgress = targetReachedCount.coerceAtMost(5),
                targetProgress = 5,
                isUnlocked = targetReachedCount >= 5,
                tip = "Stick to your timer until the finish line to build self-discipline."
            )
        )
        list.add(
            DetailedAchievement(
                id = "goal_crusher_20",
                title = "Goal Crusher",
                description = "Reach or exceed your fasting target 20 times",
                category = AchievementCategory.FASTING,
                iconEmoji = "🏆",
                rarity = AchievementRarity.RARE,
                currentProgress = targetReachedCount.coerceAtMost(20),
                targetProgress = 20,
                isUnlocked = targetReachedCount >= 20,
                tip = "Consistently reaching your fasting targets builds rock-solid consistency."
            )
        )
        list.add(
            DetailedAchievement(
                id = "overtime_hero",
                title = "Overtime Hero",
                description = "Fast 2+ hours past your scheduled goal",
                category = AchievementCategory.FASTING,
                iconEmoji = "🚀",
                rarity = AchievementRarity.RARE,
                currentProgress = overtimeCount.coerceAtMost(1),
                targetProgress = 1,
                isUnlocked = overtimeCount >= 1,
                tip = "When you feel great, extend your fast naturally into deeper fat burn."
            )
        )
        list.add(
            DetailedAchievement(
                id = "early_bird",
                title = "Sunrise Finisher",
                description = "Conclude a fast in the morning (5:00 AM – 10:00 AM)",
                category = AchievementCategory.FASTING,
                iconEmoji = "🌅",
                rarity = AchievementRarity.COMMON,
                currentProgress = morningFinishes.coerceAtMost(1),
                targetProgress = 1,
                isUnlocked = morningFinishes >= 1,
                tip = "Breaking fast in the morning aligns with natural circadian cortisol peaks."
            )
        )
        list.add(
            DetailedAchievement(
                id = "night_owl",
                title = "Twilight Faster",
                description = "Start a fasting session in the evening (after 8:00 PM)",
                category = AchievementCategory.FASTING,
                iconEmoji = "🌙",
                rarity = AchievementRarity.COMMON,
                currentProgress = nightStarts.coerceAtMost(1),
                targetProgress = 1,
                isUnlocked = nightStarts >= 1,
                tip = "Starting after dinner lets you fast effortlessly through your sleep hours."
            )
        )
        list.add(
            DetailedAchievement(
                id = "weekend_faster",
                title = "Weekend Dedicated",
                description = "Complete 3 fast sessions on a Saturday or Sunday",
                category = AchievementCategory.FASTING,
                iconEmoji = "🗓️",
                rarity = AchievementRarity.RARE,
                currentProgress = weekendFastsCount.coerceAtMost(3),
                targetProgress = 3,
                isUnlocked = weekendFastsCount >= 3,
                tip = "Maintaining your fasting schedule on weekends keeps momentum intact."
            )
        )

        // ==========================================
        // --- Category 2: Duration & Stages (DURATION)
        // ==========================================
        list.add(
            DetailedAchievement(
                id = "fast_12h",
                title = "Circadian Starter",
                description = "Complete a gentle 12-hour overnight fast",
                category = AchievementCategory.DURATION,
                iconEmoji = "🌅",
                rarity = AchievementRarity.COMMON,
                currentProgress = if (maxDurationHours >= 12f) 1 else 0,
                targetProgress = 1,
                isUnlocked = maxDurationHours >= 12f,
                tip = "A 12-hour fast resets digestive enzymes and supports gut rest."
            )
        )
        list.add(
            DetailedAchievement(
                id = "fast_14h",
                title = "Metabolic Switch",
                description = "Complete a 14-hour fast to begin glycogen clearance",
                category = AchievementCategory.DURATION,
                iconEmoji = "🔄",
                rarity = AchievementRarity.COMMON,
                currentProgress = if (maxDurationHours >= 14f) 1 else 0,
                targetProgress = 1,
                isUnlocked = maxDurationHours >= 14f,
                tip = "Between 12-14 hours, liver glycogen drops and fat oxidation accelerates."
            )
        )
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
                tip = "The gold standard protocol for daily insulin sensitivity and fat loss."
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
                id = "fast_22h",
                title = "Deep Ketosis",
                description = "Complete a 22-hour deep ketosis fast",
                category = AchievementCategory.DURATION,
                iconEmoji = "🧪",
                rarity = AchievementRarity.RARE,
                currentProgress = if (maxDurationHours >= 22f) 1 else 0,
                targetProgress = 1,
                isUnlocked = maxDurationHours >= 22f,
                tip = "Blood ketones rise significantly, supplying clean fuel to brain mitochondria."
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
        list.add(
            DetailedAchievement(
                id = "fast_60h",
                title = "Prolonged Purifier",
                description = "Complete a 60-hour prolonged cleanse",
                category = AchievementCategory.DURATION,
                iconEmoji = "🌊",
                rarity = AchievementRarity.LEGENDARY,
                currentProgress = if (maxDurationHours >= 60f) 1 else 0,
                targetProgress = 1,
                isUnlocked = maxDurationHours >= 60f,
                tip = "Deep regenerative fast with peak growth hormone and stem cell activity."
            )
        )
        list.add(
            DetailedAchievement(
                id = "fast_72h",
                title = "Immune Reset (72h)",
                description = "Complete a 72-hour deep immune reset",
                category = AchievementCategory.DURATION,
                iconEmoji = "💎",
                rarity = AchievementRarity.LEGENDARY,
                currentProgress = if (maxDurationHours >= 72f) 1 else 0,
                targetProgress = 1,
                isUnlocked = maxDurationHours >= 72f,
                tip = "At 72 hours, white blood cells recycle and hematopoietic stem cells activate."
            )
        )

        // ==========================================
        // --- Category 3: Streaks & Consistency (STREAKS)
        // ==========================================
        list.add(
            DetailedAchievement(
                id = "streak_2",
                title = "Two-Day Ignite",
                description = "Maintain a 2-day fasting streak",
                category = AchievementCategory.STREAKS,
                iconEmoji = "🌱",
                rarity = AchievementRarity.COMMON,
                currentProgress = currentStreakDays.coerceAtMost(2),
                targetProgress = 2,
                isUnlocked = currentStreakDays >= 2 || longestStreakDays >= 2,
                tip = "Two consecutive days of fasting start your streak chain."
            )
        )
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
                isUnlocked = currentStreakDays >= 3 || longestStreakDays >= 3,
                tip = "Complete daily fasting sessions without skipping."
            )
        )
        list.add(
            DetailedAchievement(
                id = "streak_5",
                title = "5-Day Momentum",
                description = "Maintain a 5-day continuous streak",
                category = AchievementCategory.STREAKS,
                iconEmoji = "⚡",
                rarity = AchievementRarity.RARE,
                currentProgress = currentStreakDays.coerceAtMost(5),
                targetProgress = 5,
                isUnlocked = currentStreakDays >= 5 || longestStreakDays >= 5,
                tip = "Five unbroken days builds metabolic flexibility."
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
                isUnlocked = currentStreakDays >= 7 || longestStreakDays >= 7,
                tip = "One whole week of discipline! You've formed a solid routine."
            )
        )
        list.add(
            DetailedAchievement(
                id = "streak_14",
                title = "Fortnight Hero",
                description = "Maintain a 14-day fasting streak",
                category = AchievementCategory.STREAKS,
                iconEmoji = "🛡️",
                rarity = AchievementRarity.EPIC,
                currentProgress = currentStreakDays.coerceAtMost(14),
                targetProgress = 14,
                isUnlocked = currentStreakDays >= 14 || longestStreakDays >= 14,
                tip = "Two uninterrupted weeks of fasting habit mastery."
            )
        )
        list.add(
            DetailedAchievement(
                id = "streak_21",
                title = "21-Day Habit Lock",
                description = "Maintain a 21-day streak to cement the habit",
                category = AchievementCategory.STREAKS,
                iconEmoji = "🧠",
                rarity = AchievementRarity.EPIC,
                currentProgress = currentStreakDays.coerceAtMost(21),
                targetProgress = 21,
                isUnlocked = currentStreakDays >= 21 || longestStreakDays >= 21,
                tip = "Psychological research shows 21 days solidifies a habit permanently."
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
                isUnlocked = currentStreakDays >= 30 || longestStreakDays >= 30,
                tip = "30 consecutive days. You are an unstoppable force of consistency!"
            )
        )
        list.add(
            DetailedAchievement(
                id = "streak_60",
                title = "Iron Will (60 Days)",
                description = "Maintain a 60-day unbroken streak",
                category = AchievementCategory.STREAKS,
                iconEmoji = "⚡",
                rarity = AchievementRarity.LEGENDARY,
                currentProgress = currentStreakDays.coerceAtMost(60),
                targetProgress = 60,
                isUnlocked = currentStreakDays >= 60 || longestStreakDays >= 60,
                tip = "Two full months of unbroken fasting devotion."
            )
        )
        list.add(
            DetailedAchievement(
                id = "streak_100",
                title = "Centennial Flame (100 Days)",
                description = "Maintain a 100-day uninterrupted streak",
                category = AchievementCategory.STREAKS,
                iconEmoji = "👑",
                rarity = AchievementRarity.LEGENDARY,
                currentProgress = currentStreakDays.coerceAtMost(100),
                targetProgress = 100,
                isUnlocked = currentStreakDays >= 100 || longestStreakDays >= 100,
                tip = "100 unbroken days. The pinnacle of discipline and longevity mastery."
            )
        )

        // ==========================================
        // --- Category 4: Nutrition & Food Tracking (NUTRITION)
        // ==========================================
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
                rarity = AchievementRarity.COMMON,
                currentProgress = totalFoodLogs.coerceAtMost(10),
                targetProgress = 10,
                isUnlocked = totalFoodLogs >= 10,
                tip = "Consistently tracking your nutrition accelerates progress."
            )
        )
        list.add(
            DetailedAchievement(
                id = "food_25_items",
                title = "Nutrition Enthusiast",
                description = "Log 25 food items total",
                category = AchievementCategory.NUTRITION,
                iconEmoji = "🍽️",
                rarity = AchievementRarity.RARE,
                currentProgress = totalFoodLogs.coerceAtMost(25),
                targetProgress = 25,
                isUnlocked = totalFoodLogs >= 25,
                tip = "Awareness of calorie intake and macro balance is the key to energy."
            )
        )
        list.add(
            DetailedAchievement(
                id = "food_50_items",
                title = "Calorie Counter Pro",
                description = "Log 50 food items total",
                category = AchievementCategory.NUTRITION,
                iconEmoji = "📊",
                rarity = AchievementRarity.EPIC,
                currentProgress = totalFoodLogs.coerceAtMost(50),
                targetProgress = 50,
                isUnlocked = totalFoodLogs >= 50,
                tip = "50 meals logged! You have developed strong nutritional intuition."
            )
        )
        list.add(
            DetailedAchievement(
                id = "food_100_items",
                title = "Master of Nutrition",
                description = "Log 100 food items in your journal",
                category = AchievementCategory.NUTRITION,
                iconEmoji = "🌟",
                rarity = AchievementRarity.LEGENDARY,
                currentProgress = totalFoodLogs.coerceAtMost(100),
                targetProgress = 100,
                isUnlocked = totalFoodLogs >= 100,
                tip = "100 food entries logged! Precision fueling at its finest."
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
                id = "food_5_barcodes",
                title = "Scan Enthusiast",
                description = "Scan and log 5 products via barcode scanner",
                category = AchievementCategory.NUTRITION,
                iconEmoji = "🔍",
                rarity = AchievementRarity.RARE,
                currentProgress = barcodeScans.coerceAtMost(5),
                targetProgress = 5,
                isUnlocked = barcodeScans >= 5,
                tip = "Barcode scanning gives you verified UK and global food database macros."
            )
        )
        list.add(
            DetailedAchievement(
                id = "food_15_barcodes",
                title = "Scan Master",
                description = "Scan and log 15 products via barcode",
                category = AchievementCategory.NUTRITION,
                iconEmoji = "🏷️",
                rarity = AchievementRarity.EPIC,
                currentProgress = barcodeScans.coerceAtMost(15),
                targetProgress = 15,
                isUnlocked = barcodeScans >= 15,
                tip = "Mastering package scanning makes food logging effortless."
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
        list.add(
            DetailedAchievement(
                id = "food_uk_supermarket_15",
                title = "British Supermarket Savvy",
                description = "Log 15 UK supermarket branded items",
                category = AchievementCategory.NUTRITION,
                iconEmoji = "🛒",
                rarity = AchievementRarity.EPIC,
                currentProgress = ukSupermarketItems.coerceAtMost(15),
                targetProgress = 15,
                isUnlocked = ukSupermarketItems >= 15,
                tip = "Enjoying accurate nutritional profiles from top UK retailers."
            )
        )
        list.add(
            DetailedAchievement(
                id = "food_macro_logged",
                title = "Satisfying Fuel",
                description = "Log 5 nutritious meals (250+ kcal) to fuel your window",
                category = AchievementCategory.NUTRITION,
                iconEmoji = "🥩",
                rarity = AchievementRarity.COMMON,
                currentProgress = substantialMealCount.coerceAtMost(5),
                targetProgress = 5,
                isUnlocked = substantialMealCount >= 5,
                tip = "Nutritious, substantial meals help you stay full and energised during your fast."
            )
        )
        list.add(
            DetailedAchievement(
                id = "food_hydration_log",
                title = "Hydration Aware",
                description = "Log 3 drinks or water entries in your journal",
                category = AchievementCategory.NUTRITION,
                iconEmoji = "💧",
                rarity = AchievementRarity.COMMON,
                currentProgress = drinksLoggedCount.coerceAtMost(3),
                targetProgress = 3,
                isUnlocked = drinksLoggedCount >= 3,
                tip = "Staying well hydrated with water and electrolytes is vital while fasting."
            )
        )

        // ==========================================
        // --- Category 5: Body & Metrics (BODY)
        // ==========================================
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
                id = "weight_15",
                title = "Transformation Tracker",
                description = "Log your weight 15 times",
                category = AchievementCategory.BODY,
                iconEmoji = "📉",
                rarity = AchievementRarity.EPIC,
                currentProgress = totalWeightLogs.coerceAtMost(15),
                targetProgress = 15,
                isUnlocked = totalWeightLogs >= 15,
                tip = "Consistency in tracking uncovers true body composition changes."
            )
        )
        list.add(
            DetailedAchievement(
                id = "weight_30",
                title = "Weight Master",
                description = "Log your weight 30 times",
                category = AchievementCategory.BODY,
                iconEmoji = "🏅",
                rarity = AchievementRarity.LEGENDARY,
                currentProgress = totalWeightLogs.coerceAtMost(30),
                targetProgress = 30,
                isUnlocked = totalWeightLogs >= 30,
                tip = "30 weigh-ins! You have a complete, high-fidelity trendline of your body."
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
        list.add(
            DetailedAchievement(
                id = "waist_3_logs",
                title = "Visceral Tracker",
                description = "Record waist measurements 3 times",
                category = AchievementCategory.BODY,
                iconEmoji = "📏",
                rarity = AchievementRarity.RARE,
                currentProgress = waistLogsCount.coerceAtMost(3),
                targetProgress = 3,
                isUnlocked = waistLogsCount >= 3,
                tip = "Tracking waist changes over time shows visceral fat loss before scale changes."
            )
        )
        list.add(
            DetailedAchievement(
                id = "full_body_checkin",
                title = "Complete Metrics",
                description = "Log weight, waist, and height in your profile",
                category = AchievementCategory.BODY,
                iconEmoji = "📋",
                rarity = AchievementRarity.COMMON,
                currentProgress = if (hasCompleteProfile) 1 else 0,
                targetProgress = 1,
                isUnlocked = hasCompleteProfile,
                tip = "Complete biometric inputs enable precise BMI, BMR, and body fat calculations."
            )
        )
        list.add(
            DetailedAchievement(
                id = "goal_weight_set",
                title = "Visionary",
                description = "Set your personal target weight goal in settings",
                category = AchievementCategory.BODY,
                iconEmoji = "🎯",
                rarity = AchievementRarity.COMMON,
                currentProgress = if (hasTargetWeightSet) 1 else 0,
                targetProgress = 1,
                isUnlocked = hasTargetWeightSet,
                tip = "Setting a clear target anchor gives your fasting journey direction."
            )
        )

        return list
    }
}
