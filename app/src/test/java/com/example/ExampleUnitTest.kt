package com.example

import com.example.data.repository.WeightUnit
import com.example.util.WeightUtils
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun testWeightFormattingKg() {
        val formatted = WeightUtils.formatWeight(70.5f, WeightUnit.KG)
        assertEquals("70.5 kg", formatted)
    }

    @Test
    fun testWeightFormattingLbs() {
        val formatted = WeightUtils.formatWeight(70f, WeightUnit.LBS)
        assertTrue(formatted.contains("lbs"))
    }

    @Test
    fun testWeightFormattingStoneLbs() {
        // 70 kg ~ 11 stone 0.3 lbs
        val formatted = WeightUtils.formatWeight(70f, WeightUnit.STONE_LBS)
        assertTrue(formatted.contains("st"))
        assertTrue(formatted.contains("lbs"))
    }

    @Test
    fun testWeightParsingStoneLbs() {
        // 10 st 0 lbs = 140 lbs ~ 63.5 kg
        val kg = WeightUtils.parseToKg("10", "0", WeightUnit.STONE_LBS)
        assertNotNull(kg)
        assertEquals(63.5f, kg!!, 0.5f)
    }

    @Test
    fun testWeightParsingLbs() {
        // 154 lbs ~ 70 kg
        val kg = WeightUtils.parseToKg("154", "", WeightUnit.LBS)
        assertNotNull(kg)
        assertEquals(70f, kg!!, 0.5f)
    }
}
