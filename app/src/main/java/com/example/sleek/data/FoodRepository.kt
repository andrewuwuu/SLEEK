package com.example.sleek.data

import android.content.Context
import android.util.Log
import com.example.sleek.utils.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import retrofit2.HttpException
import java.io.IOException

class FoodRepository(
    private val apiService: ApiService,
    private val tokenManager: TokenManager
) {
    suspend fun getMealPlans(foodAllergies: List<String> = listOf()): Flow<Resource<List<MealPlanItem>>> = flow {
        emit(Resource.Loading())
        try {
            val token = tokenManager.getIdToken()
            if (token == null) {
                emit(Resource.Error("Silakan login terlebih dahulu"))
                return@flow
            }

            if (!tokenManager.isTokenValid()) {
                tokenManager.clearTokens()
                emit(Resource.Error("Sesi Anda telah berakhir. Silakan login kembali."))
                return@flow
            }

            val response = apiService.getMealPlans("Bearer $token")

            when {
                response.isSuccessful -> {
                    val mealPlanResponse = response.body()
                    Log.d("FoodRepository", "Response body: ${response.body()}")

                    val allMealPlans = mutableListOf<MealPlanItem>()
                    mealPlanResponse?.mealPlans?.forEach { parsedPlans ->
                        parsedPlans.mealPlan?.let { mealPlans ->
                            allMealPlans.addAll(mealPlans)
                        }
                    }

                    if (allMealPlans.isEmpty()) {
                        emit(Resource.Error("Tidak ada data makanan yang tersedia"))
                    } else {
                        val filteredMealPlans = if (foodAllergies.isNotEmpty()) {
                            allMealPlans.filter { mealPlan ->
                                !foodAllergies.any { allergy ->
                                    mealPlan.dishName?.contains(allergy, ignoreCase = true) == true ||
                                            mealPlan.ingredients?.any {
                                                it!!.contains(allergy, ignoreCase = true)
                                            } == true
                                }
                            }
                        } else {
                            allMealPlans
                        }

                        if (filteredMealPlans.isEmpty()) {
                            emit(Resource.Error("Tidak ada rekomendasi makanan yang sesuai dengan alergi Anda"))
                        } else {
                            emit(Resource.Success(filteredMealPlans))
                        }
                    }
                }
                response.code() == 401 -> {
                    tokenManager.clearTokens()
                    emit(Resource.Error("Sesi Anda telah berakhir. Silakan login kembali."))
                }
                response.code() == 403 -> emit(Resource.Error("Anda tidak memiliki akses ke fitur ini"))
                response.code() == 404 -> emit(Resource.Error("Data tidak ditemukan"))
                response.code() == 500 -> emit(Resource.Error("Terjadi kesalahan pada server"))
                else -> emit(Resource.Error("Terjadi kesalahan: ${response.message()}"))
            }
        } catch (e: HttpException) {
            emit(Resource.Error("Gangguan koneksi: ${e.message ?: "Terjadi kesalahan tak terduga"}"))
        } catch (e: IOException) {
            emit(Resource.Error("Tidak dapat terhubung ke server. Periksa koneksi internet Anda"))
        } catch (e: Exception) {
            Log.e("FoodRepository", "Error: ", e)
            emit(Resource.Error("Terjadi kesalahan tak terduga: ${e.message}"))
        }
    }

    companion object {
        @Volatile
        private var instance: FoodRepository? = null

        fun getInstance(context: Context): FoodRepository {
            return instance ?: synchronized(this) {
                instance ?: FoodRepository(
                    RetrofitClient.createService(ApiService::class.java),
                    TokenManager(context)
                ).also { instance = it }
            }
        }
    }
}