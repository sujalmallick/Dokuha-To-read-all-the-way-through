package data.library

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

interface CategoryRepository {
    val categories: StateFlow<List<LibraryCategory>>
    val selectedCategory: StateFlow<String>
    val mangaCategoryMappings: StateFlow<Map<String, Set<String>>> // mangaId/url -> set of categoryIds

    fun selectCategory(categoryId: String)
    fun addCategory(name: String): Result<LibraryCategory>
    fun renameCategory(categoryId: String, newName: String): Result<Unit>
    fun deleteCategory(categoryId: String): Result<Unit>
    fun reorderCategories(orderedCategoryIds: List<String>)

    fun setMangaCategories(mangaId: String, categoryIds: Set<String>)
    fun batchAssignCategory(mangaIds: Set<String>, categoryId: String)
    fun batchRemoveFromCategory(mangaIds: Set<String>, categoryId: String)
}

class DefaultCategoryRepository : CategoryRepository {

    private val initialCategories = listOf(
        LibraryCategory(DefaultCategories.ALL, "All", 0, isDefault = true),
        LibraryCategory(DefaultCategories.READING, "Reading", 1, isDefault = true),
        LibraryCategory(DefaultCategories.PLAN_TO_READ, "Plan to Read", 2, isDefault = true),
        LibraryCategory(DefaultCategories.COMPLETED, "Completed", 3, isDefault = true),
        LibraryCategory(DefaultCategories.ON_HOLD, "On Hold", 4, isDefault = true),
        LibraryCategory("cat-favorites", "Favorites", 5, isDefault = false)
    )

    private val initialMappings = mapOf(
        "solo-leveling" to setOf(DefaultCategories.ALL, DefaultCategories.READING, "cat-favorites"),
        "aurora-blade" to setOf(DefaultCategories.ALL, DefaultCategories.READING),
        "wind-beneath" to setOf(DefaultCategories.ALL, DefaultCategories.READING),
        "shadow-crown" to setOf(DefaultCategories.ALL, DefaultCategories.PLAN_TO_READ),
        "neon-requiem" to setOf(DefaultCategories.ALL, DefaultCategories.COMPLETED),
        "frostbound" to setOf(DefaultCategories.ALL, DefaultCategories.ON_HOLD)
    )

    private val _categories = MutableStateFlow<List<LibraryCategory>>(initialCategories)
    override val categories: StateFlow<List<LibraryCategory>> = _categories.asStateFlow()

    private val _selectedCategory = MutableStateFlow<String>(DefaultCategories.ALL)
    override val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _mangaCategoryMappings = MutableStateFlow<Map<String, Set<String>>>(initialMappings)
    override val mangaCategoryMappings: StateFlow<Map<String, Set<String>>> = _mangaCategoryMappings.asStateFlow()

    override fun selectCategory(categoryId: String) {
        _selectedCategory.value = categoryId
    }

    override fun addCategory(name: String): Result<LibraryCategory> {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) {
            return Result.failure(IllegalArgumentException("Category name cannot be empty"))
        }
        if (_categories.value.any { it.name.equals(trimmed, ignoreCase = true) }) {
            return Result.failure(IllegalStateException("Category \"$trimmed\" already exists"))
        }

        val maxOrder = _categories.value.maxOfOrNull { it.order } ?: 0
        val newCategory = LibraryCategory(
            id = "cat-${System.currentTimeMillis()}",
            name = trimmed,
            order = maxOrder + 1,
            isDefault = false
        )

        _categories.update { it + newCategory }
        return Result.success(newCategory)
    }

    override fun renameCategory(categoryId: String, newName: String): Result<Unit> {
        val trimmed = newName.trim()
        if (trimmed.isEmpty()) {
            return Result.failure(IllegalArgumentException("Category name cannot be empty"))
        }
        val target = _categories.value.find { it.id == categoryId }
            ?: return Result.failure(NoSuchElementException("Category not found"))

        if (target.id == DefaultCategories.ALL) {
            return Result.failure(IllegalStateException("Cannot rename default \"All\" category"))
        }

        if (_categories.value.any { it.id != categoryId && it.name.equals(trimmed, ignoreCase = true) }) {
            return Result.failure(IllegalStateException("Another category with name \"$trimmed\" already exists"))
        }

        _categories.update { list ->
            list.map { if (it.id == categoryId) it.copy(name = trimmed) else it }
        }
        return Result.success(Unit)
    }

    override fun deleteCategory(categoryId: String): Result<Unit> {
        val target = _categories.value.find { it.id == categoryId }
            ?: return Result.failure(NoSuchElementException("Category not found"))

        if (target.id == DefaultCategories.ALL) {
            return Result.failure(IllegalStateException("Cannot delete default \"All\" category"))
        }

        // If currently selected, revert selection to ALL
        if (_selectedCategory.value == categoryId) {
            _selectedCategory.value = DefaultCategories.ALL
        }

        // Remove category
        _categories.update { list -> list.filter { it.id != categoryId } }

        // Remove mapping references
        _mangaCategoryMappings.update { currentMap ->
            currentMap.mapValues { (_, categorySet) ->
                categorySet - categoryId
            }
        }

        return Result.success(Unit)
    }

    override fun reorderCategories(orderedCategoryIds: List<String>) {
        _categories.update { currentList ->
            val listById = currentList.associateBy { it.id }
            val reordered = mutableListOf<LibraryCategory>()
            orderedCategoryIds.forEachIndexed { index, id ->
                listById[id]?.let {
                    reordered.add(it.copy(order = index))
                }
            }
            // Append any not explicitly in orderedCategoryIds
            val remaining = currentList.filter { it.id !in orderedCategoryIds }
            var nextIndex = reordered.size
            remaining.forEach {
                reordered.add(it.copy(order = nextIndex++))
            }
            reordered
        }
    }

    override fun setMangaCategories(mangaId: String, categoryIds: Set<String>) {
        // Ensure "All" is always present in categories mapping
        val finalCategories = categoryIds + DefaultCategories.ALL
        _mangaCategoryMappings.update { it + (mangaId to finalCategories) }
    }

    override fun batchAssignCategory(mangaIds: Set<String>, categoryId: String) {
        _mangaCategoryMappings.update { currentMap ->
            val updated = currentMap.toMutableMap()
            for (id in mangaIds) {
                val existing = updated[id] ?: setOf(DefaultCategories.ALL)
                updated[id] = existing + categoryId
            }
            updated
        }
    }

    override fun batchRemoveFromCategory(mangaIds: Set<String>, categoryId: String) {
        if (categoryId == DefaultCategories.ALL) return // Cannot remove from All
        _mangaCategoryMappings.update { currentMap ->
            val updated = currentMap.toMutableMap()
            for (id in mangaIds) {
                val existing = updated[id] ?: setOf(DefaultCategories.ALL)
                updated[id] = existing - categoryId
            }
            updated
        }
    }
}
