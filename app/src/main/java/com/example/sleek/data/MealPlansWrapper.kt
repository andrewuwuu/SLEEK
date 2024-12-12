package com.example.sleek.data

import com.google.gson.annotations.SerializedName

data class MealPlansWrapper(
    @SerializedName("mealPlans")
    val mealPlans: List<ParsedMealPlans>? = null
)
