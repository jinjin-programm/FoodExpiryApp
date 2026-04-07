package com.example.foodexpiryapp.domain.usecase

import com.example.foodexpiryapp.domain.model.ShoppingItem
import com.example.foodexpiryapp.domain.model.ShoppingTemplate
import com.example.foodexpiryapp.domain.repository.ShoppingRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetAllShoppingItemsUseCase @Inject constructor(
    private val repository: ShoppingRepository
) {
    operator fun invoke(): Flow<List<ShoppingItem>> = repository.getAllShoppingItems()
}

class AddShoppingItemUseCase @Inject constructor(
    private val repository: ShoppingRepository
) {
    suspend operator fun invoke(name: String) = repository.addShoppingItem(name)
}

class ToggleShoppingItemUseCase @Inject constructor(
    private val repository: ShoppingRepository
) {
    suspend operator fun invoke(item: ShoppingItem) {
        repository.updateShoppingItem(item.copy(isChecked = !item.isChecked))
    }
}

class DeleteShoppingItemUseCase @Inject constructor(
    private val repository: ShoppingRepository
) {
    suspend operator fun invoke(item: ShoppingItem) = repository.deleteShoppingItem(item)
}

class ClearCompletedShoppingItemsUseCase @Inject constructor(
    private val repository: ShoppingRepository
) {
    suspend operator fun invoke() = repository.clearCompletedItems()
}

class GetAllShoppingTemplatesUseCase @Inject constructor(
    private val repository: ShoppingRepository
) {
    operator fun invoke(): Flow<List<ShoppingTemplate>> = repository.getAllTemplates()
}

class ApplyShoppingTemplateUseCase @Inject constructor(
    private val repository: ShoppingRepository
) {
    suspend operator fun invoke(template: ShoppingTemplate) = repository.applyTemplate(template)
}

class GetInventoryItemNamesUseCase @Inject constructor(
    private val repository: ShoppingRepository
) {
    operator fun invoke(): Flow<List<String>> = repository.getInventoryItemNames()
}
