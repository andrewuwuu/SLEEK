package com.example.sleek.ui.food

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.sleek.data.FoodRepository
import com.example.sleek.data.local.MealPlanDao

class FoodViewModelFactory(
    private val foodRepository: FoodRepository,
    private val mealPlanDao: MealPlanDao,
    private val mealType: String? // Nullable mealType parameter
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FoodViewModel::class.java)) {
            return FoodViewModel(foodRepository, mealPlanDao, mealType) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}