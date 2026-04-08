package com.example.foodexpiryapp.presentation.ui.scan

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.foodexpiryapp.databinding.FragmentScanContainerBinding
import dagger.hilt.android.AndroidEntryPoint

/**
 * Container fragment hosting ViewPager2 with three scan modes.
 * Per D-04: Three scan modes via ViewPager2 horizontal swipe — Photo Scan, Barcode Scan, YOLO Scan.
 * Per D-06: No visible tab bar — swipe between modes.
 */
@AndroidEntryPoint
class ScanContainerFragment : Fragment() {

    private var _binding: FragmentScanContainerBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentScanContainerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.viewPager.adapter = ScanPagerAdapter(childFragmentManager, viewLifecycleOwner.lifecycle)
        binding.viewPager.isUserInputEnabled = true   // Enable swipe between scan modes
        binding.viewPager.setCurrentItem(ScanPagerAdapter.TAB_YOLO, false) // Default to YOLO scan tab
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
