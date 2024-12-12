package com.example.sleek.ui.food

import android.util.Log
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.sleek.data.MealPlanItem

class FoodPagerAdapter(fa: FragmentActivity) : FragmentStateAdapter(fa) {
    private val fragmentMap = mutableMapOf<Int, FoodCategoryFragment>()
    private var currentData: List<MealPlanItem>? = null

    private val mealTypes = listOf("Sarapan", "Makan Siang", "Makan Malam")

    override fun getItemCount(): Int = mealTypes.size

    override fun createFragment(position: Int): Fragment {
        val fragment = FoodCategoryFragment.newInstance(mealTypes[position])
        fragmentMap[position] = fragment

        // If we have data, update the new fragment immediately
        currentData?.let { data ->
            val mealType = mealTypes[position]
            val filteredData = data.filter { it.meal == mealType }
            Log.d("FoodPagerAdapter", "Creating fragment for $mealType with ${filteredData.size} items")
            fragment.updateData(filteredData)
        }

        return fragment
    }

    fun updateData(foods: List<MealPlanItem>) {
        currentData = foods
        Log.d("FoodPagerAdapter", "Updating with ${foods.size} items")

        // Update existing fragments
        fragmentMap.forEach { (position, fragment) ->
            val mealType = mealTypes[position]
            val filteredData = foods.filter { it.meal == mealType }
            Log.d("FoodPagerAdapter", "Updating $mealType with ${filteredData.size} items")
            fragment.updateData(filteredData)
        }
    }
}