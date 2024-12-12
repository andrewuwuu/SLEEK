package com.example.sleek.data

import androidx.recyclerview.widget.DiffUtil

class FoodDiffCallback(
    private val oldList: List<MealPlanItem>,
    private val newList: List<MealPlanItem>
) : DiffUtil.Callback() {
    override fun getOldListSize() = oldList.size
    override fun getNewListSize() = newList.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition].dishName == newList[newItemPosition].dishName
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition] == newList[newItemPosition]
    }
}