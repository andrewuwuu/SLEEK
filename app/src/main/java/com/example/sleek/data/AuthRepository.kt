package com.example.sleek.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AuthRepository {
    private val apiService: ApiService by lazy {
        RetrofitClient.createService(ApiService::class.java)
    }

    suspend fun login(request: LoginRequest): AuthResponse {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.login(request)
                if (response.isSuccessful) {
                    response.body() ?: throw Exception("Response body is null")
                } else {
                    throw Exception("Login failed: ${response.code()} ${response.message()}")
                }
            } catch (e: Exception) {
                throw e
            }
        }
    }

    suspend fun register(request: RegisterRequest): AuthResponse {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("AuthRepository", "Attempting registration for email: ${request.email}")
                val response = apiService.register(request)

                if (response.isSuccessful) {
                    Log.d("AuthRepository", "Registration successful")
                    response.body() ?: throw Exception("Response body is null")
                } else {
                    Log.e("AuthRepository", "Registration failed with code: ${response.code()}")
                    val errorBody = response.errorBody()?.string()
                    Log.e("AuthRepository", "Error body: $errorBody")
                    throw Exception("Registration failed: ${response.code()} ${response.message()}")
                }
            } catch (e: Exception) {
                Log.e("AuthRepository", "Registration error", e)
                throw e
            }
        }
    }
}