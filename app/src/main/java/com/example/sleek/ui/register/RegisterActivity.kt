package com.example.sleek.ui.register

import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.sleek.databinding.ActivityRegisterBinding
import androidx.activity.viewModels
import com.example.sleek.data.AuthResponse
import com.example.sleek.ui.login.LoginActivity

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private val viewModel: RegisterViewModel by viewModels()
    private val timeoutHandler = Handler(Looper.getMainLooper())
    private var registrationInProgress = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("RegisterActivity", "onCreate called")
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViews()
        observeViewModel()
    }

    private fun setupViews() {
        binding.registerBtn.setOnClickListener {
            if (registrationInProgress) {
                Toast.makeText(this, "Registration in progress...", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!isInternetAvailable()) {
                Toast.makeText(this, "No internet connection. Please check your connection and try again", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            val email = binding.emailRegister.text.toString()
            val password = binding.passwordRegister.text.toString()
            Log.d("RegisterActivity", "Register button clicked - Email: $email")

            if (validateInput(email, password)) {
                startRegistrationWithTimeout(email, password)
            }
        }
    }

    private fun startRegistrationWithTimeout(email: String, password: String) {
        registrationInProgress = true

        // Set timeout for 30 seconds
        timeoutHandler.postDelayed({
            if (registrationInProgress) {
                registrationInProgress = false
                showLoading(false)
                Toast.makeText(
                    this,
                    "Registration timeout. Please check your email for verification or try again",
                    Toast.LENGTH_LONG
                ).show()
                // Redirect ke login karena kemungkinan registrasi sudah berhasil
                navigateToLogin()
            }
        }, 30000) // 30 seconds timeout

        viewModel.register(email, password)
    }

    private fun isInternetAvailable(): Boolean {
        val connectivityManager = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo
            @Suppress("DEPRECATION")
            return networkInfo != null && networkInfo.isConnected
        }
    }

    private fun validateInput(email: String, password: String): Boolean {
        binding.apply {
            when {
                email.isEmpty() -> {
                    emailRegister.error = "Email cannot be empty"
                    return false
                }
                !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                    emailRegister.error = "Please enter a valid email"
                    return false
                }
                password.isEmpty() -> {
                    passwordRegister.error = "Password cannot be empty"
                    return false
                }
                password.length < 6 -> {
                    passwordRegister.error = "Password must be at least 6 characters"
                    return false
                }
            }
        }
        return true
    }

    private fun observeViewModel() {
        viewModel.registerState.observe(this) { state ->
            when(state) {
                is RegisterState.Loading -> {
                    showLoading(true)
                    Log.d("RegisterActivity", "Loading state")
                }
                is RegisterState.Success -> {
                    registrationInProgress = false
                    timeoutHandler.removeCallbacksAndMessages(null)
                    showLoading(false)
                    Log.d("RegisterActivity", "Success state: ${state.response}")
                    handleRegisterSuccess(state.response)
                }
                is RegisterState.Error -> {
                    registrationInProgress = false
                    timeoutHandler.removeCallbacksAndMessages(null)
                    showLoading(false)
                    Log.d("RegisterActivity", "Error state: ${state.message}")
                    handleError(state.message)
                }
            }
        }
    }

    private fun showLoading(isLoading: Boolean) {
        binding.apply {
            registerBtn.isEnabled = !isLoading
            registerBtn.text = if (isLoading) "Loading..." else "Register"
            progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
    }

    private fun handleError(message: String) {
        when {
            message.contains("timeout", ignoreCase = true) -> {
                Toast.makeText(
                    this,
                    "Connection timeout. Please check your email for verification or try again later",
                    Toast.LENGTH_LONG
                ).show()
                navigateToLogin()
            }
            message.contains("already exists", ignoreCase = true) -> {
                Toast.makeText(this, "Email already registered. Please login", Toast.LENGTH_LONG).show()
                navigateToLogin()
            }
            else -> showError(message)
        }
    }

    private fun handleRegisterSuccess(response: AuthResponse) {
        try {
            Log.d("RegisterActivity", "Handling registration success")
            val displayMessage = response.message?.takeIf { it.isNotEmpty() }
                ?: "Registration successful! Please check your email for verification."
            Toast.makeText(this, displayMessage, Toast.LENGTH_LONG).show()
            navigateToLogin()
        } catch (e: Exception) {
            Log.e("RegisterActivity", "Error during success handling", e)
            Toast.makeText(this, "Registration completed. Please verify your email.", Toast.LENGTH_LONG).show()
            navigateToLogin()
        }
    }

    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        timeoutHandler.removeCallbacksAndMessages(null)
    }
}