package com.example.foodexpiryapp.presentation.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.example.foodexpiryapp.R
import com.example.foodexpiryapp.databinding.FragmentMainTabsBinding
import com.example.foodexpiryapp.presentation.adapter.MainPagerAdapter
import com.google.android.material.bottomnavigation.BottomNavigationView
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainTabsFragment : Fragment() {

    private var _binding: FragmentMainTabsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMainTabsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = MainPagerAdapter(requireActivity())
        binding.viewPager.adapter = adapter

        // Sync ViewPager with BottomNavigationView in MainActivity
        val bottomNav = requireActivity().findViewById<BottomNavigationView>(R.id.bottom_navigation)
        
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                val itemId = when (position) {
                    0 -> R.id.navigation_inventory
                    1 -> R.id.navigation_shopping
                    2 -> R.id.navigation_recipes
                    3 -> R.id.navigation_planner
                    4 -> R.id.navigation_profile
                    else -> return
                }
                bottomNav.selectedItemId = itemId
            }
        })

        // Listen for BottomNav changes to update ViewPager
        bottomNav.setOnItemSelectedListener { item ->
            val position = when (item.itemId) {
                R.id.navigation_inventory -> 0
                R.id.navigation_shopping -> 1
                R.id.navigation_recipes -> 2
                R.id.navigation_planner -> 3
                R.id.navigation_profile -> 4
                else -> return@setOnItemSelectedListener false
            }
            binding.viewPager.currentItem = position
            true
        }
        
        // Initial selection based on where we came from if needed
        // For simplicity, start at 0
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
