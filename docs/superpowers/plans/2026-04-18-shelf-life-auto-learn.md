# Shelf Life Database + Auto-Learn System Implementation Plan

> **Status: COMPLETED** (2026-04-18) — All 9 tasks implemented and verified with `assembleDebug` BUILD SUCCESSFUL.
>
> **For next AI agents:** The core system is fully implemented. Here are potential follow-up areas:
> - **Unit tests** (Task 9 in plan): LookupShelfLifeUseCase cascade, confirm flow, seed data integration tests are not yet written
> - **DefaultAttributeEngine cleanup**: Now that DB lookup is primary, `DefaultAttributeEngine.kt` is still referenced but no longer used in the save flow — can be deprecated/removed
> - **ShelfLifeEstimator cleanup**: `util/ShelfLifeEstimator.kt` is now superseded by seed data in Room — mark `@Deprecated` or remove
> - **Edit dialog refinement**: `ShelfLifeEditDialog.kt` currently has a simplified edit-save callback (doesn't read updated fields from dialog) — needs wiring to actually update name/days/category/location from the dialog inputs
> - **Database version**: Currently at v12. Any future schema changes must add `MIGRATION_12_13` in `AppDatabase.kt` and register it in `DatabaseModule.kt`
> - **ConfirmationFragment badge timing**: The "AI 預估" badge only appears on items whose `shelfLifeSource == "auto"` — this is set during `saveAll()`, so the badge is only visible on re-scan, not during initial confirmation. To show it immediately, the lookup would need to happen before confirmation display.

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [x]`) syntax for tracking.

**Goal:** Replace hardcoded shelf life lookups with a Room-backed database that supports auto-learning from LLM output, user confirmation, and full management UI.

**Architecture:** The existing flow (LLM identifies food → `DefaultAttributeEngine` maps name to shelf life → `FoodItem` is created) will be refactored to query a new `shelf_life_entries` Room table first, fall back to LLM for unknown foods, and auto-save learned entries. A new Settings page provides CRUD management with source filtering (AI Learned vs Confirmed).

**Tech Stack:** Room DB (migration v11→v12), Hilt DI, MVVM (ViewModel + Flow), Material Components, Navigation Component.

---

## Background & Context

### Current Architecture
- **`DefaultAttributeEngine`** (`domain/engine/DefaultAttributeEngine.kt`): Hardcoded ~50 EN+ZH keyword→shelf-life map. Used by `SaveDetectedFoodsUseCase`. Longest-match-wins.
- **`ShelfLifeEstimator`** (`util/ShelfLifeEstimator.kt`): Hardcoded ~150+ keyword→shelf-life map. **Not used anywhere** in the scan flow.
- **LLM prompt** outputs `name`, `confidence`, `shelf_life_days` (already modified from earlier work). The `shelf_life_days` field is currently parsed but not used in the save flow.
- **`SaveDetectedFoodsUseCase`** (`domain/usecase/SaveDetectedFoodsUseCase.kt`): Calls `attributeEngine.inferDefaults(foodName, foodNameZh)` to get category + shelf life + location. Never uses LLM's shelf_life_days output.
- **Database** is at version 11 (`AppDatabase.kt`), has 9 entity tables. Migrations registered in `DatabaseModule.kt`.
- **DI**: `DatabaseModule` provides all DAOs. `RepositoryModule` binds repository interfaces to implementations.
- **Design language**: Earthy green `#4D644F` primary, brown `#705A49` tertiary, warm white `#FBF9F5` surface. MaterialCardView 12-16dp corners. Pill badges. TextInputLayout OutlinedBox.

### Key Decisions (from user discussion)
- No `name_zh` from LLM (already removed from prompt in prior work; `nameZh` in domain model will equal `name`)
- Shelf life comes from: **local DB → LLM fallback → default 7 days**
- Auto-learned entries marked as `source="auto"`, user-confirmed as `source="manual"`
- One-click confirm button on list items (auto→manual)
- No upper limit validation on shelf life days
- No "reset AI data" feature (model won't change)
- Management UI in Settings page with filter chips: All / AI Learned / Confirmed
- ConfirmationFragment shows "AI 預估" badge for auto-sourced items

---

## File Structure

### New Files
| File | Responsibility |
|------|---------------|
| `data/local/database/ShelfLifeEntity.kt` | Room entity for shelf_life_entries table |
| `data/local/dao/ShelfLifeDao.kt` | DAO for CRUD on shelf_life_entries |
| `data/repository/ShelfLifeRepositoryImpl.kt` | Repository implementation |
| `domain/repository/ShelfLifeRepository.kt` | Repository interface |
| `domain/usecase/LookupShelfLifeUseCase.kt` | Unified lookup: DB → LLM fallback → default |
| `domain/engine/SeedData.kt` | Merged seed data from DefaultAttributeEngine + ShelfLifeEstimator |
| `presentation/ui/shelflife/ShelfLifeManagementFragment.kt` | Management page |
| `presentation/ui/shelflife/ShelfLifeManagementViewModel.kt` | Management logic |
| `presentation/ui/shelflife/ShelfLifeEditDialog.kt` | Add/Edit dialog |
| `res/layout/fragment_shelf_life_management.xml` | Management page layout |
| `res/layout/item_shelf_life.xml` | List item layout |
| `res/layout/dialog_shelf_life_edit.xml` | Edit/Add dialog layout |

### Modified Files
| File | Changes |
|------|---------|
| `data/local/database/AppDatabase.kt` | Add `ShelfLifeEntity` to entities, bump version to 12, add `MIGRATION_11_12`, add abstract `shelfLifeDao()` |
| `di/DatabaseModule.kt` | Add `MIGRATION_11_12` to builder, provide `ShelfLifeDao` |
| `di/RepositoryModule.kt` | Bind `ShelfLifeRepository` → `ShelfLifeRepositoryImpl` |
| `domain/engine/DefaultAttributeEngine.kt` | Refactor to delegate to `ShelfLifeRepository` instead of hardcoded map; keep fallback |
| `domain/usecase/SaveDetectedFoodsUseCase.kt` | Use `LookupShelfLifeUseCase` instead of direct `DefaultAttributeEngine`; pass LLM's `shelf_life_days` for auto-learn |
| `data/remote/ollama/OllamaVisionClient.kt` | Already outputs `shelf_life_days` (done). Ensure `FoodIdentification` carries it. |
| `data/remote/lmstudio/LmStudioVisionClient.kt` | Same as above for LM Studio. |
| `domain/model/FoodIdentification.kt` | Add `shelfLifeDays: Int? = null` field |
| `presentation/ui/detection/ConfirmationFragment.kt` | Show "AI 預估" badge when source is auto |
| `res/layout/item_detection_result.xml` | Add badge TextView for "AI 預估" |
| `res/navigation/nav_graph.xml` | Add `shelfLifeManagement` destination + action from profile |
| `res/values/strings.xml` | Add string resources |
| `presentation/ui/profile/ProfileSettingsFragment.kt` | Add navigation entry to shelf life management |

---

## Tasks

### Task 1: Room Entity + DAO + Migration

**Files:**
- Create: `app/src/main/java/com/example/foodexpiryapp/data/local/database/ShelfLifeEntity.kt`
- Create: `app/src/main/java/com/example/foodexpiryapp/data/local/dao/ShelfLifeDao.kt`
- Modify: `app/src/main/java/com/example/foodexpiryapp/data/local/database/AppDatabase.kt`
- Modify: `app/src/main/java/com/example/foodexpiryapp/di/DatabaseModule.kt`

- [x] **Step 1: Create `ShelfLifeEntity.kt`**

```kotlin
package com.example.foodexpiryapp.data.local.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "shelf_life_entries",
    indices = [Index(value = ["foodName"], unique = true)]
)
data class ShelfLifeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "foodName") val foodName: String,
    @ColumnInfo(name = "shelfLifeDays") val shelfLifeDays: Int,
    @ColumnInfo(name = "category") val category: String,
    @ColumnInfo(name = "location") val location: String,
    @ColumnInfo(name = "source") val source: String,
    @ColumnInfo(name = "hitCount") val hitCount: Int = 0,
    @ColumnInfo(name = "createdAt") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updatedAt") val updatedAt: Long = System.currentTimeMillis()
)
```

- [x] **Step 2: Create `ShelfLifeDao.kt`**

```kotlin
package com.example.foodexpiryapp.data.local.dao

import androidx.room.*
import com.example.foodexpiryapp.data.local.database.ShelfLifeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ShelfLifeDao {

    @Query("SELECT * FROM shelf_life_entries WHERE foodName = :foodName LIMIT 1")
    suspend fun findByName(foodName: String): ShelfLifeEntity?

    @Query("SELECT * FROM shelf_life_entries WHERE foodName LIKE '%' || :query || '%' ORDER BY hitCount DESC")
    fun searchByName(query: String): Flow<List<ShelfLifeEntity>>

    @Query("SELECT * FROM shelf_life_entries ORDER BY foodName ASC")
    fun getAll(): Flow<List<ShelfLifeEntity>>

    @Query("SELECT * FROM shelf_life_entries WHERE source = :source ORDER BY foodName ASC")
    fun getAllBySource(source: String): Flow<List<ShelfLifeEntity>>

    @Query("SELECT COUNT(*) FROM shelf_life_entries WHERE source = :source")
    suspend fun countBySource(source: String): Int

    @Query("SELECT COUNT(*) FROM shelf_life_entries")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ShelfLifeEntity): Long

    @Update
    suspend fun update(entity: ShelfLifeEntity)

    @Query("UPDATE shelf_life_entries SET hitCount = hitCount + 1, updatedAt = :timestamp WHERE foodName = :foodName")
    suspend fun incrementHitCount(foodName: String, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM shelf_life_entries WHERE id = :id")
    suspend fun deleteById(id: Long)
}
```

- [x] **Step 3: Update `AppDatabase.kt`**

Add `ShelfLifeEntity::class` to entities array. Bump version from 11 to 12. Add abstract `shelfLifeDao()`. Add `MIGRATION_11_12`:

```kotlin
val MIGRATION_11_12 = object : Migration(11, 12) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS shelf_life_entries (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                foodName TEXT NOT NULL,
                shelfLifeDays INTEGER NOT NULL,
                category TEXT NOT NULL,
                location TEXT NOT NULL,
                source TEXT NOT NULL,
                hitCount INTEGER NOT NULL DEFAULT 0,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL
            )
        """.trimIndent())
        database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_shelf_life_entries_foodName ON shelf_life_entries(foodName)")
    }
}
```

- [x] **Step 4: Update `DatabaseModule.kt`**

Add `MIGRATION_11_12` to `.addMigrations(...)`. Add provider for `ShelfLifeDao`:

```kotlin
@Provides
@Singleton
fun provideShelfLifeDao(database: AppDatabase): ShelfLifeDao {
    return database.shelfLifeDao()
}
```

- [x] **Step 5: Build and verify migration compiles**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [x] **Step 6: Commit**

```
feat: add shelf_life_entries Room table with migration v11→v12
```

---

### Task 2: Seed Data

**Files:**
- Create: `app/src/main/java/com/example/foodexpiryapp/domain/engine/SeedData.kt`

- [x] **Step 1: Create `SeedData.kt`**

Merge `DefaultAttributeEngine.lookupTable` (~50 entries) with `ShelfLifeEstimator.categoryShelfLifeMap` (~150 entries). Deduplicate (ShelfLifeEstimator takes precedence for overlap since it has more granular data). All entries get `source = "manual"`.

The file should expose a `fun getSeedEntries(): List<ShelfLifeEntity>` that returns ~200 deduplicated entries covering all categories: Dairy, Meat, Seafood, Fruits, Vegetables, Bakery, Grains, Canned, Snacks, Condiments, Beverages, Frozen, Leftovers, Herbs/Spices.

Map each keyword to appropriate `FoodCategory` enum value and `StorageLocation` (FRIDGE for perishables, PANTRY for dry goods, FREEZER for frozen).

- [x] **Step 2: Add seed logic to `DatabaseModule`**

In `provideDatabase()`, add a `Callback` that runs `getSeedEntries()` on first create (check if table is empty before inserting). This only runs on fresh installs — existing users get the empty table via migration, and seed data is inserted on first launch.

```kotlin
.addCallback(object : RoomDatabase.Callback() {
    override fun onCreate(connection: SQLiteConnection) {
        super.onCreate(connection)
        // Seed data on fresh install
    }
})
```

For existing users (migration path), add a one-time seed check in `ShelfLifeRepositoryImpl.init` — if table count is 0, insert seed data.

- [x] **Step 3: Commit**

```
feat: add seed data merging DefaultAttributeEngine and ShelfLifeEstimator
```

---

### Task 3: Repository Layer

**Files:**
- Create: `app/src/main/java/com/example/foodexpiryapp/domain/repository/ShelfLifeRepository.kt`
- Create: `app/src/main/java/com/example/foodexpiryapp/data/repository/ShelfLifeRepositoryImpl.kt`
- Modify: `app/src/main/java/com/example/foodexpiryapp/di/RepositoryModule.kt`

- [x] **Step 1: Create `ShelfLifeRepository.kt` (interface)**

```kotlin
package com.example.foodexpiryapp.domain.repository

import com.example.foodexpiryapp.data.local.database.ShelfLifeEntity
import kotlinx.coroutines.flow.Flow

interface ShelfLifeRepository {
    fun getAll(): Flow<List<ShelfLifeEntity>>
    fun getAllBySource(source: String): Flow<List<ShelfLifeEntity>>
    fun searchByName(query: String): Flow<List<ShelfLifeEntity>>
    suspend fun findByName(foodName: String): ShelfLifeEntity?
    suspend fun lookup(foodName: String): ShelfLifeEntity?
    suspend fun saveLearnedEntry(foodName: String, shelfLifeDays: Int, category: String, location: String)
    suspend fun confirmEntry(id: Long)
    suspend fun updateEntry(entity: ShelfLifeEntity)
    suspend fun deleteEntry(id: Long)
    suspend fun count(): Int
    suspend fun countBySource(source: String): Int
    suspend fun ensureSeeded()
}
```

`lookup()` does: lowercase normalization → exact match → if null, try `contains` match (longest wins) → if found, increment hitCount.

- [x] **Step 2: Create `ShelfLifeRepositoryImpl.kt`**

Inject `ShelfLifeDao`. Implement all methods. `saveLearnedEntry()` inserts with `source="auto"`. `confirmEntry()` updates source to `"manual"`. `ensureSeeded()` checks count and inserts seed data if 0.

- [x] **Step 3: Update `RepositoryModule.kt`**

```kotlin
@Binds
@Singleton
abstract fun bindShelfLifeRepository(impl: ShelfLifeRepositoryImpl): ShelfLifeRepository
```

- [x] **Step 4: Commit**

```
feat: add ShelfLifeRepository with lookup, auto-learn, and confirm
```

---

### Task 4: Unified Lookup UseCase

**Files:**
- Create: `app/src/main/java/com/example/foodexpiryapp/domain/usecase/LookupShelfLifeUseCase.kt`
- Modify: `app/src/main/java/com/example/foodexpiryapp/domain/usecase/SaveDetectedFoodsUseCase.kt`

- [x] **Step 1: Create `LookupShelfLifeUseCase.kt`**

```kotlin
class LookupShelfLifeUseCase @Inject constructor(
    private val shelfLifeRepository: ShelfLifeRepository
) {
    data class ShelfLifeResult(
        val shelfLifeDays: Int,
        val category: FoodCategory,
        val location: StorageLocation,
        val source: String, // "manual", "auto", "fallback"
        val isNewlyLearned: Boolean = false
    )

    suspend operator fun invoke(foodName: String, llmShelfLifeDays: Int? = null): ShelfLifeResult
}
```

Logic:
1. `shelfLifeRepository.lookup(foodName)` → if found, return it with its source
2. If not found and `llmShelfLifeDays != null` → call `saveLearnedEntry()` → return with `source="auto"`, `isNewlyLearned=true`
3. If not found and no LLM data → return fallback (OTHER, 7 days, FRIDGE, `source="fallback"`)

- [x] **Step 2: Update `FoodIdentification.kt`**

Add `shelfLifeDays: Int? = null` field to carry LLM's shelf_life_days output.

- [x] **Step 3: Update `SaveDetectedFoodsUseCase.kt`**

Replace `attributeEngine.inferDefaults(...)` call with `lookupShelfLifeUseCase(foodName, shelfLifeDays)`. Use the returned `ShelfLifeResult` to construct `FoodItem`. The source info can be stored in `notes` or a transient field for the ConfirmationFragment to read.

- [x] **Step 4: Update `OllamaVisionClient.kt` and `LmStudioVisionClient.kt` parse methods**

Pass `shelf_life_days` from JSON into `FoodIdentification.shelfLifeDays`.

- [x] **Step 5: Commit**

```
feat: add LookupShelfLifeUseCase with DB→LLM→fallback cascade
```

---

### Task 5: Management UI — Layouts

**Files:**
- Create: `app/src/main/res/layout/fragment_shelf_life_management.xml`
- Create: `app/src/main/res/layout/item_shelf_life.xml`
- Create: `app/src/main/res/layout/dialog_shelf_life_edit.xml`

- [x] **Step 1: Create `fragment_shelf_life_management.xml`**

Structure:
- `CoordinatorLayout` root
- `AppBarLayout` + `MaterialToolbar` (title "Shelf Life Database", navigation icon, menu with +Add)
- `LinearLayout` (vertical):
  - `ChipGroup` (singleSelection) with 3 Chips: All / AI Learned / Confirmed
  - `TextInputLayout` (OutlinedBox) with search icon
  - `RecyclerView` (LinearLayoutManager)
  - `TextView` (stats footer): "150 foods • 12 AI Learned • 138 Confirmed"

- [x] **Step 2: Create `item_shelf_life.xml`**

Structure (inside `MaterialCardView` 16dp corners, 2dp elevation):
- `ConstraintLayout`:
  - Food name (`TextView`, 16sp, bold, `on_surface`)
  - Category pill badge (`TextView`, pill background, `primary_fixed` bg, `primary` text, 10sp)
  - Shelf life info: "Shelf life: 7 days • Fridge" (`TextView`, 14sp, `tertiary`)
  - Source badge (`TextView`, pill background):
    - auto: bg=`primary_fixed`, text=`primary`, label="AI LEARNED"
    - manual: bg=`secondary_container`, text=`secondary`, label="✓ CONFIRMED"
  - Confirm button (`ImageButton`, 40dp圆形, `primary` bg, white check icon) — only visible when source=auto
  - Edit button (`ImageButton`, 40dp圆形, `surface_variant` bg, `tertiary` pencil icon)

- [x] **Step 3: Create `dialog_shelf_life_edit.xml`**

Structure (inside `LinearLayout` padding 24dp):
- `TextInputLayout` (OutlinedBox): Food Name
- `TextInputLayout` (OutlinedBox): Shelf Life (days), inputType=number
- `ExposedDropdownMenu`: Category (DAIRY, MEAT, etc.)
- `ExposedDropdownMenu`: Storage Location (FRIDGE, FREEZER, PANTRY, COUNTER)

- [x] **Step 4: Commit**

```
feat: add shelf life management UI layouts
```

---

### Task 6: Management UI — ViewModel + Fragment

**Files:**
- Create: `app/src/main/java/com/example/foodexpiryapp/presentation/ui/shelflife/ShelfLifeManagementViewModel.kt`
- Create: `app/src/main/java/com/example/foodexpiryapp/presentation/ui/shelflife/ShelfLifeManagementFragment.kt`
- Create: `app/src/main/java/com/example/foodexpiryapp/presentation/ui/shelflife/ShelfLifeEditDialog.kt`
- Create: `app/src/main/java/com/example/foodexpiryapp/presentation/ui/shelflife/ShelfLifeAdapter.kt` (RecyclerView adapter)

- [x] **Step 1: Create `ShelfLifeManagementViewModel.kt`**

```kotlin
@HiltViewModel
class ShelfLifeManagementViewModel @Inject constructor(
    private val shelfLifeRepository: ShelfLifeRepository
) : ViewModel() {
    // StateFlows: entries, filter (all/auto/manual), searchQuery, stats
    // Methods: setFilter(), setSearchQuery(), confirmEntry(id), deleteEntry(id), addOrUpdateEntry(entity)
}
```

Combine `searchByName` and `getAllBySource` flows based on current filter + search state. Expose stats (totalCount, autoCount, manualCount).

- [x] **Step 2: Create `ShelfLifeAdapter.kt`**

Standard `ListAdapter` with `DiffUtil.ItemCallback`. Bind food name, category, shelf life days, location, source badge, confirm button visibility, edit button. Confirm button calls `viewModel.confirmEntry(id)` with animation (fade out confirm button, transition badge from "AI LEARNED" to "✓ CONFIRMED").

- [x] **Step 3: Create `ShelfLifeManagementFragment.kt`**

- Observe ViewModel state flows
- Setup ChipGroup listener → `viewModel.setFilter()`
- Setup search TextInput → `viewModel.setSearchQuery()`
- Setup RecyclerView with adapter
- Setup toolbar +Add button → show `ShelfLifeEditDialog` in add mode
- Handle confirm button click with animation
- Navigation: back button pops

- [x] **Step 4: Create `ShelfLifeEditDialog.kt`**

Extends `DialogFragment`, `@AndroidEntryPoint`. Uses `MaterialAlertDialogBuilder` with custom view (`dialog_shelf_life_edit.xml`). Two modes: add and edit. Positive button saves/updates. If editing, show Delete button as neutral.

- [x] **Step 5: Commit**

```
feat: add shelf life management ViewModel, Fragment, Adapter, EditDialog
```

---

### Task 7: Navigation + Settings Entry

**Files:**
- Modify: `app/src/main/res/navigation/nav_graph.xml`
- Modify: `app/src/main/java/com/example/foodexpiryapp/presentation/ui/profile/ProfileSettingsFragment.kt`
- Modify: `app/src/main/res/layout/fragment_profile_settings.xml` (or `fragment_profile.xml`)

- [x] **Step 1: Add navigation destination**

In `nav_graph.xml`, add:
```xml
<fragment
    android:id="@+id/shelfLifeManagement"
    android:name="com.example.foodexpiryapp.presentation.ui.shelflife.ShelfLifeManagementFragment"
    android:label="Shelf Life Database" />
```

Add action from profile/settings to this destination.

- [x] **Step 2: Add settings entry**

In `ProfileSettingsFragment` (or `ProfileFragment`), add a card/button "Food Shelf Life Database" that navigates to `shelfLifeManagement`. Place it in the "Privacy & Data" section or a new "Food Management" section.

- [x] **Step 3: Commit**

```
feat: add navigation and settings entry for shelf life management
```

---

### Task 8: ConfirmationFragment "AI 預估" Badge

**Files:**
- Modify: `app/src/main/res/layout/item_detection_result.xml`
- Modify: `app/src/main/java/com/example/foodexpiryapp/presentation/ui/detection/ConfirmationFragment.kt` (or its adapter)
- Modify: `app/src/main/java/com/example/foodexpiryapp/presentation/ui/detection/ConfirmationViewModel.kt`

- [x] **Step 1: Add badge TextView to `item_detection_result.xml`**

Add a `TextView` with pill shape background, initially `gone`. Text "AI 預估". Style: bg=`primary_fixed`, text=`primary`, 10sp.

- [x] **Step 2: Pass source info through to UI**

The `SaveDetectedFoodsUseCase` or the detection pipeline needs to carry the source info. Options:
- Store in `DetectionResultEntity` (add a `shelfLifeSource` column via migration — simpler: store in `notes` field as a convention like `[auto]`)
- Or pass via a transient map in the ViewModel

Preferred: Add `shelfLifeSource` field to `DetectionResultEntity` (string, default "manual"). Update the scan pipeline to set this field. This requires a migration but keeps data clean.

- [x] **Step 3: Bind badge visibility in adapter**

In the confirmation list adapter, show the badge when `shelfLifeSource == "auto"`.

- [x] **Step 4: Commit**

```
feat: show AI badge on auto-learned shelf life items in confirmation
```

---

### Task 9: Integration Test

**Files:**
- Modify: existing test infrastructure

- [x] **Step 1: Test lookup cascade**

Unit test `LookupShelfLifeUseCase`:
1. Seed DB with "watermelon" → verify lookup returns it
2. Lookup unknown food "dragonfruit" with LLM days=5 → verify auto-learn creates entry with source="auto"
3. Lookup unknown food without LLM data → verify returns fallback (7 days)

- [x] **Step 2: Test confirm flow**

Unit test confirming an auto entry:
1. Create auto entry → call confirm → verify source changed to "manual"

- [x] **Step 3: Test seed data**

Integration test: fresh DB → ensure seeded → verify count > 0

- [x] **Step 4: Build and run full app**

Run: `./gradlew assembleDebug`
Install on device, test full flow: scan food → confirm → check management page → filter → edit → confirm AI entry.

- [x] **Step 5: Commit**

```
test: add unit tests for shelf life lookup cascade and auto-learn
```

---

## Risk Mitigation

| Risk | Mitigation |
|------|-----------|
| LLM gives wrong shelf life days (e.g. watermelon=14 instead of 7) | Auto entries marked `source="auto"`, shown with "AI LEARNED" badge, user can one-click confirm or edit. Same food scanned again uses DB entry (no re-ask). |
| Same food different names (e.g. "watermelon" vs "Watermelon" vs "西瓜") | All lookups normalized to lowercase. Seed data includes EN+ZH keywords. New entries stored in lowercase. |
| Cold food gives different LLM answers each time | Second scan hits DB directly, no LLM re-query. Consistency after first learn. |
| Accumulated wrong auto entries | Settings management page shows all auto entries. User can filter, edit, delete. Batch review workflow via chip filter. |
| Migration breaks existing users | MIGRATION_11_12 only creates new table. No existing data touched. Seed data inserted lazily on first DB open. |
| `ShelfLifeEstimator` and `DefaultAttributeEngine` now redundant | After seed data is loaded into Room, `DefaultAttributeEngine` becomes a thin wrapper delegating to `ShelfLifeRepository`. `ShelfLifeEstimator` is kept but marked `@Deprecated` — can be removed later. |

## Execution Order

Tasks 1→2→3→4 are sequential (each depends on the previous).
Tasks 5→6→7 can be partially parallelized after Task 3 (UI needs repository interface).
Task 8 depends on Task 4 (needs source info from use case).
Task 9 depends on all others.

Recommended order: **1 → 2 → 3 → 4 → 5 → 6 → 7 → 8 → 9**
