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
    private const val TIMEOUT = 60L // Increased timeout
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
                connectTimeout(TIMEOUT, TimeUnit.SECONDS)
                readTimeout(TIMEOUT, TimeUnit.SECONDS)
                writeTimeout(TIMEOUT, TimeUnit.SECONDS)
                retryOnConnectionFailure(true)
                followRedirects(true)
                followSslRedirects(true)
                addInterceptor(loggingInterceptor)
                addInterceptor { chain ->
                    val original = chain.request()
                    val requestBuilder = original.newBuilder()
                        .header("Content-Type", "application/json")
                        .header("Accept", "application/json")
                        .header("Connection", "keep-alive")
                        .method(original.method, original.body)

                    if (::tokenManager.isInitialized && tokenManager.isTokenValid()) {
                        tokenManager.getIdToken()?.let { token ->
                            requestBuilder.header("Authorization", "Bearer $token")
                            Log.d("RetrofitClient", "Token added to request")
                        }
                    }

                    val request = requestBuilder.build()
                    Log.d("RetrofitClient", "Making request to: ${request.url}")

                    try {
                        val response = chain.proceed(request)
                        if (!response.isSuccessful) {
                            Log.e("RetrofitClient", "Server returned error code: ${response.code}")
                        }
                        response
                    } catch (e: Exception) {
                        Log.e("RetrofitClient", "Network request failed", e)
                        throw e
                    }
                }
            }.build()
        } catch (e: Exception) {
            Log.e("RetrofitClient", "Error creating OkHttpClient", e)
            throw e
        }
    }

    private var retrofit: Retrofit? = null

    fun <T> createService(serviceClass: Class<T>): T {
        if (retrofit == null) {
            retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(createOkHttpClient())
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        }
        return retrofit!!.create(serviceClass)
    }
}