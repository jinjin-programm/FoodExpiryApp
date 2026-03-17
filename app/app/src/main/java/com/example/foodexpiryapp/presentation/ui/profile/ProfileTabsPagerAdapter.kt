package com.example.foodexpiryapp.presentation.ui.profile

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter

class ProfileTabsPagerAdapter(
    fragmentManager: FragmentManager,
    lifecycle: Lifecycle
) : FragmentStateAdapter(fragmentManager, lifecycle) {

    override fun getItemCount(): Int = 4

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> ProfileSettingsFragment()
            1 -> ProfileAccountFragment()
            2 -> ProfileFeedbackFragment()
            3 -> ProfileHelpFragment()
            else -> ProfileSettingsFragment()
        }
    }

    fun getPageTitle(position: Int): String {
        return when (position) {
            0 -> "Settings"
            1 -> "Account"
            2 -> "Feedback"
            3 -> "Help"
            else -> ""
        }
    }
}
