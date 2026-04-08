package com.example.foodexpiryapp.presentation.ui.shopping

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.foodexpiryapp.databinding.FragmentShoppingBinding
import com.example.foodexpiryapp.domain.model.AnalyticsEvent
import com.example.foodexpiryapp.domain.model.EventType
import com.example.foodexpiryapp.domain.repository.AnalyticsRepository
import com.example.foodexpiryapp.presentation.adapter.ShoppingItemAdapter
import com.example.foodexpiryapp.presentation.adapter.ShoppingTemplateAdapter
import com.example.foodexpiryapp.presentation.viewmodel.ShoppingViewModel
import com.google.android.material.snackbar.Snackbar
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

    private lateinit var shoppingAdapter: ShoppingItemAdapter
    private lateinit var templateAdapter: ShoppingTemplateAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentShoppingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupRecyclerViews()
        setupQuickAdd()
        observeStats()
        observeShoppingItems()
        observeTemplates()
        observeInventoryStatus()

        analyticsRepository.trackEvent(
            AnalyticsEvent(
                eventName = "screen_view",
                eventType = EventType.SCREEN_VIEW,
                additionalData = mapOf("screen_name" to "shopping")
            )
        )
    }

    private fun setupRecyclerViews() {
        shoppingAdapter = ShoppingItemAdapter(
            onToggle = { viewModel.onToggleItem(it) },
            onDelete = { viewModel.onDeleteItem(it) }
        )
        binding.shoppingListRecyclerView.apply {
            adapter = shoppingAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }

        templateAdapter = ShoppingTemplateAdapter(
            onApplyTemplate = { template ->
                viewModel.applyTemplate(template)
                Snackbar.make(binding.root, "Added ${template.itemNames.size} items from ${template.name}", Snackbar.LENGTH_SHORT).show()
            }
        )
        binding.recyclerTemplates.apply {
            adapter = templateAdapter
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            addOnItemTouchListener(object : RecyclerView.OnItemTouchListener {
                private var initialX = 0f
                private var initialY = 0f
                private val touchSlop = ViewConfiguration.get(requireContext()).scaledTouchSlop

                override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
                    when (e.action) {
                        MotionEvent.ACTION_DOWN -> {
                            initialX = e.rawX
                            initialY = e.rawY
                            parent?.requestDisallowInterceptTouchEvent(false)
                        }
                        MotionEvent.ACTION_MOVE -> {
                            val dx = e.rawX - initialX
                            val dy = e.rawY - initialY
                            if (Math.abs(dx) > touchSlop && Math.abs(dx) > Math.abs(dy)) {
                                val canScrollHorizontally = if (dx < 0) {
                                    rv.canScrollHorizontally(1)
                                } else {
                                    rv.canScrollHorizontally(-1)
                                }
                                if (canScrollHorizontally) {
                                    parent?.requestDisallowInterceptTouchEvent(true)
                                }
                            }
                        }
                        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                            parent?.requestDisallowInterceptTouchEvent(false)
                        }
                    }
                    return false
                }
                override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) {}
                override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {}
            })
        }
    }

    private fun setupQuickAdd() {
        binding.btnAddQuick.setOnClickListener {
            val name = binding.editQuickAdd.text.toString().trim()
            if (name.isNotEmpty()) {
                viewModel.onAddItem(name)
                binding.editQuickAdd.text.clear()
            }
        }
    }

    private fun observeStats() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.weeklyStats.collect { stats ->
                    binding.textStatAdded.text = stats.itemsAdded.toString()
                    binding.textStatEaten.text = stats.itemsEaten.toString()
                    binding.textStatExpired.text = stats.itemsExpired.toString()
                }
            }
        }
    }

    private fun observeShoppingItems() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.shoppingItems.collect { items ->
                    shoppingAdapter.submitList(items)
                    binding.textEmpty.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
                    val uncheckedCount = items.count { !it.isChecked }
                    binding.textRemainingInfo.text = "$uncheckedCount item${if (uncheckedCount != 1) "s" else ""} remaining for your weekly pantry restock."
                }
            }
        }
    }

    private fun observeTemplates() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.templates.collect { templates ->
                    templateAdapter.submitList(templates)
                }
            }
        }
    }

    private fun observeInventoryStatus() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.inventoryItemNames.collect { names ->
                    shoppingAdapter.updateInventoryStatus(names)
                    shoppingAdapter.notifyDataSetChanged()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
