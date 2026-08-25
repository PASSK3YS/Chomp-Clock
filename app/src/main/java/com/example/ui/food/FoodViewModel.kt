package com.example.ui.food

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.entity.FoodEntry
import com.example.data.local.entity.SavedFoodItem
import com.example.data.repository.FoodSearchResult
import com.example.data.repository.UkFoodRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class BarcodeLookupState {
    object Idle : BarcodeLookupState()
    object Loading : BarcodeLookupState()
    data class Success(val food: FoodSearchResult) : BarcodeLookupState()
    data class NotFound(val barcode: String) : BarcodeLookupState()
    data class Error(val message: String) : BarcodeLookupState()
}

class FoodViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val dao = db.foodEntryDao()
    private val savedDao = db.savedFoodItemDao()
    private val repository = UkFoodRepository()

    val foodEntries: StateFlow<List<FoodEntry>> = dao.getAllEntries()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val savedFoodItems: StateFlow<List<SavedFoodItem>> = savedDao.getAllSavedItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Search state
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedSupermarket = MutableStateFlow("All")
    val selectedSupermarket: StateFlow<String> = _selectedSupermarket.asStateFlow()

    private val _searchResults = MutableStateFlow<List<FoodSearchResult>>(emptyList())
    val searchResults: StateFlow<List<FoodSearchResult>> = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    // Barcode Lookup State
    private val _barcodeLookupState = MutableStateFlow<BarcodeLookupState>(BarcodeLookupState.Idle)
    val barcodeLookupState: StateFlow<BarcodeLookupState> = _barcodeLookupState.asStateFlow()

    private var searchJob: Job? = null

    init {
        // Initial popular UK items list
        viewModelScope.launch {
            performSearch("", "All")
        }
    }

    fun onSearchQueryChanged(newQuery: String) {
        _searchQuery.value = newQuery
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _isSearching.value = true
            delay(250) // Debounce typing
            performSearch(newQuery, _selectedSupermarket.value)
            _isSearching.value = false
        }
    }

    fun onSupermarketFilterChanged(supermarket: String) {
        _selectedSupermarket.value = supermarket
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _isSearching.value = true
            performSearch(_searchQuery.value, supermarket)
            _isSearching.value = false
        }
    }

    private suspend fun performSearch(query: String, supermarket: String) {
        val results = repository.searchFood(query, supermarket)
        _searchResults.value = results
    }

    fun addFoodEntry(
        name: String,
        servingSize: String,
        calories: Int,
        mealType: String,
        barcode: String? = null,
        brandOrSupermarket: String = "Custom / Saved",
        saveForFuture: Boolean = false
    ) {
        val cleanName = name.trim().ifEmpty { "Food item" }
        val cleanServing = servingSize.trim().ifEmpty { "1 serving" }
        val cleanCalories = maxOf(0, calories)

        viewModelScope.launch {
            dao.insertEntry(
                FoodEntry(
                    name = cleanName,
                    servingSize = cleanServing,
                    calories = cleanCalories,
                    mealType = mealType,
                    date = System.currentTimeMillis(),
                    barcode = barcode
                )
            )

            if (saveForFuture) {
                savedDao.insertItem(
                    SavedFoodItem(
                        name = cleanName,
                        servingSize = cleanServing,
                        calories = cleanCalories,
                        defaultMealType = mealType,
                        barcode = barcode,
                        brandOrSupermarket = brandOrSupermarket
                    )
                )
            }
        }
    }

    fun saveFoodItemDirectly(
        name: String,
        servingSize: String,
        calories: Int,
        defaultMealType: String = "Snacks",
        barcode: String? = null,
        brandOrSupermarket: String = "Custom Saved"
    ) {
        val cleanName = name.trim().ifEmpty { "Food item" }
        val cleanServing = servingSize.trim().ifEmpty { "1 serving" }
        val cleanCalories = maxOf(0, calories)

        viewModelScope.launch {
            savedDao.insertItem(
                SavedFoodItem(
                    name = cleanName,
                    servingSize = cleanServing,
                    calories = cleanCalories,
                    defaultMealType = defaultMealType,
                    barcode = barcode,
                    brandOrSupermarket = brandOrSupermarket
                )
            )
        }
    }

    fun deleteSavedFoodItem(item: SavedFoodItem) {
        viewModelScope.launch {
            savedDao.deleteItem(item)
        }
    }

    fun deleteFoodEntry(entry: FoodEntry) {
        viewModelScope.launch {
            dao.deleteEntry(entry)
        }
    }

    fun scanBarcodeAndLookup(barcode: String, onResult: (FoodSearchResult?) -> Unit) {
        _barcodeLookupState.value = BarcodeLookupState.Loading
        viewModelScope.launch {
            try {
                // First check if user already saved this barcode locally!
                val localSaved = savedDao.getByBarcode(barcode)
                if (localSaved != null) {
                    val result = FoodSearchResult(
                        id = "saved_${localSaved.id}",
                        name = localSaved.name,
                        brandOrSupermarket = localSaved.brandOrSupermarket,
                        category = localSaved.defaultMealType,
                        caloriesPerServing = localSaved.calories,
                        servingSize = localSaved.servingSize,
                        caloriesPer100g = localSaved.calories,
                        barcode = barcode,
                        isUkSupermarket = true
                    )
                    _barcodeLookupState.value = BarcodeLookupState.Success(result)
                    onResult(result)
                    return@launch
                }

                val result = repository.lookupBarcode(barcode)
                if (result != null) {
                    _barcodeLookupState.value = BarcodeLookupState.Success(result)
                    onResult(result)
                } else {
                    _barcodeLookupState.value = BarcodeLookupState.NotFound(barcode)
                    onResult(null)
                }
            } catch (e: Exception) {
                _barcodeLookupState.value = BarcodeLookupState.Error(e.localizedMessage ?: "Failed to lookup barcode")
                onResult(null)
            }
        }
    }

    fun clearBarcodeState() {
        _barcodeLookupState.value = BarcodeLookupState.Idle
    }
}
