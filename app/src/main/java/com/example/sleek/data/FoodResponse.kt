package com.example.sleek.data

import com.google.gson.annotations.SerializedName

data class MealPlanResponse(
	@SerializedName("mealPlans")
	val mealPlans: List<ParsedMealPlans>? = null
)

data class ParsedMealPlans(
	@SerializedName("mealPlan")
	val mealPlan: List<MealPlanItem>? = null
)

data class MealPlanItem(
	@SerializedName("meal")
	val meal: String? = null,
	@SerializedName("ingredients")
	val ingredients: List<String?>? = null,
	@SerializedName("dishName")
	val dishName: String? = null,
	@SerializedName("calories")
	val calories: String? = null
)