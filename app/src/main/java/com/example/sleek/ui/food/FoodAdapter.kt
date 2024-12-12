package com.example.sleek.ui.food

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.sleek.R
import com.example.sleek.data.FoodDiffCallback
import com.example.sleek.data.MealPlanItem

class FoodAdapter : RecyclerView.Adapter<FoodAdapter.FoodViewHolder>() {
    private var foods: List<MealPlanItem> = emptyList()

    class FoodViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val foodName: TextView = view.findViewById(R.id.tvFoodName)
        val calories: TextView = view.findViewById(R.id.tvCalories)
        val ingredients: TextView = view.findViewById(R.id.tvIngredients)
    }

    fun updateData(newFoods: List<MealPlanItem>) {
        val diffCallback = FoodDiffCallback(foods, newFoods)
        val diffResult = DiffUtil.calculateDiff(diffCallback)

        foods = newFoods
        diffResult.dispatchUpdatesTo(this)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FoodViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_food, parent, false)
        return FoodViewHolder(view)
    }

    override fun onBindViewHolder(holder: FoodViewHolder, position: Int) {
        val food = foods[position]
        holder.foodName.text = food.dishName ?: "Nama tidak tersedia"
        holder.calories.text = food.calories ?: "Kalori tidak tersedia"
        holder.ingredients.text = "Bahan: ${food.ingredients?.joinToString(", ") ?: "Tidak ada bahan"}"
    }

    override fun getItemCount() = foods.size
}