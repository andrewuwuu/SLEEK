package com.example.sleek.data

import com.google.gson.annotations.SerializedName

data class LogoutRequest(
    @SerializedName("idToken")
    val idToken: String
)
