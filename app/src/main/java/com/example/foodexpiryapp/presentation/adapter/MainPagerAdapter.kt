package com.example.foodexpiryapp.presentation.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.foodexpiryapp.presentation.ui.inventory.InventoryFragment
import com.example.foodexpiryapp.presentation.ui.planner.PlannerFragment
import com.example.foodexpiryapp.presentation.ui.profile.ProfileFragment
import com.example.foodexpiryapp.presentation.ui.recipes.RecipesFragment
import com.example.foodexpiryapp.presentation.ui.shopping.ShoppingFragment

class MainPagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {

    override fun getItemCount(): Int = 5

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> InventoryFragment()
            1 -> ShoppingFragment()
            2 -> RecipesFragment()
            3 -> PlannerFragment()
            4 -> ProfileFragment()
            else -> InventoryFragment()
        }
    }
}
