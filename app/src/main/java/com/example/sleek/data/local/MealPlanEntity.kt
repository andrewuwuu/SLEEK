package com.example.sleek.data.local

import androidx.room.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

@Entity(tableName = "meal_plans")
data class MealPlanEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val meal: String?,
    val dishName: String?,
    val calories: String?,
    val ingredients: String?
)