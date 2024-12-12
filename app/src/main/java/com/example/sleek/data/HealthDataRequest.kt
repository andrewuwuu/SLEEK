package com.example.sleek.data

data class HealthDataRequest(
    val age: String,
    val gender: String,
    val weight_kg: String,
    val height_cm: String,
    val food_allergies: List<String> = listOf()
)
