package com.example.sleek.data

import android.util.Log
import okhttp3.Response
import okhttp3.Interceptor
import java.io.IOException

class RetryInterceptor(private val maxRetries: Int = 3) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        var response: Response? = null
        var exception: Exception? = null

        repeat(maxRetries) { attempt ->
            try {
                response = chain.proceed(request)
            } catch (e: Exception) {
                exception = e
                Log.e("RetryInterceptor", "Attempt ${attempt + 1} failed: ${e.message}")
                if (attempt == maxRetries - 1) throw e
            }
        }

        throw exception ?: IOException("Unknown error occurred")
    }
}