package com.example.sleek.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.sleek.data.MealPlanItem

@Dao
interface MealPlanDao {
    @Query("SELECT * FROM meal_plans WHERE meal = :mealType")
    suspend fun getMealsByType(mealType: String): List<MealPlanEntity>

    @Query("SELECT * FROM meal_plans")
    suspend fun getAllMeals(): List<MealPlanEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeals(meals: List<MealPlanEntity>)

    @Query("DELETE FROM meal_plans")
    suspend fun deleteAllMeals()
}