package com.example.sleek.ui.food

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sleek.data.FoodRepository
import com.example.sleek.data.local.MealPlanDao
import com.example.sleek.data.local.MealPlanEntity
import com.example.sleek.utils.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FoodViewModel(
    private val repository: FoodRepository,
    private val mealPlanDao: MealPlanDao,
    private val mealType: String?
) : ViewModel() {

    private val _foodData = MutableLiveData<Resource<List<MealPlanEntity>>>()
    val foodData: LiveData<Resource<List<MealPlanEntity>>> = _foodData

    init {
        fetchMealPlans()
    }

    private fun fetchMealPlans() {
        viewModelScope.launch {
            _foodData.value = Resource.Loading()

            try {
                // First try to get from local database
                val localData = if (mealType != null) {
                    mealPlanDao.getMealsByType(mealType)
                } else {
                    mealPlanDao.getAllMeals()
                }

                if (localData.isNotEmpty()) {
                    _foodData.value = Resource.Success(localData)
                }

                // Then fetch from remote
                repository.getMealPlans().collect { result ->
                    when (result) {
                        is Resource.Success -> {
                            val updatedData = if (mealType != null) {
                                mealPlanDao.getMealsByType(mealType)
                            } else {
                                mealPlanDao.getAllMeals()
                            }
                            _foodData.value = Resource.Success(updatedData)
                        }
                        is Resource.Error -> {
                            if (localData.isNotEmpty()) {
                                _foodData.value = Resource.Success(localData)
                            } else {
                                _foodData.value = Resource.Error(result.message ?: "Unknown error")
                            }
                        }
                        is Resource.Loading -> {
                            if (localData.isEmpty()) {
                                _foodData.value = Resource.Loading()
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                _foodData.value = Resource.Error("Unexpected error: ${e.message}")
            }
        }
    }

    fun refreshMealPlans() {
        fetchMealPlans()
    }
}