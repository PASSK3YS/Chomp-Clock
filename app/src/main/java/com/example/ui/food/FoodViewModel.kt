package com.example.ui.food

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.entity.FoodEntry
import com.example.data.remote.OpenFoodFactsApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

class FoodViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val dao = db.foodEntryDao()

    private val api: OpenFoodFactsApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://world.openfoodfacts.org/")
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
            .create(OpenFoodFactsApi::class.java)
    }

    val foodEntries: StateFlow<List<FoodEntry>> = dao.getAllEntries()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addFoodEntry(name: String, servingSize: String, calories: Int, mealType: String) {
        viewModelScope.launch {
            dao.insertEntry(
                FoodEntry(
                    name = name,
                    servingSize = servingSize,
                    calories = calories,
                    mealType = mealType,
                    date = System.currentTimeMillis()
                )
            )
        }
    }
    
    fun scanBarcode(barcode: String, mealType: String) {
        viewModelScope.launch {
            try {
                val response = api.getProduct(barcode)
                if (response.status == 1 && response.product != null) {
                    val name = response.product.productName ?: "Unknown Product"
                    val cal = response.product.nutriments?.energyKcal100g?.toInt() ?: 0
                    dao.insertEntry(
                        FoodEntry(
                            name = name,
                            servingSize = "100g",
                            calories = cal,
                            mealType = mealType,
                            date = System.currentTimeMillis(),
                            barcode = barcode
                        )
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
