package com.example.util

import com.example.data.repository.HeightUnit
import com.example.data.repository.WeightUnit
import java.util.Locale
import kotlin.math.roundToInt

object WeightUtils {
    const val LBS_PER_KG = 2.2046226218f
    const val LBS_PER_STONE = 14f
    const val CM_PER_INCH = 2.54f

    /**
     * Converts a weight in kilograms to a formatted display string based on [WeightUnit].
     */
    fun formatWeight(weightKg: Float, unit: WeightUnit): String {
        if (weightKg <= 0f) return "0 ${unit.shortName}"
        return when (unit) {
            WeightUnit.KG -> String.format(Locale.getDefault(), "%.1f kg", weightKg)
            WeightUnit.LBS -> {
                val lbs = weightKg * LBS_PER_KG
                String.format(Locale.getDefault(), "%.1f lbs", lbs)
            }
            WeightUnit.STONE_LBS -> {
                val totalLbs = weightKg * LBS_PER_KG
                val stones = (totalLbs / LBS_PER_STONE).toInt()
                val remLbs = totalLbs % LBS_PER_STONE
                if (stones > 0) {
                    String.format(Locale.getDefault(), "%d st %.1f lbs", stones, remLbs)
                } else {
                    String.format(Locale.getDefault(), "%.1f lbs", remLbs)
                }
            }
        }
    }

    /**
     * Converts raw input strings to kilograms based on unit.
     */
    fun parseToKg(
        primaryInput: String,
        secondaryInput: String = "",
        unit: WeightUnit
    ): Float? {
        return when (unit) {
            WeightUnit.KG -> primaryInput.toFloatOrNull()
            WeightUnit.LBS -> {
                val lbs = primaryInput.toFloatOrNull() ?: return null
                lbs / LBS_PER_KG
            }
            WeightUnit.STONE_LBS -> {
                val stones = primaryInput.toFloatOrNull() ?: 0f
                val lbs = secondaryInput.toFloatOrNull() ?: 0f
                if (stones <= 0f && lbs <= 0f) return null
                val totalLbs = (stones * LBS_PER_STONE) + lbs
                totalLbs / LBS_PER_KG
            }
        }
    }

    /**
     * Converts height in centimeters to a Pair of (feet, inches).
     */
    fun cmToFeetAndInches(heightCm: Float): Pair<Int, Int> {
        if (heightCm <= 0f) return Pair(5, 7)
        val totalInches = (heightCm / CM_PER_INCH).roundToInt()
        val feet = totalInches / 12
        val inches = totalInches % 12
        return Pair(feet, inches)
    }

    /**
     * Converts feet and inches to centimeters.
     */
    fun feetAndInchesToCm(feet: Int, inches: Int): Float {
        val totalInches = (feet * 12) + inches
        return totalInches * CM_PER_INCH
    }

    /**
     * Converts height cm to display string based on [HeightUnit] or imperial flag.
     */
    fun formatHeight(heightCm: Float, unit: HeightUnit = HeightUnit.CM): String {
        if (heightCm <= 0f) return "0 cm"
        return when (unit) {
            HeightUnit.CM -> String.format(Locale.getDefault(), "%.0f cm", heightCm)
            HeightUnit.FT_IN -> {
                val (feet, inches) = cmToFeetAndInches(heightCm)
                "$feet' $inches\""
            }
        }
    }

    /**
     * Converts height cm to display string (either cm or ft/in).
     */
    fun formatHeight(heightCm: Float, useImperial: Boolean): String {
        return formatHeight(heightCm, if (useImperial) HeightUnit.FT_IN else HeightUnit.CM)
    }

    /**
     * Formats waist measurement.
     */
    fun formatWaist(waistCm: Float?, useImperial: Boolean): String {
        if (waistCm == null || waistCm <= 0f) return "Not set"
        return if (useImperial) {
            val inches = waistCm / CM_PER_INCH
            String.format(Locale.getDefault(), "%.1f in", inches)
        } else {
            String.format(Locale.getDefault(), "%.1f cm", waistCm)
        }
    }
}
