package com.example.data.remote

import com.squareup.moshi.Json
import retrofit2.http.GET
import retrofit2.http.Path

interface OpenFoodFactsApi {
    @GET("api/v2/product/{barcode}.json")
    suspend fun getProduct(@Path("barcode") barcode: String): ProductResponse
}

data class ProductResponse(
    @Json(name = "status") val status: Int,
    @Json(name = "product") val product: Product?
)

data class Product(
    @Json(name = "product_name") val productName: String?,
    @Json(name = "nutriments") val nutriments: Nutriments?
)

data class Nutriments(
    @Json(name = "energy-kcal_100g") val energyKcal100g: Double?
)
