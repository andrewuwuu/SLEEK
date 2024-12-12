package com.example.sleek.data

import android.content.Context

class TokenManager(private val context: Context) {
    private val prefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)

    // Ubah parameter expiresIn menjadi String sesuai AuthResponse
    fun saveToken(idToken: String, refreshToken: String, expiresIn: String) {
        prefs.edit().apply {
            putString("id_token", idToken)
            putString("refresh_token", refreshToken)
            // Convert expiresIn String ke Long sebelum kalkulasi
            putLong("expires_at", System.currentTimeMillis() + (expiresIn.toLong() * 1000))
            apply()
        }
    }

    fun getIdToken(): String? = prefs.getString("id_token", null)

    fun getRefreshToken(): String? = prefs.getString("refresh_token", null)

    fun isTokenValid(): Boolean {
        val token = getIdToken()
        val expiresAt = prefs.getLong("expires_at", 0)
        return !token.isNullOrEmpty() && System.currentTimeMillis() < expiresAt
    }

    fun clearTokens() {
        prefs.edit().clear().apply()
    }
}