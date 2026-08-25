package com.example

import com.example.data.local.UkFoodCatalog
import com.example.data.repository.UkFoodRepository
import com.example.data.repository.WeightUnit
import com.example.util.WeightUtils
import kotlinx.coroutines.runBlocking
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

    @Test
    fun testUkFoodCatalogHasSupermarkets() {
        assertTrue(UkFoodCatalog.items.isNotEmpty())
        val supermarkets = UkFoodCatalog.items.map { it.supermarketOrBrand }.toSet()
        assertTrue(supermarkets.contains("Tesco"))
        assertTrue(supermarkets.contains("Sainsbury's"))
        assertTrue(supermarkets.contains("ASDA"))
        assertTrue(supermarkets.contains("M&S"))
        assertTrue(supermarkets.contains("Heinz"))
        assertTrue(supermarkets.contains("Warburtons"))
    }

    @Test
    fun testUkFoodLocalSearch() = runBlocking {
        val repo = UkFoodRepository()
        val tescoResults = repo.searchFood("chicken", "Tesco")
        assertTrue(tescoResults.isNotEmpty())
        assertTrue(tescoResults.all { it.brandOrSupermarket.equals("Tesco", ignoreCase = true) })
    }

    @Test
    fun testUkFoodBarcodeLookupLocal() = runBlocking {
        val repo = UkFoodRepository()
        // Heinz beans barcode: 5000157024671
        val result = repo.lookupBarcode("5000157024671")
        assertNotNull(result)
        assertEquals("Heinz", result?.brandOrSupermarket)
        assertTrue(result?.name?.contains("Heinz") == true)
    }
}
