package com.example.sleek.ui.main

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.sleek.R
import com.example.sleek.data.ApiService
import com.example.sleek.data.HealthDataRequest
import com.example.sleek.data.RetrofitClient
import com.example.sleek.ui.healthresult.ResultHealthActivity
import com.example.sleek.ui.login.LoginActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

class DataUserActivity : AppCompatActivity() {
    private lateinit var ageInput: TextInputEditText
    private lateinit var genderRadioGroup: RadioGroup
    private lateinit var weightInput: TextInputEditText
    private lateinit var heightInput: TextInputEditText
    private lateinit var allergyCheckbox: MaterialCheckBox
    private lateinit var allergyInput: TextInputEditText
    private lateinit var sendButton: MaterialButton

    private val apiService = RetrofitClient.createService(ApiService::class.java)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()

        // Cek token terlebih dahulu
        val prefs = getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
        val token = prefs.getString("id_token", null)

        if (token == null) {
            Toast.makeText(this, "Session expired, please login again", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_data_user)
        initializeViews()
        setupListeners()
    }

    private fun initializeViews() {
        ageInput = findViewById(R.id.ageInput)
        genderRadioGroup = findViewById(R.id.genderRadioGroup)
        weightInput = findViewById(R.id.weightInput)
        heightInput = findViewById(R.id.heightInput)
        allergyCheckbox = findViewById(R.id.allergyCheck)
        allergyInput = findViewById(R.id.allergyInput)
        sendButton = findViewById(R.id.sendButton)
    }

    private fun setupListeners() {
        allergyCheckbox.setOnCheckedChangeListener { _, isChecked ->
            allergyInput.isEnabled = isChecked
            if (!isChecked) {
                allergyInput.text?.clear()
            }
        }

        sendButton.setOnClickListener {
            if (validateInputs()) {
                sendHealthData()
            }
        }
    }

    private fun validateInputs(): Boolean {
        val age = ageInput.text.toString()
        val gender = when (genderRadioGroup.checkedRadioButtonId) {
            R.id.maleRadio -> "1"
            R.id.femaleRadio -> "0"
            else -> ""
        }
        val weight = weightInput.text.toString()
        val height = heightInput.text.toString()

        when {
            age.isEmpty() || age.toIntOrNull() == null -> {
                ageInput.error = "Usia harus diisi dengan angka"
                return false
            }
            gender.isEmpty() -> {
                Toast.makeText(this, "Pilih gender terlebih dahulu", Toast.LENGTH_SHORT).show()
                return false
            }
            weight.isEmpty() || weight.toFloatOrNull() == null -> {
                weightInput.error = "Berat badan harus diisi dengan angka"
                return false
            }
            height.isEmpty() || height.toFloatOrNull() == null -> {
                heightInput.error = "Tinggi badan harus diisi dengan angka"
                return false
            }
            allergyCheckbox.isChecked && allergyInput.text.toString().isEmpty() -> {
                allergyInput.error = "Data alergi harus diisi"
                return false
            }
        }

        return true
    }

    private fun sendHealthData() {
        sendButton.isEnabled = false
        sendButton.text = "Sending..."

        // Mengambil token dari SharedPreferences
        val prefs = getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
        val token = prefs.getString("id_token", null)

        if (token == null) {
            Toast.makeText(this, "Session expired, please login again", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        val healthData = HealthDataRequest(
            age = ageInput.text.toString(),
            gender = when (genderRadioGroup.checkedRadioButtonId) {
                R.id.maleRadio -> "1"
                R.id.femaleRadio -> "0"
                else -> ""
            },
            weight_kg = weightInput.text.toString(),
            height_cm = heightInput.text.toString(),
            food_allergies = if (allergyCheckbox.isChecked && !allergyInput.text.isNullOrEmpty()) {
                allergyInput.text.toString().split(",").map { it.trim() }
            } else {
                listOf()
            }
        )

        lifecycleScope.launch {
            try {
                // Menambahkan token ke request
                val bearerToken = "Bearer $token"
                val response = apiService.sendHealthData(bearerToken, healthData)

                if (response.isSuccessful) {
                    Toast.makeText(this@DataUserActivity, "Data berhasil dikirim", Toast.LENGTH_SHORT).show()

                    val intent = Intent(this@DataUserActivity, ResultHealthActivity::class.java).apply {
                        putExtra("AGE", ageInput.text.toString().toInt())
                        putExtra("GENDER", when (genderRadioGroup.checkedRadioButtonId) {
                            R.id.maleRadio -> 1
                            R.id.femaleRadio -> 0
                            else -> -1
                        })
                        putExtra("WEIGHT", weightInput.text.toString().toFloat())
                        putExtra("HEIGHT", heightInput.text.toString().toFloat())
                        putStringArrayListExtra("FOOD_ALLERGIES", ArrayList(
                            if (allergyCheckbox.isChecked && !allergyInput.text.isNullOrEmpty()) {
                                allergyInput.text.toString().split(",").map { it.trim() }
                            } else {
                                listOf()
                            }
                        ))
                    }
                    startActivity(intent)
                    finish()
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e("DataUserActivity", "Error response: $errorBody")
                    when (response.code()) {
                        401 -> {
                            Toast.makeText(this@DataUserActivity, "Sesi telah berakhir, silakan login kembali", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this@DataUserActivity, LoginActivity::class.java))
                            finish()
                        }
                        else -> {
                            Toast.makeText(
                                this@DataUserActivity,
                                "Gagal mengirim data: ${response.code()}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            } catch (t: Throwable) {
                Log.e("DataUserActivity", "Network error", t)
                Toast.makeText(
                    this@DataUserActivity,
                    "Error: ${t.message ?: "Unknown error"}",
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                sendButton.isEnabled = true
                sendButton.text = "Send"
            }
        }
    }
}