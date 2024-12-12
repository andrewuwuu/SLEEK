package com.example.sleek.ui.login

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sleek.data.App
import com.example.sleek.data.AuthRepository
import com.example.sleek.data.AuthResponse
import com.example.sleek.data.LoginRequest
import com.example.sleek.data.TokenManager
import kotlinx.coroutines.launch

class LoginViewModel(
    private val authRepository: AuthRepository = AuthRepository(),
    private val tokenManager: TokenManager = TokenManager(App.instance)
) : ViewModel() {

    private val _loginState = MutableLiveData<LoginState>()
    val loginState: LiveData<LoginState> = _loginState

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _loginState.value = LoginState.Loading
            try {
                // Create a LoginRequest object
                val loginRequest = LoginRequest(email = email, password = password)
                // Pass the LoginRequest object to the login method
                val response = authRepository.login(loginRequest)
                Log.d("LoginViewModel", "Response: $response")
                _loginState.value = LoginState.Success(response)
            } catch (e: Exception) {
                Log.e("LoginViewModel", "Login error", e)
                _loginState.value = LoginState.Error(e.message ?: "Unknown error")
            }
        }
    }
}

sealed class LoginState {
    object Loading : LoginState()
    data class Success(val response: AuthResponse) : LoginState()
    data class Error(val message: String) : LoginState()
}