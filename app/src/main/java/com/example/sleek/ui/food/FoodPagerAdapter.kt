package com.example.sleek.ui.food

import android.util.Log
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.sleek.data.MealPlanItem

class FoodPagerAdapter(fa: FragmentActivity) : FragmentStateAdapter(fa) {
    private val fragments = listOf(
        FoodCategoryFragment.newInstance("Sarapan"),
        FoodCategoryFragment.newInstance("Makan Siang"),
        FoodCategoryFragment.newInstance("Makan Malam")
    )

    override fun getItemCount(): Int = fragments.size

    override fun createFragment(position: Int): Fragment = fragments[position]

    fun updateData(foods: List<MealPlanItem>) {
        val groupedFoods = foods.groupBy { it.meal ?: "Tidak Diketahui" }

        fragments.forEachIndexed { index, fragment ->
            val mealFoods = when (index) {
                0 -> groupedFoods["Sarapan"]
                1 -> groupedFoods["Makan Siang"]
                2 -> groupedFoods["Makan Malam"]
                else -> null
            }

            Log.d("FoodPagerAdapter", "Updating fragment $index with data: $mealFoods")
            mealFoods?.let { fragment.updateData(it) } ?: fragment.updateData(emptyList())
        }
    }
}