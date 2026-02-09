
package com.example.foodexpiryapp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.foodexpiryapp.domain.model.FoodItem
import com.example.foodexpiryapp.domain.model.FoodCategory
import com.example.foodexpiryapp.domain.model.StorageLocation
import java.time.LocalDate
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        // Test creating a food item
        val milk = FoodItem(
            id = 1,
            name = "Milk",
            category = FoodCategory.DAIRY,
            expiryDate = LocalDate.now().plusDays(7),
            quantity = 1,
            location = StorageLocation.FRIDGE
        )

// Print it to Logcat
        android.util.Log.d("FoodExpiryApp", "Created food item: $milk")
    }
}


