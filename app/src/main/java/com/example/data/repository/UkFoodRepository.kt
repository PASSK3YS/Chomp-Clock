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

        // 1. Search local curated UK database (Instant, high accuracy for UK staples, cereals, dinners, snacks)
        val localMatches = UkFoodCatalog.items.filter { item ->
            val matchesFilter = when (supermarketFilter) {
                "All" -> true
                "Tesco" -> item.supermarketOrBrand.equals("Tesco", ignoreCase = true)
                "Sainsbury's" -> item.supermarketOrBrand.equals("Sainsbury's", ignoreCase = true)
                "ASDA" -> item.supermarketOrBrand.equals("ASDA", ignoreCase = true)
                "M&S" -> item.supermarketOrBrand.equals("M&S", ignoreCase = true) || item.supermarketOrBrand.contains("Marks", ignoreCase = true)
                "Morrisons" -> item.supermarketOrBrand.equals("Morrisons", ignoreCase = true)
                "Aldi/Lidl" -> item.supermarketOrBrand.equals("Aldi", ignoreCase = true) || item.supermarketOrBrand.equals("Lidl", ignoreCase = true)
                "Cereals" -> item.category.contains("Cereal", ignoreCase = true) || item.category.contains("Breakfast", ignoreCase = true)
                "Dinner Combos" -> item.category.contains("Dinner", ignoreCase = true) || item.category.contains("Ready Meals", ignoreCase = true)
                "Snacks & Drinks" -> item.category.contains("Snack", ignoreCase = true) || item.category.contains("Crisps", ignoreCase = true) || item.category.contains("Drink", ignoreCase = true) || item.category.contains("Confectionery", ignoreCase = true)
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

        // 2. Query Open Food Facts UK API if user typed at least 2 characters
        if (trimmed.length >= 2) {
            try {
                val searchQuery = if (supermarketFilter != "All" && !supermarketFilter.contains("Combos") && !supermarketFilter.contains("Snacks") && supermarketFilter != "UK Brands") {
                    "$trimmed $supermarketFilter"
                } else {
                    trimmed
                }
                val response = api.searchProducts(searchTerms = searchQuery, pageSize = 25)
                val apiItems = response.products?.mapNotNull { prod ->
                    val rawName = prod.productName ?: prod.productNameEn ?: return@mapNotNull null
                    if (rawName.isBlank()) return@mapNotNull null

                    val brand = prod.brands?.split(",")?.firstOrNull()?.trim()
                        ?: prod.brandsTags?.firstOrNull()?.replace("-", " ")?.replaceFirstChar { it.uppercase() }
                        ?: "UK Food"

                    val formattedName = formatProductName(brand, rawName)

                    val cal100g = extractCalories100g(prod.nutriments)
                    val serving = prod.servingSize?.trim()?.takeIf { it.isNotBlank() } ?: "1 serving (100g)"
                    val calServing = extractCaloriesServing(prod.nutriments, serving, prod.servingQuantity, cal100g)

                    val prot = (prod.nutriments?.proteins100g ?: 0.0).toFloat()
                    val carbs = (prod.nutriments?.carbohydrates100g ?: 0.0).toFloat()
                    val fat = (prod.nutriments?.fat100g ?: 0.0).toFloat()

                    // Avoid duplicate barcodes or names
                    if (results.any { it.name.equals(formattedName, ignoreCase = true) || (it.barcode != null && it.barcode == prod.code) }) {
                        null
                    } else {
                        FoodSearchResult(
                            id = prod.code ?: "api_${formattedName.hashCode()}",
                            name = formattedName,
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
                            imageUrl = prod.imageThumbUrl ?: prod.imageFrontUrl ?: prod.imageUrl
                        )
                    }
                } ?: emptyList()
                results.addAll(apiItems)
            } catch (e: Exception) {
                // Keep local catalog results intact if offline
            }
        }

        results
    }

    suspend fun lookupBarcode(barcode: String): FoodSearchResult? = withContext(Dispatchers.IO) {
        val trimmedCode = barcode.trim()
        if (trimmedCode.isBlank()) return@withContext null

        // 1. Check local catalog first (instant, exact UK nutritional values)
        val local = UkFoodCatalog.items.firstOrNull { 
            it.barcode == trimmedCode || 
            (trimmedCode.length == 12 && it.barcode == "0$trimmedCode") ||
            (trimmedCode.startsWith("0") && it.barcode == trimmedCode.drop(1))
        }
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

        // 2. Query Open Food Facts API (try original barcode and zero-padded variations)
        val codesToTry = mutableListOf(trimmedCode)
        if (trimmedCode.length == 12) codesToTry.add("0$trimmedCode")
        if (trimmedCode.startsWith("0") && trimmedCode.length == 13) codesToTry.add(trimmedCode.drop(1))

        for (code in codesToTry) {
            // First attempt: Typed v0
            try {
                val response = api.getProductV0(code)
                val prod = response.product
                if (prod != null) {
                    val result = mapProductToResult(code, prod)
                    if (result != null) return@withContext result
                }
            } catch (e: Exception) {
                // Ignore and try raw parsing
            }

            // Second attempt: Raw JSON via v0 endpoint parsed with JSONObject (handles any typing quirks)
            try {
                val rawBody = api.getProductRaw(code).string()
                val json = org.json.JSONObject(rawBody)
                val status = json.optInt("status", -1)
                if (status == 1 && json.has("product")) {
                    val pJson = json.getJSONObject("product")
                    val rawName = listOfNotNull(
                        pJson.optString("product_name").takeIf { it.isNotBlank() },
                        pJson.optString("product_name_en").takeIf { it.isNotBlank() },
                        pJson.optString("generic_name").takeIf { it.isNotBlank() },
                        pJson.optString("generic_name_en").takeIf { it.isNotBlank() },
                        pJson.optString("abbreviated_product_name").takeIf { it.isNotBlank() }
                    ).firstOrNull()

                    if (rawName != null) {
                        val brand = pJson.optString("brands").split(",").firstOrNull()?.trim()
                            ?.takeIf { it.isNotBlank() } ?: "UK Food"
                        val formattedName = formatProductName(brand, rawName)

                        val nutrimentsJson = pJson.optJSONObject("nutriments")
                        var cal100g = 150
                        var calServing: Int? = null

                        if (nutrimentsJson != null) {
                            val directKcal100 = nutrimentsJson.optDouble("energy-kcal_100g", Double.NaN)
                            val altKcal100 = nutrimentsJson.optDouble("energy-kcal", Double.NaN)
                            val kj100 = nutrimentsJson.optDouble("energy_100g", Double.NaN)

                            cal100g = when {
                                !directKcal100.isNaN() && directKcal100 > 0 -> directKcal100.toInt()
                                !altKcal100.isNaN() && altKcal100 > 0 -> altKcal100.toInt()
                                !kj100.isNaN() && kj100 > 0 -> (kj100 / 4.184).toInt()
                                else -> 150
                            }

                            val directKcalServ = nutrimentsJson.optDouble("energy-kcal_serving", Double.NaN)
                            val kjServ = nutrimentsJson.optDouble("energy_serving", Double.NaN)
                            if (!directKcalServ.isNaN() && directKcalServ > 0) {
                                calServing = directKcalServ.toInt()
                            } else if (!kjServ.isNaN() && kjServ > 0) {
                                calServing = (kjServ / 4.184).toInt()
                            }
                        }

                        val servingText = pJson.optString("serving_size").trim().takeIf { it.isNotBlank() } ?: "1 serving (100g)"
                        val finalServingCal = calServing ?: extractCaloriesServing(null, servingText, pJson.optDouble("serving_quantity", Double.NaN).takeIf { !it.isNaN() }, cal100g)

                        val prot = nutrimentsJson?.optDouble("proteins_100g", 0.0)?.toFloat() ?: 0f
                        val carbs = nutrimentsJson?.optDouble("carbohydrates_100g", 0.0)?.toFloat() ?: 0f
                        val fat = nutrimentsJson?.optDouble("fat_100g", 0.0)?.toFloat() ?: 0f
                        val category = pJson.optString("categories").split(",").firstOrNull()?.trim()?.takeIf { it.isNotBlank() } ?: "Scanned Product"
                        val img = pJson.optString("image_front_thumb_url").takeIf { it.isNotBlank() }
                            ?: pJson.optString("image_url").takeIf { it.isNotBlank() }

                        return@withContext FoodSearchResult(
                            id = code,
                            name = formattedName,
                            brandOrSupermarket = brand,
                            category = category,
                            caloriesPerServing = maxOf(0, finalServingCal),
                            servingSize = servingText,
                            caloriesPer100g = maxOf(0, cal100g),
                            proteinGrams = maxOf(0f, prot),
                            carbsGrams = maxOf(0f, carbs),
                            fatGrams = maxOf(0f, fat),
                            barcode = code,
                            isUkSupermarket = isUkSupermarketBrand(brand),
                            imageUrl = img
                        )
                    }
                }
            } catch (e: Exception) {
                // Continue
            }
        }

        null
    }

    private fun mapProductToResult(code: String, prod: com.example.data.remote.Product): FoodSearchResult? {
        val brand = prod.brands?.split(",")?.firstOrNull()?.trim()
            ?: prod.brandsTags?.firstOrNull()?.replace("-", " ")?.replaceFirstChar { it.uppercase() }
            ?: "UK Food"

        val rawName = listOfNotNull(
            prod.productName?.takeIf { it.isNotBlank() },
            prod.productNameEn?.takeIf { it.isNotBlank() },
            prod.genericName?.takeIf { it.isNotBlank() },
            prod.genericNameEn?.takeIf { it.isNotBlank() },
            prod.abbreviatedProductName?.takeIf { it.isNotBlank() }
        ).firstOrNull() ?: return null

        val formattedName = formatProductName(brand, rawName)

        val cal100g = extractCalories100g(prod.nutriments)
        val servingText = prod.servingSize?.trim()?.takeIf { it.isNotBlank() } ?: "1 serving (100g)"
        val calServing = extractCaloriesServing(prod.nutriments, servingText, prod.servingQuantity, cal100g)

        val gramsInServing = prod.servingQuantity
            ?: Regex("""(\d+(?:\.\d+)?)\s*(?:g|ml|grams|millilitres|g\b|ml\b)""", RegexOption.IGNORE_CASE)
                .find(servingText)?.groupValues?.get(1)?.toDoubleOrNull()

        val prot = (prod.nutriments?.proteinsServing
            ?: (if (gramsInServing != null) (prod.nutriments?.proteins100g ?: 0.0) * gramsInServing / 100.0 else prod.nutriments?.proteins100g)
            ?: 0.0).toFloat()

        val carbs = (prod.nutriments?.carbohydratesServing
            ?: (if (gramsInServing != null) (prod.nutriments?.carbohydrates100g ?: 0.0) * gramsInServing / 100.0 else prod.nutriments?.carbohydrates100g)
            ?: 0.0).toFloat()

        val fat = (prod.nutriments?.fatServing
            ?: (if (gramsInServing != null) (prod.nutriments?.fat100g ?: 0.0) * gramsInServing / 100.0 else prod.nutriments?.fat100g)
            ?: 0.0).toFloat()

        val category = prod.genericName?.takeIf { it.isNotBlank() }
            ?: prod.categories?.split(",")?.firstOrNull()?.trim()
            ?: "Scanned Product"

        return FoodSearchResult(
            id = prod.code ?: code,
            name = formattedName,
            brandOrSupermarket = brand,
            category = category,
            caloriesPerServing = maxOf(0, calServing),
            servingSize = servingText,
            caloriesPer100g = maxOf(0, cal100g),
            proteinGrams = maxOf(0f, prot),
            carbsGrams = maxOf(0f, carbs),
            fatGrams = maxOf(0f, fat),
            barcode = code,
            isUkSupermarket = isUkSupermarketBrand(brand),
            imageUrl = prod.imageThumbUrl ?: prod.imageFrontUrl ?: prod.imageUrl
        )
    }

    private fun extractCalories100g(nutriments: com.example.data.remote.Nutriments?): Int {
        if (nutriments == null) return 150

        // 1. Direct kcal per 100g
        nutriments.energyKcal100g?.let { if (it > 0) return it.toInt() }
        nutriments.energyKcal?.let { if (it > 0) return it.toInt() }
        nutriments.energyKcalValue?.let { if (it > 0) return it.toInt() }

        // 2. Direct energy in kJ converted to kcal (1 kcal = 4.184 kJ)
        nutriments.energy100g?.let { if (it > 0) return (it / 4.184).toInt() }
        nutriments.energy?.let { if (it > 0) return (it / 4.184).toInt() }

        return 150
    }

    private fun extractCaloriesServing(
        nutriments: com.example.data.remote.Nutriments?,
        servingText: String,
        servingQuantity: Double?,
        cal100g: Int
    ): Int {
        if (nutriments?.energyKcalServing != null && nutriments.energyKcalServing > 0) {
            return nutriments.energyKcalServing.toInt()
        }
        if (nutriments?.energyServing != null && nutriments.energyServing > 0) {
            return (nutriments.energyServing / 4.184).toInt()
        }

        // Try extracting grams or ml from serving text
        val grams = servingQuantity ?: Regex("""(\d+(?:\.\d+)?)\s*(?:g|ml|grams|millilitres|g\b|ml\b)""", RegexOption.IGNORE_CASE)
            .find(servingText)?.groupValues?.get(1)?.toDoubleOrNull()

        if (grams != null && grams > 0) {
            return kotlin.math.round(cal100g * (grams / 100.0)).toInt()
        }

        return cal100g
    }

    private fun formatProductName(brand: String, rawName: String): String {
        val cleanName = rawName.trim()
        val cleanBrand = brand.trim()

        if (cleanBrand.isBlank() || cleanBrand.equals("UK Food", ignoreCase = true) || cleanBrand.equals("UK Product", ignoreCase = true)) {
            return cleanName
        }

        // Avoid repeated brand e.g. "Tesco Tesco British Milk" -> "Tesco British Milk"
        if (cleanName.startsWith(cleanBrand, ignoreCase = true)) {
            return cleanName
        }

        return "$cleanBrand $cleanName"
    }

    private fun isUkSupermarketBrand(brand: String): Boolean {
        val ukStores = listOf(
            "Tesco", "Sainsbury's", "ASDA", "M&S", "Marks & Spencer", "Morrisons",
            "Aldi", "Lidl", "Waitrose", "Co-op", "Iceland", "Greggs", "Pret",
            "Heinz", "Warburtons", "Cadbury", "Walkers", "Weetabix", "Kellogg's",
            "Nestlé", "Yorkshire Tea", "Alpro", "Innocent", "Lucozade", "Vimto", "Irn-Bru"
        )
        return ukStores.any { brand.contains(it, ignoreCase = true) }
    }
}
