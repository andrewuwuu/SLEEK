package com.example.sleek.ui.food

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.sleek.R
import com.example.sleek.data.FoodRepository
import com.example.sleek.data.MealPlanItem
import com.example.sleek.data.local.AppDatabase
import com.example.sleek.utils.Resource
import com.google.android.material.snackbar.Snackbar

class FoodCategoryFragment : Fragment() {
    private var _recyclerView: RecyclerView? = null
    private val recyclerView get() = _recyclerView!!
    private lateinit var adapter: FoodAdapter
    private var mealType: String? = null
    private lateinit var viewModel: FoodViewModel
    private var pendingData: List<MealPlanItem>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mealType = arguments?.getString(ARG_MEAL_TYPE)
        adapter = FoodAdapter() // Initialize adapter early

        // Initialize ViewModel
        val repository = FoodRepository.getInstance(requireContext())
        val database = AppDatabase.getDatabase(requireContext())
        val mealPlanDao = database.mealPlanDao()
        val viewModelFactory = FoodViewModelFactory(repository, mealPlanDao, mealType)
        viewModel = ViewModelProvider(this, viewModelFactory)[FoodViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_food_category, container, false)
        _recyclerView = view.findViewById(R.id.recyclerView)
        setupRecyclerView()
        observeData()

        // Apply any pending data updates
        pendingData?.let {
            updateData(it)
            pendingData = null
        }

        return view
    }

    private fun setupRecyclerView() {
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = adapter
    }

    private fun observeData() {
        viewModel.foodData.observe(viewLifecycleOwner) { result ->
            when (result) {
                is Resource.Success -> {
                    result.data?.let { entities ->
                        if (entities.isEmpty()) {
                            showError("Tidak ada menu tersedia untuk ${mealType ?: "unknown"}")
                        } else {
                            val mealItems = entities.map { entity ->
                                MealPlanItem(
                                    meal = entity.meal,
                                    dishName = entity.dishName,
                                    calories = entity.calories,
                                    ingredients = entity.ingredients?.split(",") ?: listOf()
                                )
                            }
                            updateData(mealItems)
                        }
                    }
                }
                is Resource.Error -> showError(result.message ?: "Unknown error")
                is Resource.Loading -> {
                    // Show loading if needed
                }
            }
        }
    }

    fun updateData(foods: List<MealPlanItem>) {
        if (!isAdded) {
            pendingData = foods
            return
        }

        Log.d("FoodCategoryFragment", "Updating ${mealType} with ${foods.size} items")
        adapter.updateData(foods)
    }

    private fun showError(message: String) {
        view?.let {
            Snackbar.make(it, message, Snackbar.LENGTH_LONG).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _recyclerView = null
    }

    companion object {
        private const val ARG_MEAL_TYPE = "meal_type"

        fun newInstance(mealType: String): FoodCategoryFragment {
            return FoodCategoryFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_MEAL_TYPE, mealType)
                }
            }
        }
    }
}