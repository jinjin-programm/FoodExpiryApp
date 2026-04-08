package com.example.foodexpiryapp.presentation.ui.detection

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import dagger.hilt.android.AndroidEntryPoint

/**
 * Stub ConfirmationFragment — will be fully implemented in Task 2.
 * Exists to satisfy nav_graph reference for Task 1 build verification.
 */
@AndroidEntryPoint
class ConfirmationFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return View(requireContext())
    }
}
