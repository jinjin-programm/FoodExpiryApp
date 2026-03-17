package com.example.foodexpiryapp.presentation.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.foodexpiryapp.databinding.FragmentProfileFeedbackBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ProfileFeedbackFragment : Fragment() {

    private var _binding: FragmentProfileFeedbackBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileFeedbackBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // Feedback fragment content will be added here
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
