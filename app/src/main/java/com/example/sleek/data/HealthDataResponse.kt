package com.example.sleek.data

data class HealthDataResponse(
    val predicted_bmr: Double,
    val weight_category: String,
    val recommended_calories: Double? = null,
)
