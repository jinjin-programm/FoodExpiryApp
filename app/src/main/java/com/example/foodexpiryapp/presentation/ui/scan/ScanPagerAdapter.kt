package com.example.foodexpiryapp.presentation.ui.scan

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.foodexpiryapp.presentation.ui.vision.VisionScanFragment
import com.example.foodexpiryapp.presentation.ui.yolo.YoloScanFragment

/**
 * ViewPager2 adapter for the three scan modes.
 * Per D-04: Three scan modes via horizontal swipe — Photo Scan, Barcode Scan, YOLO Scan.
 * Per D-06: Existing Photo and Barcode scan modes unchanged.
 */
class ScanPagerAdapter(
    fragmentManager: FragmentManager,
    lifecycle: Lifecycle
) : FragmentStateAdapter(fragmentManager, lifecycle) {

    companion object {
        const val TAB_PHOTO = 0
        const val TAB_BARCODE = 1
        const val TAB_YOLO = 2
    }

    override fun getItemCount(): Int = 3

    override fun createFragment(position: Int): Fragment = when (position) {
        TAB_PHOTO -> VisionScanFragment()      // Photo Scan (existing)
        TAB_BARCODE -> ScanFragment()          // Barcode Scan (existing)
        TAB_YOLO -> YoloScanFragment()         // YOLO Scan (new/updated)
        else -> throw IllegalArgumentException("Invalid scan tab position: $position")
    }
}
