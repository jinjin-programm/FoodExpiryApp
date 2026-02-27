package com.example.foodexpiryapp.presentation.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.foodexpiryapp.databinding.FragmentProfileContainerBinding
import com.example.foodexpiryapp.presentation.util.FirstTimeSetupHelper
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileContainerBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileContainerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupViewPager()
        checkAndShowFirstTimeSetupDialog()
    }

    private fun setupViewPager() {
        val adapter = ProfileTabsPagerAdapter(childFragmentManager, lifecycle)
        binding.viewPager.adapter = adapter

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = adapter.getPageTitle(position)
        }.attach()
    }

    private fun checkAndShowFirstTimeSetupDialog() {
        viewLifecycleOwner.lifecycleScope.launch {
            FirstTimeSetupHelper.isFirstTimeLaunch(requireContext()).collect { isFirstTime ->
                if (isFirstTime) {
                    showFirstTimeSetupDialog()
                }
            }
        }
    }

    private fun showFirstTimeSetupDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Welcome to Food Expiry App")
            .setMessage(
                "Track your food items and get notifications before they expire.\n\n" +
                "Get started by:\n" +
                "• Adding your household details in Settings\n" +
                "• Setting up your notification preferences\n" +
                "• Adding your first food items\n\n" +
                "Happy tracking!"
            )
            .setPositiveButton("Get Started") { dialog, _ ->
                dialog.dismiss()
                markFirstTimeSetupComplete()
            }
            .setCancelable(false)
            .show()
    }

    private fun markFirstTimeSetupComplete() {
        viewLifecycleOwner.lifecycleScope.launch {
            FirstTimeSetupHelper.markFirstTimeSetupComplete(requireContext())
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
