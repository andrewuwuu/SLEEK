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
import com.example.sleek.data.MealPlanItem
import com.example.sleek.data.local.AppDatabase
import com.example.sleek.data.local.MealPlanEntity
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

        setupViewModel()
        setupViewPager()
        setupData()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        // Hide title if you don’t want to show it
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

    private fun setupViewModel() {
        val repository = FoodRepository.getInstance(this)
        val database = AppDatabase.getDatabase(this)
        val mealPlanDao = database.mealPlanDao()

        val viewModelFactory = FoodViewModelFactory(
            foodRepository = repository,
            mealPlanDao = mealPlanDao,
            mealType = null // Pass null or a default meal type if needed
        )

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
                    result.data?.let { mealPlanEntities ->
                        Log.d("FoodListActivity", "Received meal plans: $mealPlanEntities")

                        // Convert MealPlanEntity to MealPlanItem
                        val mealItems = mealPlanEntities.map { entity ->
                            MealPlanItem(
                                meal = entity.meal,
                                dishName = entity.dishName,
                                calories = entity.calories,
                                ingredients = entity.ingredients?.split(",") ?: listOf()
                            )
                        }

                        pagerAdapter.updateData(mealItems)
                    }
                }
                is Resource.Error -> {
                    showLoading(false)
                    result.message?.let { message ->
                        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
                        if (message.contains("berakhir", true) || message.contains("login", true)) {
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