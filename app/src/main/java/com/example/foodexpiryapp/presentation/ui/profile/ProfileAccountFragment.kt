package com.example.foodexpiryapp.presentation.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.foodexpiryapp.databinding.FragmentProfileAccountBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ProfileAccountFragment : Fragment() {

    private var _binding: FragmentProfileAccountBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileAccountBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // Account fragment content will be added here
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
