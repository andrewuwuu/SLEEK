package com.example.sleek.data

data class GroupedMealPlans(
    val breakfast: List<MealPlanItem> = emptyList(),
    val lunch: List<MealPlanItem> = emptyList(),
    val dinner: List<MealPlanItem> = emptyList()
)
