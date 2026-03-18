package com.example.foodexpiryapp.presentation.ui.shopping

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.foodexpiryapp.R
import com.example.foodexpiryapp.databinding.FragmentShoppingBinding
import com.example.foodexpiryapp.databinding.ItemStatCardBinding
import com.example.foodexpiryapp.domain.model.AnalyticsEvent
import com.example.foodexpiryapp.domain.model.EventType
import com.example.foodexpiryapp.domain.repository.AnalyticsRepository
import com.example.foodexpiryapp.presentation.viewmodel.ShoppingViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ShoppingFragment : Fragment() {

    private var _binding: FragmentShoppingBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ShoppingViewModel by viewModels()
    
    @Inject
    lateinit var analyticsRepository: AnalyticsRepository

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentShoppingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupStatCards()
        observeStats()
        
        // Track screen view
        analyticsRepository.trackEvent(
            AnalyticsEvent(
                eventName = "screen_view",
                eventType = EventType.SCREEN_VIEW,
                additionalData = mapOf("screen_name" to "shopping")
            )
        )
    }

    private fun setupStatCards() {
        // Setup Added Card (Green)
        val addedCard = ItemStatCardBinding.bind(binding.cardAdded.root)
        addedCard.textStatIcon.text = "🟢"
        addedCard.textStatLabel.text = "Added"
        addedCard.textStatCount.text = "0"
        addedCard.root.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.stat_added_bg))

        // Setup Eaten Card (Orange)
        val eatenCard = ItemStatCardBinding.bind(binding.cardEaten.root)
        eatenCard.textStatIcon.text = "✅"
        eatenCard.textStatLabel.text = "Eaten"
        eatenCard.textStatCount.text = "0"
        eatenCard.root.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.stat_eaten_bg))

        // Setup Expired Card (Red)
        val expiredCard = ItemStatCardBinding.bind(binding.cardExpired.root)
        expiredCard.textStatIcon.text = "❌"
        expiredCard.textStatLabel.text = "Expired"
        expiredCard.textStatCount.text = "0"
        expiredCard.root.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.stat_expired_bg))
    }

    private fun observeStats() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.weeklyStats.collect { stats ->
                    ItemStatCardBinding.bind(binding.cardAdded.root).textStatCount.text = stats.itemsAdded.toString()
                    ItemStatCardBinding.bind(binding.cardEaten.root).textStatCount.text = stats.itemsEaten.toString()
                    ItemStatCardBinding.bind(binding.cardExpired.root).textStatCount.text = stats.itemsExpired.toString()
                    binding.textNotificationsCount.text = stats.notificationsSent.toString()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
