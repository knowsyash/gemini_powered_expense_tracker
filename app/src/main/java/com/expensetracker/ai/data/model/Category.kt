package com.expensetracker.ai.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class Category(
        @PrimaryKey(autoGenerate = true) val id: Long = 0,
        val name: String,
        val parentCategory: String,
        val isMainCategory: Boolean = false,
        val colorCode: String? = null,
        val iconName: String? = null
)

// Helper class to define category structure
object CategoryStructure {
    val MAIN_CATEGORIES = listOf("Travel", "Food", "Stationary", "Miscellaneous")

    val CATEGORY_SUBCATEGORIES =
            mapOf(
                    "Travel" to
                            listOf(
                                    "Flight",
                                    "Hotel",
                                    "Taxi/Uber",
                                    "Bus/Train",
                                    "Fuel",
                                    "Visa/Passport",
                                    "Travel Insurance",
                                    "Local Transport",
                                    "Other Travel"
                            ),
                    "Food" to
                            listOf(
                                    "Restaurant",
                                    "Groceries",
                                    "Coffee/Tea",
                                    "Snacks",
                                    "Delivery",
                                    "Fast Food",
                                    "Dining Out",
                                    "Cooking Ingredients",
                                    "Other Food"
                            ),
                    "Stationary" to
                            listOf(
                                    "Books",
                                    "Pens/Pencils",
                                    "Notebooks",
                                    "Office Supplies",
                                    "Electronics",
                                    "Software",
                                    "Printer Items",
                                    "Art Supplies",
                                    "Other Stationary"
                            ),
                    "Miscellaneous" to
                            listOf(
                                    "Friend",
                                    "Family",
                                    "Gift",
                                    "Donation",
                                    "Entertainment",
                                    "Health",
                                    "Personal Care",
                                    "Bills",
                                    "Other Misc"
                            )
            )

    fun getMainCategoryForSubcategory(subcategory: String): String {
        for ((mainCategory, subcategories) in CATEGORY_SUBCATEGORIES) {
            if (subcategories.contains(subcategory)) {
                return mainCategory
            }
        }
        return "Miscellaneous" // Default fallback
    }

    fun getAllSubcategories(): List<String> {
        return CATEGORY_SUBCATEGORIES.values.flatten()
    }
}
