package com.example.util

import com.example.data.repository.WeightUnit
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

enum class WeightTrajectory {
    WEIGHT_LOSS,
    WEIGHT_GAIN,
    MAINTENANCE
}

data class WeeklyWeightProjection(
    val bmr: Int,
    val tdee: Int,
    val dailyBudget: Int,
    val dailyDeficit: Int, // positive = deficit (burning more than eating), negative = surplus
    val weeklyKcalBalance: Int,
    val weeklyWeightChangeLbs: Float, // positive = losing weight, negative = gaining weight
    val weeklyWeightChangeKg: Float,
    val waistToHeightRatio: Float?,
    val trajectory: WeightTrajectory,
    val summaryText: String,
    val formattedRate: String,
    val explanationText: String
)

object CalorieWeightCalculator {
    const val KCAL_PER_POUND_FAT = 3500f
    const val KCAL_PER_KG_FAT = 7700f

    /**
     * Calculates the Mifflin-St Jeor Basal Metabolic Rate (BMR).
     */
    fun calculateBmr(
        weightKg: Float,
        heightCm: Float,
        age: Int = 30,
        gender: String = "Male"
    ): Int {
        val safeWeight = if (weightKg > 0f) weightKg else 70f
        val safeHeight = if (heightCm > 0f) heightCm else 170f
        val safeAge = if (age in 10..120) age else 30

        val base = (10f * safeWeight) + (6.25f * safeHeight) - (5f * safeAge)
        val genderOffset = if (gender.equals("Male", ignoreCase = true)) 5f else -161f
        return (base + genderOffset).roundToInt().coerceAtLeast(800)
    }

    /**
     * Calculates Total Daily Energy Expenditure (TDEE) factoring in BMR,
     * baseline activity (1.2x sedentary), and waist circumference telemetry if available.
     */
    fun calculateTdee(
        weightKg: Float,
        heightCm: Float,
        waistCm: Float? = null,
        age: Int = 30,
        gender: String = "Male"
    ): Int {
        val bmr = calculateBmr(weightKg, heightCm, age, gender)
        val baselineTdee = bmr * 1.2f

        val waistFactor = if (waistCm != null && waistCm > 0f && heightCm > 0f) {
            val whtr = waistCm / heightCm
            when {
                whtr >= 0.58f -> 1.04f
                whtr >= 0.50f -> 1.02f
                whtr <= 0.40f -> 0.98f
                else -> 1.0f
            }
        } else {
            1.0f
        }

        return (baselineTdee * waistFactor).roundToInt()
    }

    /**
     * Projects weekly weight loss or gain based on daily calorie budget vs estimated TDEE.
     */
    fun calculateWeeklyProjection(
        dailyBudget: Int,
        weightKg: Float,
        heightCm: Float,
        waistCm: Float? = null,
        age: Int = 30,
        gender: String = "Male",
        unit: WeightUnit = WeightUnit.KG
    ): WeeklyWeightProjection {
        val bmr = calculateBmr(weightKg, heightCm, age, gender)
        val tdee = calculateTdee(weightKg, heightCm, waistCm, age, gender)
        val safeBudget = if (dailyBudget > 0) dailyBudget else bmr

        val dailyDeficit = tdee - safeBudget
        val weeklyKcalBalance = dailyDeficit * 7

        val weeklyWeightChangeLbs = weeklyKcalBalance / KCAL_PER_POUND_FAT
        val weeklyWeightChangeKg = weeklyKcalBalance / KCAL_PER_KG_FAT

        val whtr = if (waistCm != null && waistCm > 0f && heightCm > 0f) waistCm / heightCm else null

        val trajectory = when {
            weeklyWeightChangeLbs >= 0.08f -> WeightTrajectory.WEIGHT_LOSS
            weeklyWeightChangeLbs <= -0.08f -> WeightTrajectory.WEIGHT_GAIN
            else -> WeightTrajectory.MAINTENANCE
        }

        val absLbs = abs(weeklyWeightChangeLbs)
        val absKg = abs(weeklyWeightChangeKg)

        val formattedRate = when (unit) {
            WeightUnit.KG -> String.format(Locale.getDefault(), "%.2f kg/wk", absKg)
            WeightUnit.LBS -> String.format(Locale.getDefault(), "%.2f lbs/wk", absLbs)
            WeightUnit.STONE_LBS -> {
                val st = (absLbs / 14f).toInt()
                val remLbs = absLbs % 14f
                if (st > 0) {
                    String.format(Locale.getDefault(), "%d st %.1f lbs/wk", st, remLbs)
                } else {
                    String.format(Locale.getDefault(), "%.2f lbs/wk", remLbs)
                }
            }
        }

        val summaryText = when (trajectory) {
            WeightTrajectory.WEIGHT_LOSS -> "Losing ~$formattedRate"
            WeightTrajectory.WEIGHT_GAIN -> "Gaining ~$formattedRate"
            WeightTrajectory.MAINTENANCE -> "Weight Stable (Maintenance)"
        }

        val explanationText = when (trajectory) {
            WeightTrajectory.WEIGHT_LOSS -> {
                "At $safeBudget kcal/day, your daily deficit is ~$dailyDeficit kcal below maintenance (~$tdee kcal/day). You are projected to burn ~$weeklyKcalBalance kcal weekly fat stores (~$formattedRate)."
            }
            WeightTrajectory.WEIGHT_GAIN -> {
                val surplus = abs(dailyDeficit)
                "At $safeBudget kcal/day, you are consuming a daily surplus of ~$surplus kcal above maintenance (~$tdee kcal/day), projected to gain ~$formattedRate."
            }
            WeightTrajectory.MAINTENANCE -> {
                "At $safeBudget kcal/day, your intake matches your estimated maintenance requirement (~$tdee kcal/day), keeping your weight stable."
            }
        }

        return WeeklyWeightProjection(
            bmr = bmr,
            tdee = tdee,
            dailyBudget = safeBudget,
            dailyDeficit = dailyDeficit,
            weeklyKcalBalance = weeklyKcalBalance,
            weeklyWeightChangeLbs = weeklyWeightChangeLbs,
            weeklyWeightChangeKg = weeklyWeightChangeKg,
            waistToHeightRatio = whtr,
            trajectory = trajectory,
            summaryText = summaryText,
            formattedRate = formattedRate,
            explanationText = explanationText
        )
    }
}
