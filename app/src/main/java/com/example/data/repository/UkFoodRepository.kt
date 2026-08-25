package com.example.data.repository

import com.example.data.local.UkFoodCatalog
import com.example.data.local.UkFoodProduct
import com.example.data.remote.OpenFoodFactsApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class FoodSearchResult(
    val id: String,
    val name: String,
    val brandOrSupermarket: String,
    val category: String,
    val caloriesPerServing: Int,
    val servingSize: String,
    val caloriesPer100g: Int,
    val proteinGrams: Float = 0f,
    val carbsGrams: Float = 0f,
    val fatGrams: Float = 0f,
    val barcode: String? = null,
    val isUkSupermarket: Boolean = true,
    val imageUrl: String? = null
)

class UkFoodRepository(
    private val api: OpenFoodFactsApi = OpenFoodFactsApi.create()
) {
    suspend fun searchFood(query: String, supermarketFilter: String = "All"): List<FoodSearchResult> = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        val results = mutableListOf<FoodSearchResult>()

        // 1. Search local curated UK database (Instant, high accuracy)
        val localMatches = UkFoodCatalog.items.filter { item ->
            val matchesFilter = when (supermarketFilter) {
                "All" -> true
                "Tesco" -> item.supermarketOrBrand.equals("Tesco", ignoreCase = true)
                "Sainsbury's" -> item.supermarketOrBrand.equals("Sainsbury's", ignoreCase = true)
                "ASDA" -> item.supermarketOrBrand.equals("ASDA", ignoreCase = true)
                "M&S" -> item.supermarketOrBrand.equals("M&S", ignoreCase = true)
                "Morrisons" -> item.supermarketOrBrand.equals("Morrisons", ignoreCase = true)
                "Aldi/Lidl" -> item.supermarketOrBrand.equals("Aldi", ignoreCase = true) || item.supermarketOrBrand.equals("Lidl", ignoreCase = true)
                "UK Brands" -> !listOf("Tesco", "Sainsbury's", "ASDA", "M&S", "Morrisons", "Aldi", "Lidl").contains(item.supermarketOrBrand)
                else -> true
            }

            if (!matchesFilter) return@filter false

            if (trimmed.isEmpty()) {
                true
            } else {
                item.name.contains(trimmed, ignoreCase = true) ||
                item.supermarketOrBrand.contains(trimmed, ignoreCase = true) ||
                item.category.contains(trimmed, ignoreCase = true)
            }
        }.map { item ->
            FoodSearchResult(
                id = item.id,
                name = item.name,
                brandOrSupermarket = item.supermarketOrBrand,
                category = item.category,
                caloriesPerServing = item.caloriesPerServing,
                servingSize = item.defaultServing,
                caloriesPer100g = item.caloriesPer100g,
                proteinGrams = item.proteinGrams,
                carbsGrams = item.carbsGrams,
                fatGrams = item.fatGrams,
                barcode = item.barcode,
                isUkSupermarket = true
            )
        }
        results.addAll(localMatches)

        // 2. Query Open Food Facts UK API if user provided at least 2 chars
        if (trimmed.length >= 2) {
            try {
                val searchQuery = if (supermarketFilter != "All" && supermarketFilter != "UK Brands") {
                    "$trimmed $supermarketFilter"
                } else {
                    trimmed
                }
                val response = api.searchProducts(searchTerms = searchQuery, pageSize = 20)
                val apiItems = response.products?.mapNotNull { prod ->
                    val name = prod.productName ?: prod.productNameEn ?: return@mapNotNull null
                    if (name.isBlank()) return@mapNotNull null

                    val brand = prod.brands?.split(",")?.firstOrNull()?.trim() ?: "UK Product"
                    val cal100g = prod.nutriments?.energyKcal100g?.toInt()
                        ?: prod.nutriments?.energyKcal?.toInt()
                        ?: 150
                    val serving = prod.servingSize?.trim() ?: "1 serving (100g)"
                    val calServing = prod.nutriments?.energyKcalServing?.toInt() ?: cal100g

                    val prot = (prod.nutriments?.proteins100g ?: 0.0).toFloat()
                    val carbs = (prod.nutriments?.carbohydrates100g ?: 0.0).toFloat()
                    val fat = (prod.nutriments?.fat100g ?: 0.0).toFloat()

                    // Avoid duplicate barcodes/names
                    if (results.any { it.name.equals(name, ignoreCase = true) || (it.barcode != null && it.barcode == prod.code) }) {
                        null
                    } else {
                        FoodSearchResult(
                            id = prod.code ?: "api_${name.hashCode()}",
                            name = name,
                            brandOrSupermarket = brand,
                            category = prod.genericName ?: "Food item",
                            caloriesPerServing = calServing,
                            servingSize = serving,
                            caloriesPer100g = cal100g,
                            proteinGrams = prot,
                            carbsGrams = carbs,
                            fatGrams = fat,
                            barcode = prod.code,
                            isUkSupermarket = isUkSupermarketBrand(brand),
                            imageUrl = prod.imageThumbUrl ?: prod.imageUrl
                        )
                    }
                } ?: emptyList()
                results.addAll(apiItems)
            } catch (e: Exception) {
                // Online search failed or offline, local database results are still intact
            }
        }

        results
    }

    suspend fun lookupBarcode(barcode: String): FoodSearchResult? = withContext(Dispatchers.IO) {
        val trimmedCode = barcode.trim()

        // 1. Check local catalog first
        val local = UkFoodCatalog.items.firstOrNull { it.barcode == trimmedCode }
        if (local != null) {
            return@withContext FoodSearchResult(
                id = local.id,
                name = local.name,
                brandOrSupermarket = local.supermarketOrBrand,
                category = local.category,
                caloriesPerServing = local.caloriesPerServing,
                servingSize = local.defaultServing,
                caloriesPer100g = local.caloriesPer100g,
                proteinGrams = local.proteinGrams,
                carbsGrams = local.carbsGrams,
                fatGrams = local.fatGrams,
                barcode = local.barcode,
                isUkSupermarket = true
            )
        }

        // 2. Query Open Food Facts API for the barcode
        try {
            val response = api.getProduct(trimmedCode)
            if (response.status == 1 && response.product != null) {
                val prod = response.product
                val name = prod.productName ?: prod.productNameEn ?: "Product #$trimmedCode"
                val brand = prod.brands?.split(",")?.firstOrNull()?.trim() ?: "UK Brand"
                val cal100g = prod.nutriments?.energyKcal100g?.toInt()
                    ?: prod.nutriments?.energyKcal?.toInt()
                    ?: 180
                val serving = prod.servingSize?.trim() ?: "1 serving (100g)"
                val calServing = prod.nutriments?.energyKcalServing?.toInt() ?: cal100g

                val prot = (prod.nutriments?.proteins100g ?: 0.0).toFloat()
                val carbs = (prod.nutriments?.carbohydrates100g ?: 0.0).toFloat()
                val fat = (prod.nutriments?.fat100g ?: 0.0).toFloat()

                return@withContext FoodSearchResult(
                    id = prod.code ?: trimmedCode,
                    name = name,
                    brandOrSupermarket = brand,
                    category = prod.genericName ?: "Scanned Product",
                    caloriesPerServing = calServing,
                    servingSize = serving,
                    caloriesPer100g = cal100g,
                    proteinGrams = prot,
                    carbsGrams = carbs,
                    fatGrams = fat,
                    barcode = trimmedCode,
                    isUkSupermarket = isUkSupermarketBrand(brand),
                    imageUrl = prod.imageThumbUrl ?: prod.imageUrl
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        null
    }

    private fun isUkSupermarketBrand(brand: String): Boolean {
        val ukStores = listOf("Tesco", "Sainsbury's", "ASDA", "M&S", "Marks & Spencer", "Morrisons", "Aldi", "Lidl", "Waitrose", "Co-op", "Iceland", "Greggs", "Pret", "Heinz", "Warburtons", "Cadbury", "Walkers")
        return ukStores.any { brand.contains(it, ignoreCase = true) }
    }
}
