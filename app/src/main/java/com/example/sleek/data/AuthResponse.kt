package com.example.sleek.data

import com.google.gson.annotations.SerializedName

data class AuthResponse(
    @SerializedName("message") val message: String,
    @SerializedName("idToken") val idToken : String,
    @SerializedName("refreshToken") val refreshToken: String,
    @SerializedName("expiresIn") val expiresIn: String
)
