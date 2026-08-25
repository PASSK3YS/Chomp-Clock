package com.example.data.model

import androidx.compose.ui.graphics.Color

enum class AchievementCategory(val displayName: String, val icon: String) {
    ALL("All", "🌟"),
    FASTING("Fasting", "⏳"),
    DURATION("Duration", "⏱️"),
    STREAKS("Streaks", "🔥"),
    NUTRITION("Nutrition", "🥗"),
    BODY("Body", "⚖️")
}

enum class AchievementRarity(val label: String, val color: Color) {
    COMMON("Common", Color(0xFF94A3B8)),
    RARE("Rare", Color(0xFF60A5FA)),
    EPIC("Epic", Color(0xFFA855F7)),
    LEGENDARY("Legendary", Color(0xFFF59E0B))
}

data class DetailedAchievement(
    val id: String,
    val title: String,
    val description: String,
    val category: AchievementCategory,
    val iconEmoji: String,
    val rarity: AchievementRarity,
    val currentProgress: Int,
    val targetProgress: Int,
    val isUnlocked: Boolean,
    val unlockedDate: String? = null,
    val tip: String = ""
) {
    val progressPercent: Float
        get() = if (targetProgress <= 0) 1f else (currentProgress.toFloat() / targetProgress).coerceIn(0f, 1f)
}
