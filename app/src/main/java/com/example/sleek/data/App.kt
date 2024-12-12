package com.example.sleek.data

import android.app.Application
import com.example.sleek.data.TokenManager

class App : Application() {
    companion object {
        lateinit var instance: App
            private set
    }

    lateinit var tokenManager: TokenManager

    override fun onCreate() {
        super.onCreate()
        instance = this
        tokenManager = TokenManager(this)
    }
}