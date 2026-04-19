package com.example.foodexpiryapp.presentation.ui.chat

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.foodexpiryapp.databinding.FragmentChatBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ChatFragment : Fragment() {

    private var _binding: FragmentChatBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ChatViewModel by viewModels()
    private lateinit var adapter: ChatAdapter

override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentChatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupListeners()
        observeState()

        viewModel.checkServerConnection()
    }

    private fun setupRecyclerView() {
        adapter = ChatAdapter()
        binding.rvMessages.layoutManager = LinearLayoutManager(requireContext()).apply {
            stackFromEnd = true
        }
        binding.rvMessages.adapter = adapter
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.etMessage.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEND) {
                sendMessage()
                true
            } else {
                false
            }
        }

        binding.etMessage.setOnKeyListener { _, keyCode, event ->
            if (keyCode == android.view.KeyEvent.KEYCODE_ENTER && event.action == android.view.KeyEvent.ACTION_DOWN) {
                val isShiftPressed = event.isShiftPressed
                if (!isShiftPressed) {
                    sendMessage()
                    true
                } else {
                    false
                }
            } else {
                false
            }
        }

        binding.etMessage.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                binding.btnSend.isEnabled = !s.isNullOrBlank() && !viewModel.isLoading.value
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        binding.btnSend.setOnClickListener {
            sendMessage()
        }
    }

    private fun sendMessage() {
        val text = binding.etMessage.text.toString()
        if (text.isNotBlank()) {
            viewModel.sendMessage(text)
            binding.etMessage.text?.clear()
        }
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.messages.collect { messages ->
                        adapter.submitList(messages) {
                            binding.rvMessages.scrollToPosition(adapter.currentList.size - 1)
                        }
                    }
                }

                launch {
                    viewModel.isLoading.collect { isLoading ->
                        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
                        binding.btnSend.isEnabled = !isLoading && !binding.etMessage.text.isNullOrBlank()
                    }
                }

                launch {
                    viewModel.isServerConnected.collect { isConnected ->
                        updateStatusIndicator(isConnected)
                        binding.btnSend.isEnabled = isConnected && !binding.etMessage.text.isNullOrBlank()
                        if (!isConnected) {
                            Toast.makeText(
                                requireContext(),
                                "AI server not connected. Please check server settings.",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }

                launch {
                    viewModel.statusText.collect { status ->
                        binding.tvStatus.text = status
                    }
                }
            }
        }
    }

    private fun updateStatusIndicator(isConnected: Boolean) {
        val color = if (isConnected) Color.GREEN else Color.RED
        binding.viewStatusIndicator.backgroundTintList = 
            android.content.res.ColorStateList.valueOf(color)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}