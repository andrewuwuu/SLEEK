package com.example.sleek.ui.register

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sleek.data.AuthRepository
import com.example.sleek.data.AuthResponse
import com.example.sleek.data.RegisterRequest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.launch
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import retrofit2.HttpException
import java.util.concurrent.TimeoutException

class RegisterViewModel(
    private val authRepository: AuthRepository = AuthRepository()
) : ViewModel() {
    private val _registerState = MutableLiveData<RegisterState>()
    val registerState: LiveData<RegisterState> = _registerState

    companion object {
        private const val TIMEOUT_MS = 30000L // 30 seconds timeout
    }

    fun register(email: String, password: String) {
        viewModelScope.launch {
            _registerState.value = RegisterState.Loading
            try {
                Log.d("RegisterViewModel", "Attempting registration for email: $email")

                val request = RegisterRequest(
                    email = email,
                    password = password
                )

                // Wrap the repository call with timeout
                val response = withTimeout(TIMEOUT_MS) {
                    authRepository.register(request)
                }

                if (response != null) {
                    Log.d("RegisterViewModel", "Registration successful")
                    _registerState.value = RegisterState.Success(response)
                } else {
                    Log.e("RegisterViewModel", "Registration response was null")
                    _registerState.value = RegisterState.Error("Server response was invalid")
                }
            } catch (e: Exception) {
                Log.e("RegisterViewModel", "Registration failed", e)
                val errorMessage = when (e) {
                    is TimeoutException -> {
                        "Registration timeout. Please check your email for verification or try again"
                    }
                    is SocketTimeoutException -> {
                        "Connection timeout. Please check your internet connection and try again"
                    }
                    is HttpException -> {
                        when (e.code()) {
                            409 -> "Email already registered. Please login"
                            401 -> "Unauthorized access"
                            403 -> "Access forbidden"
                            404 -> "Service not found"
                            500 -> "Internal server error. Please try again later"
                            else -> "Server error: ${e.code()}"
                        }
                    }
                    is UnknownHostException -> {
                        "Network error: Unable to reach server. Please check your internet connection"
                    }
                    is ConnectException -> {
                        "Unable to connect to server. Please check your internet connection"
                    }
                    is CancellationException -> {
                        throw e // Let coroutine cancellation propagate
                    }
                    else -> {
                        Log.e("RegisterViewModel", "Unexpected error", e)
                        e.message ?: "An unexpected error occurred"
                    }
                }
                _registerState.value = RegisterState.Error(errorMessage)
            }
        }
    }
}

sealed class RegisterState {
    object Loading : RegisterState()
    data class Success(val response: AuthResponse) : RegisterState()
    data class Error(val message: String) : RegisterState()
}