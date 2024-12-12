package com.example.sleek.ui.food

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sleek.data.FoodRepository
import com.example.sleek.data.MealPlanItem
import com.example.sleek.data.TokenManager
import com.example.sleek.utils.Resource
import kotlinx.coroutines.launch

class FoodViewModel(
    private val repository: FoodRepository,
    private val tokenManager: TokenManager
) : ViewModel() {
    private val _foodData = MutableLiveData<Resource<List<MealPlanItem>>>()
    val foodData: LiveData<Resource<List<MealPlanItem>>> = _foodData

    private var foodAllergies: List<String> = listOf()
    private var isDataLoaded = false

    init {
        checkTokenAndGetData()
    }


    fun setFoodAllergies(allergies: List<String>) {
        foodAllergies = allergies
        refreshData()
    }

    private fun checkTokenAndGetData() {
        if (!isDataLoaded && validateToken()) {
            getMealPlans()
        }
    }

    private fun validateToken(): Boolean {
        return if (!tokenManager.isTokenValid()) {
            _foodData.value = Resource.Error("Sesi Anda telah berakhir. Silakan login kembali.")
            false
        } else {
            true
        }
    }

    private fun getMealPlans() {
        viewModelScope.launch {
            _foodData.value = Resource.Loading()
            try {
                repository.getMealPlans(foodAllergies).collect { result ->
                    _foodData.value = result
                    if (result is Resource.Success) {
                        isDataLoaded = true
                    } else if (result is Resource.Error) {
                        isDataLoaded = false
                    }
                }
            } catch (e: Exception) {
                isDataLoaded = false
                _foodData.value = Resource.Error("Terjadi kesalahan saat memuat data: ${e.message}")
            }
        }
    }

    fun refreshData() {
        isDataLoaded = false
        checkTokenAndGetData()
    }
}