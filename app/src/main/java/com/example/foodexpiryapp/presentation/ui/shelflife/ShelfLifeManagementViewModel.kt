package com.example.foodexpiryapp.presentation.ui.shelflife

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodexpiryapp.data.local.database.ShelfLifeEntity
import com.example.foodexpiryapp.domain.repository.ShelfLifeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ShelfLifeManagementViewModel @Inject constructor(
    private val repository: ShelfLifeRepository
) : ViewModel() {

    sealed class Filter { data object All : Filter(); data object Auto : Filter(); data object Manual : Filter() }

    private val _filter = MutableStateFlow<Filter>(Filter.All)
    val filter: StateFlow<Filter> = _filter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")

    val entries: StateFlow<List<ShelfLifeEntity>> = combine(_filter, _searchQuery) { filter, query ->
        Pair(filter, query)
    }.flatMapLatest { (filter, query) ->
        val source = when (filter) {
            is Filter.All -> null
            is Filter.Auto -> "auto"
            is Filter.Manual -> "manual"
        }
        if (query.isBlank()) {
            if (source != null) repository.getAllBySource(source) else repository.getAll()
        } else {
            repository.searchByName(query)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _totalCount = MutableStateFlow(0)
    private val _autoCount = MutableStateFlow(0)
    private val _manualCount = MutableStateFlow(0)

    val statsText: StateFlow<String> = combine(_totalCount, _autoCount, _manualCount) { total, auto, manual ->
        "$total foods \u2022 $auto AI Learned \u2022 $manual Confirmed"
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    init {
        viewModelScope.launch {
            repository.ensureSeeded()
            refreshStats()
        }
    }

    fun setFilter(filter: Filter) {
        _filter.value = filter
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun confirmEntry(id: Long) {
        viewModelScope.launch {
            repository.confirmEntry(id)
            refreshStats()
        }
    }

    fun deleteEntry(id: Long) {
        viewModelScope.launch {
            repository.deleteEntry(id)
            refreshStats()
        }
    }

    fun addOrUpdateEntry(entity: ShelfLifeEntity) {
        viewModelScope.launch {
            repository.updateEntry(entity)
            refreshStats()
        }
    }

    fun addNewEntry(name: String, days: Int, category: String, location: String) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            repository.updateEntry(
                ShelfLifeEntity(
                    foodName = name.lowercase().trim(),
                    shelfLifeDays = days,
                    category = category,
                    location = location,
                    source = "manual",
                    hitCount = 0,
                    createdAt = now,
                    updatedAt = now
                )
            )
            refreshStats()
        }
    }

    private suspend fun refreshStats() {
        _totalCount.value = repository.count()
        _autoCount.value = repository.countBySource("auto")
        _manualCount.value = repository.countBySource("manual")
    }
}
