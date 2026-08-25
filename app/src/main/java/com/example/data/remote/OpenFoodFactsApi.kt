package com.example.data.remote

import com.squareup.moshi.Json
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Path
import retrofit2.http.Query

interface OpenFoodFactsApi {
    @Headers("User-Agent: ChompClock - Android - Version 1.0")
    @GET("api/v2/product/{barcode}.json")
    suspend fun getProduct(@Path("barcode") barcode: String): ProductResponse

    @Headers("User-Agent: ChompClock - Android - Version 1.0")
    @GET("cgi/search.pl")
    suspend fun searchProducts(
        @Query("search_terms") searchTerms: String,
        @Query("search_simple") searchSimple: Int = 1,
        @Query("action") action: String = "process",
        @Query("json") json: Int = 1,
        @Query("page_size") pageSize: Int = 25,
        @Query("cc") countryCode: String = "gb",
        @Query("lc") languageCode: String = "en"
    ): SearchResponse

    companion object {
        private const val BASE_URL = "https://world.openfoodfacts.org/"

        fun create(): OpenFoodFactsApi {
            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(MoshiConverterFactory.create())
                .build()
                .create(OpenFoodFactsApi::class.java)
        }
    }
}

data class SearchResponse(
    @Json(name = "count") val count: Int? = 0,
    @Json(name = "products") val products: List<Product>? = emptyList()
)

data class ProductResponse(
    @Json(name = "status") val status: Any? = null,
    @Json(name = "status_verbose") val statusVerbose: String? = null,
    @Json(name = "code") val code: String? = null,
    @Json(name = "product") val product: Product? = null
)

data class Product(
    @Json(name = "code") val code: String? = null,
    @Json(name = "product_name") val productName: String? = null,
    @Json(name = "product_name_en") val productNameEn: String? = null,
    @Json(name = "generic_name") val genericName: String? = null,
    @Json(name = "generic_name_en") val genericNameEn: String? = null,
    @Json(name = "abbreviated_product_name") val abbreviatedProductName: String? = null,
    @Json(name = "brands") val brands: String? = null,
    @Json(name = "brands_tags") val brandsTags: List<String>? = null,
    @Json(name = "categories") val categories: String? = null,
    @Json(name = "quantity") val quantity: String? = null,
    @Json(name = "serving_size") val servingSize: String? = null,
    @Json(name = "serving_quantity") val servingQuantity: Double? = null,
    @Json(name = "image_url") val imageUrl: String? = null,
    @Json(name = "image_front_thumb_url") val imageThumbUrl: String? = null,
    @Json(name = "image_front_url") val imageFrontUrl: String? = null,
    @Json(name = "nutriments") val nutriments: Nutriments? = null
)

data class Nutriments(
    @Json(name = "energy-kcal_100g") val energyKcal100g: Double? = null,
    @Json(name = "energy-kcal_serving") val energyKcalServing: Double? = null,
    @Json(name = "energy-kcal") val energyKcal: Double? = null,
    @Json(name = "energy-kcal_value") val energyKcalValue: Double? = null,
    @Json(name = "energy-kcal_unit") val energyKcalUnit: String? = null,
    @Json(name = "energy_100g") val energy100g: Double? = null,
    @Json(name = "energy_serving") val energyServing: Double? = null,
    @Json(name = "energy") val energy: Double? = null,
    @Json(name = "energy_unit") val energyUnit: String? = null,
    @Json(name = "proteins_100g") val proteins100g: Double? = null,
    @Json(name = "proteins_serving") val proteinsServing: Double? = null,
    @Json(name = "proteins") val proteins: Double? = null,
    @Json(name = "carbohydrates_100g") val carbohydrates100g: Double? = null,
    @Json(name = "carbohydrates_serving") val carbohydratesServing: Double? = null,
    @Json(name = "carbohydrates") val carbohydrates: Double? = null,
    @Json(name = "fat_100g") val fat100g: Double? = null,
    @Json(name = "fat_serving") val fatServing: Double? = null,
    @Json(name = "fat") val fat: Double? = null,
    @Json(name = "fiber_100g") val fiber100g: Double? = null,
    @Json(name = "fiber_serving") val fiberServing: Double? = null,
    @Json(name = "sugars_100g") val sugars100g: Double? = null,
    @Json(name = "sugars_serving") val sugarsServing: Double? = null,
    @Json(name = "salt_100g") val salt100g: Double? = null,
    @Json(name = "salt_serving") val saltServing: Double? = null
)
