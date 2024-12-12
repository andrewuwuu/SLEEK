package com.example.sleek.data

import android.content.Context
import android.util.Log
import com.example.sleek.data.local.AppDatabase
import com.example.sleek.data.local.MealPlanDao
import com.example.sleek.data.local.MealPlanEntity
import com.example.sleek.utils.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import retrofit2.HttpException
import java.io.IOException

class FoodRepository(
    private val apiService: ApiService,
    private val tokenManager: TokenManager,
    private val mealPlanDao: MealPlanDao
) {
    suspend fun getMealPlans(): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())
        try {
            val token = tokenManager.getIdToken()
            if (token == null) {
                emit(Resource.Error("Please log in first"))
                return@flow
            }

            if (!tokenManager.isTokenValid()) {
                tokenManager.clearTokens()
                emit(Resource.Error("Session expired, please log in again"))
                return@flow
            }
            withContext(Dispatchers.IO) {
                mealPlanDao.deleteAllMeals()
                Log.d("FoodRepository", "Cleared old meal plans from Room")
            }

            val response = withTimeout(35_000) { // 35 seconds timeout
                apiService.getMealPlans("Bearer $token")
            }

            if (response.isSuccessful) {
                val mealPlanResponse = response.body()
                val allMealPlans = mealPlanResponse?.mealPlans?.flatMap { parsedPlans ->
                    parsedPlans.mealPlan?.map { mealPlan ->
                        MealPlanEntity(
                            meal = mealPlan.meal,
                            dishName = mealPlan.dishName,
                            calories = mealPlan.calories,
                            ingredients = mealPlan.ingredients?.joinToString(",")
                        )
                    } ?: emptyList()
                } ?: emptyList()



                withContext(Dispatchers.IO) {
                    if (allMealPlans.isNotEmpty()) {
                        mealPlanDao.insertMeals(allMealPlans)
                        Log.d("FoodRepository", "New meal plans saved to Room: $allMealPlans")
                    } else {
                        Log.d("FoodRepository", "No new meal plans received from API")
                    }
                }
                emit(Resource.Success(Unit))
            } else {
                emit(Resource.Error("Error: ${response.message()}"))
            }
        } catch (e: Exception) {
            emit(Resource.Error("Terjadi kesalahan: ${e.message}"))
            Log.e("FoodRepository", "Error fetching data: ${e.message}", e)
        }
    }


    companion object {
        @Volatile
        private var instance: FoodRepository? = null

        fun getInstance(context: Context): FoodRepository {
            val database = AppDatabase.getDatabase(context)
            return instance ?: synchronized(this) {
                instance ?: FoodRepository(
                    RetrofitClient.createService(ApiService::class.java),
                    TokenManager(context),
                    database.mealPlanDao()
                ).also { instance = it }
            }
        }
    }
}