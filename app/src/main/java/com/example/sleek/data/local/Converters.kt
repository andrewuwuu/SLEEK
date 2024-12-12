package com.example.sleek.data.local

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Converters {
    @TypeConverter
    fun fromIngredientsList(ingredients: List<String>?): String? {
        return Gson().toJson(ingredients)
    }

    @TypeConverter
    fun toIngredientsList(data: String?): List<String>? {
        return data?.let {
            val type = object : TypeToken<List<String>>() {}.type
            Gson().fromJson(it, type)
        }
    }
}