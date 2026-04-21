package com.example.foodexpiryapp.data.repository

import com.example.foodexpiryapp.data.local.dao.ShelfLifeDao
import com.example.foodexpiryapp.data.local.database.ShelfLifeEntity
import com.example.foodexpiryapp.domain.engine.SeedData
import com.example.foodexpiryapp.domain.repository.ShelfLifeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShelfLifeRepositoryImpl @Inject constructor(
    private val dao: ShelfLifeDao
) : ShelfLifeRepository {

    @Volatile
    private var seeded = false
    private val seedMutex = Mutex()

    override fun getAll(): Flow<List<ShelfLifeEntity>> = dao.getAll()

    override fun getAllBySource(source: String): Flow<List<ShelfLifeEntity>> = dao.getAllBySource(source)

    override fun searchByName(query: String): Flow<List<ShelfLifeEntity>> = dao.searchByName(query)

    override suspend fun findByName(foodName: String): ShelfLifeEntity? = dao.findByName(foodName)

    override suspend fun lookup(foodName: String): ShelfLifeEntity? {
        val normalized = foodName.lowercase().trim()
        val exact = dao.findByName(normalized)
        if (exact != null) {
            dao.incrementHitCount(normalized)
            return exact
        }
        val all = dao.getAllSync()
        val matches = all.filter { normalized.contains(it.foodName) || it.foodName.contains(normalized) }
        val best = matches.maxByOrNull { it.foodName.length }
        if (best != null) {
            dao.incrementHitCount(best.foodName)
            return best
        }
        return null
    }

    override suspend fun saveLearnedEntry(foodName: String, shelfLifeDays: Int, category: String, location: String) {
        val normalized = foodName.lowercase().trim()
        val now = System.currentTimeMillis()
        dao.insert(
            ShelfLifeEntity(
                foodName = normalized,
                shelfLifeDays = shelfLifeDays,
                category = category,
                location = location,
                source = "auto",
                hitCount = 1,
                createdAt = now,
                updatedAt = now
            )
        )
    }

    override suspend fun confirmEntry(id: Long) {
        val entity = dao.findById(id) ?: return
        dao.update(entity.copy(source = "manual", updatedAt = System.currentTimeMillis()))
    }

    override suspend fun updateEntry(entity: ShelfLifeEntity) {
        dao.update(entity.copy(updatedAt = System.currentTimeMillis()))
    }

    override suspend fun deleteEntry(id: Long) {
        dao.deleteById(id)
    }

    override suspend fun count(): Int = dao.count()

    override suspend fun countBySource(source: String): Int = dao.countBySource(source)

    override suspend fun ensureSeeded() {
        if (seeded) return
        seedMutex.withLock {
            if (seeded) return
            if (dao.count() > 0) {
                seeded = true
                return
            }
            val entries = SeedData.getSeedEntries()
            dao.insertAll(entries)
            seeded = true
        }
    }
}
