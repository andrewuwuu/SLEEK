package com.example.sleek.ui.food

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.sleek.R
import com.example.sleek.data.MealPlanItem
import com.google.firebase.database.*

class FoodCategoryFragment : Fragment() {
    private var _recyclerView: RecyclerView? = null
    private val recyclerView get() = _recyclerView!!
    private lateinit var adapter: FoodAdapter
    private var mealType: String? = null
    private lateinit var databaseReference: DatabaseReference

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mealType = arguments?.getString(ARG_MEAL_TYPE)
        databaseReference = FirebaseDatabase.getInstance().getReference("meals")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_food_category, container, false)
        _recyclerView = view.findViewById(R.id.recyclerView)
        setupRecyclerView()
        fetchMeals()
        return view
    }

    private fun setupRecyclerView() {
        recyclerView.layoutManager = LinearLayoutManager(context)
        adapter = FoodAdapter()
        recyclerView.adapter = adapter
    }

    private fun fetchMeals() {
        if (mealType.isNullOrEmpty()) {
            Log.e("FoodCategoryFragment", "Meal type is null or empty")
            return
        }

        databaseReference.child(mealType!!).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val mealList = mutableListOf<MealPlanItem>()
                    for (mealSnapshot in snapshot.children) {
                        val meal = mealSnapshot.getValue(MealPlanItem::class.java)
                        meal?.let { mealList.add(it) }
                    }
                    updateData(mealList)
                } else {
                    Log.e("FoodCategoryFragment", "No data found for meal type: $mealType")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FoodCategoryFragment", "Firebase error: ${error.message}")
            }
        })
    }

    fun updateData(foods: List<MealPlanItem>) {
        if (!::adapter.isInitialized) {
            Log.e("FoodCategoryFragment", "Adapter not initialized")
            return
        }

        adapter.updateData(foods)
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _recyclerView = null
    }
}