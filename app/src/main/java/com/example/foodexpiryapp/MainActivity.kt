package com.example.foodexpiryapp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.viewpager2.widget.ViewPager2
import com.example.foodexpiryapp.R
import com.example.foodexpiryapp.databinding.ActivityMainBinding
import com.example.foodexpiryapp.domain.model.AnalyticsEvent
import com.example.foodexpiryapp.domain.model.EventType
import com.example.foodexpiryapp.domain.repository.AnalyticsRepository
import com.example.foodexpiryapp.presentation.adapter.MainPagerAdapter
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    @Inject
    lateinit var analyticsRepository: AnalyticsRepository

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Permission granted
        } else {
            Toast.makeText(this, "Notification permission is required for expiry alerts", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        android.util.Log.d("MainActivity", "onCreate started")
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        android.util.Log.d("MainActivity", "layout set")

        // Track app opened
        analyticsRepository.trackEvent(
            AnalyticsEvent(
                eventName = "app_opened",
                eventType = EventType.APP_OPENED
            )
        )

        askNotificationPermission()

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        // Setup ViewPager2
        val pagerAdapter = MainPagerAdapter(this)
        binding.viewPager.adapter = pagerAdapter
        binding.viewPager.offscreenPageLimit = 1

        // Sync ViewPager with BottomNavigationView
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                val itemId = when (position) {
                    0 -> R.id.navigation_inventory
                    1 -> R.id.navigation_shopping
                    2 -> R.id.navigation_recipes
                    3 -> R.id.navigation_planner
                    4 -> R.id.navigation_profile
                    else -> R.id.navigation_inventory
                }
                Log.d("NAV_DEBUG", "ViewPager2 onPageSelected: position=$position, itemId=$itemId")
                if (binding.bottomNavigation.selectedItemId != itemId) {
                    binding.bottomNavigation.selectedItemId = itemId
                }
            }
        })

        binding.bottomNavigation.setOnItemSelectedListener { item ->
            val page = when (item.itemId) {
                R.id.navigation_inventory -> 0
                R.id.navigation_shopping -> 1
                R.id.navigation_recipes -> 2
                R.id.navigation_planner -> 3
                R.id.navigation_profile -> 4
                else -> -1
            }
            if (page != -1 && binding.viewPager.currentItem != page) {
                binding.viewPager.isVisible = true
                binding.navHostFragment.isVisible = false
                binding.viewPager.post {
                    if (binding.viewPager.currentItem != page) {
                        binding.viewPager.currentItem = page
                    }
                }
            }
            page != -1
        }

        // Listen for navigation changes to hide ViewPager when on detail screens
        navController.addOnDestinationChangedListener { _, destination, _ ->
            val isMainTab = destination.id in listOf(
                R.id.navigation_inventory,
                R.id.navigation_shopping,
                R.id.navigation_recipes,
                R.id.navigation_planner,
                R.id.navigation_profile
            )
            Log.d("NAV_DEBUG", "DestinationChanged: id=${destination.id} label=${destination.label} isMainTab=$isMainTab")
            Log.d("NAV_DEBUG", "  -> ViewPager visible=$isMainTab, NavHost visible=${!isMainTab}")
            Log.d("NAV_DEBUG", "  -> NavController currentDest: id=${navController.currentDestination?.id} label=${navController.currentDestination?.label}")
            binding.viewPager.isVisible = isMainTab
            binding.navHostFragment.isVisible = !isMainTab
        }

        android.util.Log.d("MainActivity", "navigation setup complete")
    }

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
