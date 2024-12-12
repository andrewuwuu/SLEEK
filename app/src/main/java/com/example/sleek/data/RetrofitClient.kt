package com.example.sleek.data

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

object RetrofitClient {
    private const val BASE_URL = "https://tesbackendfin-932656826209.asia-southeast2.run.app/"
    private const val TIMEOUT = 30L
    private lateinit var tokenManager: TokenManager

    fun initialize(tokenManager: TokenManager) {
        this.tokenManager = tokenManager
    }

    private val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
        override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
        override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
        override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
    })

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private fun createOkHttpClient(): OkHttpClient {
        return try {
            val sslContext = SSLContext.getInstance("TLS")
            sslContext.init(null, trustAllCerts, SecureRandom())

            OkHttpClient.Builder().apply {
                sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
                hostnameVerifier { _, _ -> true }
                connectTimeout(60, TimeUnit.SECONDS)
                readTimeout(60, TimeUnit.SECONDS)
                writeTimeout(60, TimeUnit.SECONDS)
                retryOnConnectionFailure(true)
                addInterceptor(loggingInterceptor)
                addInterceptor { chain ->
                    val original = chain.request()
                    val requestBuilder = original.newBuilder()
                        .header("Content-Type", "application/json")
                        .header("Accept", "application/json")

                    // Add token only if available and valid
                    if (::tokenManager.isInitialized && tokenManager.isTokenValid()) {
                        val token = tokenManager.getIdToken()
                        if (!token.isNullOrEmpty()) {
                            requestBuilder.header("Authorization", "Bearer $token")
                            Log.d("RetrofitClient", "Adding token to request")
                        }
                    }

                    val request = requestBuilder.method(original.method, original.body).build()
                    Log.d("RetrofitClient", "Making request to: ${request.url}")

                    try {
                        chain.proceed(request)
                    } catch (e: Exception) {
                        when (e) {
                            is java.net.UnknownHostException -> {
                                Log.e(
                                    "RetrofitClient",
                                    "Tidak dapat terhubung ke server. Periksa koneksi internet.",
                                    e
                                )
                            }

                            is java.net.SocketTimeoutException -> {
                                Log.e("RetrofitClient", "Koneksi timeout", e)
                            }

                            is javax.net.ssl.SSLHandshakeException -> {
                                Log.e("RetrofitClient", "SSL Certificate error", e)
                            }

                            else -> {
                                Log.e("RetrofitClient", "Network request failed: ${e.message}", e)
                            }
                        }
                        throw e
                    }
                }
            }.build()
        } catch (e: Exception) {
            Log.e("RetrofitClient", "Error creating OkHttpClient", e)
            throw e
        }
    }

    fun <T> createService(serviceClass: Class<T>): T {
        return try {
            Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(createOkHttpClient())
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(serviceClass)
        } catch (e: Exception) {
            Log.e("RetrofitClient", "Error creating Retrofit service", e)
            throw e
        }
    }
}