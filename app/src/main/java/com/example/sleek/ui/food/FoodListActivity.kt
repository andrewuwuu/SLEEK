package com.example.sleek.ui.food

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.sleek.R
import com.example.sleek.data.FoodRepository
import com.example.sleek.data.TokenManager
import com.example.sleek.databinding.ActivityFoodListBinding
import com.example.sleek.ui.login.LoginActivity
import com.example.sleek.ui.logout.LogoutActivity
import com.example.sleek.utils.Resource
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayoutMediator

class FoodListActivity : AppCompatActivity() {
    private lateinit var binding: ActivityFoodListBinding
    private lateinit var viewModel: FoodViewModel
    private lateinit var pagerAdapter: FoodPagerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFoodListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()

        val tokenManager = TokenManager(this)
        if (!tokenManager.isTokenValid()) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        setupViewModel(tokenManager)
        setupViewPager()
        setupData()

        val foodAllergies = intent.getStringArrayListExtra("FOOD_ALLERGIES")?.toList() ?: listOf()
        viewModel.setFoodAllergies(foodAllergies)
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        // Hapus baris ini jika Anda ingin menampilkan judul
        supportActionBar?.setDisplayShowTitleEnabled(false)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate menu resource file
        menuInflater.inflate(R.menu.food_list_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_logout -> {
                startActivity(Intent(this, LogoutActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }



    private fun setupViewModel(tokenManager: TokenManager) {
        val repository = FoodRepository.getInstance(this)
        val viewModelFactory = FoodViewModelFactory(repository, tokenManager)
        viewModel = ViewModelProvider(this, viewModelFactory)[FoodViewModel::class.java]
    }

    private fun setupViewPager() {
        pagerAdapter = FoodPagerAdapter(this)
        binding.viewPager.adapter = pagerAdapter

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Sarapan"
                1 -> "Makan Siang"
                else -> "Makan Malam"
            }
        }.attach()
    }

    private fun setupData() {
        viewModel.foodData.observe(this) { result ->
            when (result) {
                is Resource.Success -> {
                    showLoading(false)
                    result.data?.let {
                        Log.d("FoodListActivity", "Received meal plans: $it")
                        pagerAdapter.updateData(it)
                    }
                }
                is Resource.Error -> {
                    showLoading(false)
                    result.message?.let {
                        Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG).show()
                        if (it.contains("berakhir", true) || it.contains("login", true)) {
                            startActivity(Intent(this, LoginActivity::class.java))
                            finish()
                        }
                    }
                }
                is Resource.Loading -> {
                    Log.d("FoodListActivity", "Loading meal plans...")
                    showLoading(true)
                }
            }
        }
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
    }
}