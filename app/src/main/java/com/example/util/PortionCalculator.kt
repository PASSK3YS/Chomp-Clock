package com.example.util

import java.util.Locale
import kotlin.math.roundToInt

/**
 * Utility for parsing serving sizes, scaling calorie counts proportionally based on
 * grams / weight / serving multipliers, and formatting user-friendly portion labels.
 */
object PortionCalculator {

    /**
     * Extracts numerical gram or millilitre quantity from a serving text string.
     * Examples:
     * - "100g" -> 100f
     * - "100 g" -> 100f
     * - "1 bar (35g)" -> 35f
     * - "2 slices (56 grams)" -> 56f
     * - "330ml" -> 330f
     * - "1/2 can (200g)" -> 200f
     * - "45" -> 45f
     */
    fun parseGrams(servingText: String?, defaultGrams: Float = 100f): Float {
        if (servingText.isNullOrBlank()) return defaultGrams

        val cleanText = servingText.trim().lowercase(Locale.getDefault())

        // 1. Try finding parenthesized weight first: e.g. "1 bar (35g)" or "(120 g)"
        val parenRegex = Regex("""\(\s*(\d+(?:\.\d+)?)\s*(?:g|gram|grams|ml|milliliters?|millilitres?)\s*\)""")
        val parenMatch = parenRegex.find(cleanText)
        if (parenMatch != null) {
            val value = parenMatch.groupValues[1].toFloatOrNull()
            if (value != null && value > 0f) return value
        }

        // 2. Try finding explicit "Xg" or "X g" or "X grams" or "Xml" anywhere in the string
        val weightRegex = Regex("""(\d+(?:\.\d+)?)\s*(?:g|gram|grams|ml|milliliters?|millilitres?)\b""")
        val weightMatch = weightRegex.find(cleanText)
        if (weightMatch != null) {
            val value = weightMatch.groupValues[1].toFloatOrNull()
            if (value != null && value > 0f) return value
        }

        // 3. Try finding any standalone number in the string (e.g. "100")
        val numRegex = Regex("""\b(\d+(?:\.\d+)?)\b""")
        val numMatch = numRegex.find(cleanText)
        if (numMatch != null) {
            val value = numMatch.groupValues[1].toFloatOrNull()
            if (value != null && value > 0f) return value
        }

        return defaultGrams
    }

    /**
     * Calculates total calories by scaling base calories according to eaten grams vs base grams.
     * E.g. base = 250 kcal per 100g, eaten = 200g -> 500 kcal.
     * E.g. base = 250 kcal per 100g, eaten = 50g -> 125 kcal.
     */
    fun calculateScaledCalories(baseCalories: Float, baseGrams: Float, targetGrams: Float): Int {
        if (baseGrams <= 0f || targetGrams <= 0f || baseCalories <= 0f) return 0
        val ratio = targetGrams / baseGrams
        return (baseCalories * ratio).roundToInt()
    }

    /**
     * Calculates total calories from base calories and a serving multiplier.
     * E.g. base = 250 kcal, multiplier = 2.0x -> 500 kcal.
     */
    fun calculateMultiplierCalories(baseCalories: Float, multiplier: Float): Int {
        if (baseCalories <= 0f || multiplier <= 0f) return 0
        return (baseCalories * multiplier).roundToInt()
    }

    /**
     * Formats a clean, readable serving description for the logged food entry.
     * E.g. "200g (2x serving)" or "150g" or "50g (½ serving)"
     */
    fun formatPortionDescription(
        targetGrams: Float,
        baseGrams: Float,
        multiplier: Float? = null,
        baseServingUnit: String = "g"
    ): String {
        val calculatedMultiplier = multiplier ?: if (baseGrams > 0f) targetGrams / baseGrams else 1.0f

        val formattedGrams = if (targetGrams % 1f == 0f) {
            "${targetGrams.toInt()}$baseServingUnit"
        } else {
            String.format(Locale.getDefault(), "%.1f%s", targetGrams, baseServingUnit)
        }

        return when {
            kotlin.math.abs(calculatedMultiplier - 1.0f) < 0.05f -> {
                formattedGrams
            }
            kotlin.math.abs(calculatedMultiplier - 0.5f) < 0.05f -> {
                "$formattedGrams (½ serving)"
            }
            kotlin.math.abs(calculatedMultiplier - 2.0f) < 0.05f -> {
                "$formattedGrams (2x double serving)"
            }
            kotlin.math.abs(calculatedMultiplier - 3.0f) < 0.05f -> {
                "$formattedGrams (3x triple serving)"
            }
            else -> {
                val multStr = if (calculatedMultiplier % 1f == 0f) {
                    "${calculatedMultiplier.toInt()}x"
                } else {
                    String.format(Locale.getDefault(), "%.1fx", calculatedMultiplier)
                }
                "$formattedGrams ($multStr serving)"
            }
        }
    }

    /**
     * Returns a summary calculation string for UI transparency.
     * E.g. "2.5 kcal/g" or "250 kcal per 100g"
     */
    fun formatCalorieDensity(baseCalories: Float, baseGrams: Float): String {
        if (baseGrams <= 0f || baseCalories <= 0f) return "0 kcal/g"
        val density = baseCalories / baseGrams
        return if (density >= 1.0f) {
            String.format(Locale.getDefault(), "%.1f kcal/g", density)
        } else {
            String.format(Locale.getDefault(), "%.2f kcal/g", density)
        }
    }
}
