package com.example.sleek.ui.healthresult

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.sleek.R
import com.example.sleek.data.ApiService
import com.example.sleek.data.HealthDataResponse
import com.example.sleek.data.RetrofitClient
import com.example.sleek.data.TokenManager
import com.example.sleek.databinding.ActivityResultHealthBinding
import com.example.sleek.ui.food.FoodListActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

class ResultHealthActivity : AppCompatActivity() {
    private lateinit var binding: ActivityResultHealthBinding
    private lateinit var apiService: ApiService
    private lateinit var tokenManager: TokenManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResultHealthBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.hide()

        tokenManager = TokenManager(this)

        if (!tokenManager.isTokenValid()) {
            Toast.makeText(this, "Session expired. Please login again.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        apiService = RetrofitClient.createService(ApiService::class.java)
        fetchPrediction()
        setupInitialViews()

        binding.btnGoToMain.setOnClickListener {
            openFoodListActivity()
        }
    }

    private fun setupInitialViews() {
        tokenManager = TokenManager(this)
        apiService = RetrofitClient.createService(ApiService::class.java)

        binding.btnGoToMain.setOnClickListener {
            startActivity(Intent(this, FoodListActivity::class.java))
        }
    }

    private fun updateStickmanImage(category: String) {
        val stickmanResource = when (category.lowercase()) {
            "ideal" -> R.drawable.stickman_ideal
            "obesitas", "obese" -> R.drawable.stickman_obese
            "overweight" -> R.drawable.stickman_overweight
            "malnutrisi", "underweight" -> R.drawable.stickman_underweight
            else -> R.drawable.stickman // default fallback
        }

        binding.imgStickman.apply {
            setImageResource(stickmanResource)
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            // Memperbesar ukuran gambar dengan scale
            scaleX = 2.0f
            scaleY = 2.0f
            // Mengurangi padding agar gambar bisa lebih besar
            setPadding(8, 8, 8, 8)
        }

        // Tambahkan animasi fade untuk transisi yang lebih halus
        binding.imgStickman.alpha = 0f
        binding.imgStickman.animate()
            .alpha(1f)
            .setDuration(500)
            .start()
    }

    private fun fetchPrediction() {
        if (!tokenManager.isTokenValid()) {
            Toast.makeText(this, "Session expired. Please login again.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = apiService.getPrediction("Bearer ${tokenManager.getIdToken()}")

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body() != null) {
                        showPredictionResult(response.body()!!)
                    } else {
                        Toast.makeText(
                            this@ResultHealthActivity,
                            "Failed to get prediction: ${response.message()}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@ResultHealthActivity,
                        "Error: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun openFoodListActivity(){
        val intent = Intent(this, FoodListActivity::class.java).apply {
            putStringArrayListExtra("FOOD_ALLERGIES",
                getIntent().getStringArrayListExtra("FOOD_ALLERGIES"))
        }
        startActivity(intent)
    }

    private fun showPredictionResult(prediction: HealthDataResponse) {
        binding.apply {
            // Status Kesehatan
            tvHealthResult.text = "Kategori Berat: ${prediction.weight_category}"

            // Update status kesehatan
            val healthStatus = when (prediction.weight_category.lowercase()) {
                "ideal" -> "Sehat"
                "malnutrisi", "underweight" -> "Perlu Penambahan Berat Badan"
                "overweight" -> "Perlu Pengurangan Berat Badan"
                "obesitas", "obese" -> "Perlu Konsultasi Dokter"
                else -> "Status Tidak Diketahui"
            }
            tvHealthStatus.text = "Status: $healthStatus"

            // Informasi Kalori
            tvBmr.text = "BMR: ${prediction.predicted_bmr.roundToInt()} kal/hari"

            // Set warna berdasarkan kategori
            val healthColor = when (prediction.weight_category.lowercase()) {
                "malnutrisi" -> getColor(R.color.yellow)
                "ideal" -> getColor(R.color.green)
                "overweight" -> getColor(R.color.orange)
                "obese", "obesitas" -> getColor(R.color.red)
                else -> getColor(R.color.black)
            }
            tvHealthResult.setTextColor(healthColor)

            // Update stickman image
            updateStickmanImage(prediction.weight_category)
        }
    }
}