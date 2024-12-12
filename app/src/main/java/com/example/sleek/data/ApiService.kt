package com.example.sleek.data

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.HTTP
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

interface ApiService {
    @POST("/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    @POST("/register")
    suspend fun register(@Body request: RegisterRequest): Response<AuthResponse>

    @POST("/healthData")
    suspend fun sendHealthData(
        @Header("Authorization") token: String,
        @Body healthData: HealthDataRequest
    ): Response<HealthDataResponse>

    @GET("/predict")
    suspend fun getPrediction(
        @Header("Authorization") token: String,
    ): Response<HealthDataResponse>

    @GET("/mealPlan")
    suspend fun getMealPlans(
        @Header("Authorization") token: String
    ): Response<MealPlanResponse>

    @POST("/logout")
    suspend fun logout(@Body request: LogoutRequest): Response<Unit>

}
