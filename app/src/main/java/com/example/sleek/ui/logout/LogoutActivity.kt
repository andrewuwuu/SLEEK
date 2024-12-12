package com.example.sleek.ui.logout

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.sleek.R
import com.example.sleek.data.LogoutRequest
import com.example.sleek.data.RetrofitClient
import com.example.sleek.data.TokenManager
import com.example.sleek.data.ApiService
import com.example.sleek.ui.login.LoginActivity
import kotlinx.coroutines.launch

class LogoutActivity : AppCompatActivity() {
    private lateinit var tokenManager: TokenManager
    private lateinit var apiService: ApiService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_logout)

        // Initialize TokenManager and ApiService
        tokenManager = TokenManager(this)
        RetrofitClient.initialize(tokenManager)
        apiService = RetrofitClient.createService(ApiService::class.java)

        findViewById<Button>(R.id.btnLogout).setOnClickListener {
            lifecycleScope.launch {
                performLogout()
            }
        }

        findViewById<Button>(R.id.btnCancel).setOnClickListener {
            finish()
        }
    }

    private suspend fun performLogout() {
        try {
            val token = tokenManager.getIdToken()
            if (token == null) {
                Toast.makeText(this, "Token tidak ditemukan", Toast.LENGTH_SHORT).show()
                return
            }

            val logoutRequest = LogoutRequest(idToken = token)
            val response = apiService.logout(logoutRequest)

            if (response.isSuccessful) {
                tokenManager.clearTokens() // Using the correct method name
                Toast.makeText(this, "Berhasil keluar", Toast.LENGTH_SHORT).show()

                // Navigate to LoginActivity
                val intent = Intent(this, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            } else {
                Toast.makeText(this,
                    "Gagal keluar: ${response.message()}",
                    Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this,
                "Error: ${e.message}",
                Toast.LENGTH_SHORT).show()
        }
    }
}