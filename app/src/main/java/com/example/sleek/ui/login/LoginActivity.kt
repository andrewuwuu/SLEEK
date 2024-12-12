package com.example.sleek.ui.login

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.viewModels
import com.example.sleek.data.AuthResponse
import com.example.sleek.data.TokenManager
import com.example.sleek.databinding.ActivityLoginBinding
import com.example.sleek.ui.main.DataUserActivity
import com.example.sleek.ui.register.RegisterActivity
import com.example.sleek.ui.food.FoodListActivity

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private val viewModel: LoginViewModel by viewModels()
    private lateinit var tokenManager: TokenManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        tokenManager = TokenManager(this)
        setupViews()
        observeViewModel()
    }

    private fun setupViews() {
        binding.apply {
            loginBtn.setOnClickListener {
                val email = emailLogin.text.toString()
                val password = passwordLogin.text.toString()

                if (validateInput(email, password)) {
                    viewModel.login(email, password)
                }
            }

            registerBtn.setOnClickListener {
                startActivity(Intent(this@LoginActivity, RegisterActivity::class.java))
            }
        }
    }

    private fun validateInput(email: String, password: String): Boolean {
        binding.apply {
            when {
                email.isEmpty() -> {
                    emailLogin.error = "Email cannot be empty"
                    return false
                }
                !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                    emailLogin.error = "Please enter a valid email"
                    return false
                }
                password.isEmpty() -> {
                    passwordLogin.error = "Password cannot be empty"
                    return false
                }
            }
        }
        return true
    }

    private fun observeViewModel() {
        viewModel.loginState.observe(this) { state ->
            when(state) {
                is LoginState.Loading -> showLoading(true)
                is LoginState.Success -> {
                    showLoading(false)
                    handleLoginSuccess(state.response)
                }
                is LoginState.Error -> {
                    showLoading(false)
                    showError(state.message)
                }
            }
        }
    }

    private fun showLoading(isLoading: Boolean) {
        binding.loginBtn.apply {
            isEnabled = !isLoading
            text = if (isLoading) "Loading..." else "Login"
        }
        binding.registerBtn.isEnabled = !isLoading
    }

    private fun handleLoginSuccess(response: AuthResponse) {
        Log.d("LoginActivity", "Received token: ${response.idToken.take(20)}...")

        // Menggunakan TokenManager untuk menyimpan token
        tokenManager.saveToken(
            idToken = response.idToken,
            refreshToken = response.refreshToken,
            expiresIn = response.expiresIn
        )

        Toast.makeText(this, "Login successful", Toast.LENGTH_SHORT).show()

        // Cek apakah ada redirect dari ResultHealthActivity
        if (intent.getBooleanExtra("REDIRECT_TO_FOOD", false)) {
            val foodIntent = Intent(this, FoodListActivity::class.java).apply {
                // Forward semua data yang diterima dari ResultHealthActivity
                putExtra("CALORIES", intent.getIntExtra("CALORIES", 0))
                putExtra("HEALTH_CONDITION", intent.getStringExtra("HEALTH_CONDITION"))
                putExtra("HAS_ALLERGY", intent.getBooleanExtra("HAS_ALLERGY", false))
                putExtra("ALLERGY_TEXT", intent.getStringExtra("ALLERGY_TEXT"))
            }
            startActivity(foodIntent)
        } else {
            // Normal flow ke DataUserActivity
            Intent(this, DataUserActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(this)
            }
        }
        finish()
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}