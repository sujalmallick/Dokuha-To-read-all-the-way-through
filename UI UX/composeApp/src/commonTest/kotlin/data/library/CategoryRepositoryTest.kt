package data.library

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class CategoryRepositoryTest {

    @Test
    fun testInitialCategories() {
        val repo = DefaultCategoryRepository()
        val list = repo.categories.value

        assertTrue(list.isNotEmpty())
        val allCat = list.find { it.id == DefaultCategories.ALL }
        assertNotNull(allCat)
        assertTrue(allCat.isDefault)

        val readingCat = list.find { it.id == DefaultCategories.READING }
        assertNotNull(readingCat)
        assertEquals("Reading", readingCat.name)
    }

    @Test
    fun testAddCategory() {
        val repo = DefaultCategoryRepository()
        val result = repo.addCategory("Shonen Action")
        assertTrue(result.isSuccess)

        val newCat = result.getOrThrow()
        assertEquals("Shonen Action", newCat.name)
        assertFalse(newCat.isDefault)

        val list = repo.categories.value
        assertTrue(list.any { it.id == newCat.id })
    }

    @Test
    fun testAddDuplicateCategoryFails() {
        val repo = DefaultCategoryRepository()
        val result1 = repo.addCategory("Isekai")
        assertTrue(result1.isSuccess)

        val result2 = repo.addCategory("Isekai")
        assertTrue(result2.isFailure)

        val result3 = repo.addCategory("isekai")
        assertTrue(result3.isFailure)
    }

    @Test
    fun testAddEmptyCategoryFails() {
        val repo = DefaultCategoryRepository()
        val result = repo.addCategory("   ")
        assertTrue(result.isFailure)
    }

    @Test
    fun testRenameCategory() {
        val repo = DefaultCategoryRepository()
        val addRes = repo.addCategory("Old Name")
        val cat = addRes.getOrThrow()

        val renameRes = repo.renameCategory(cat.id, "New Name")
        assertTrue(renameRes.isSuccess)

        val updated = repo.categories.value.find { it.id == cat.id }
        assertNotNull(updated)
        assertEquals("New Name", updated.name)
    }

    @Test
    fun testCannotRenameAllCategory() {
        val repo = DefaultCategoryRepository()
        val renameRes = repo.renameCategory(DefaultCategories.ALL, "Everything")
        assertTrue(renameRes.isFailure)
    }

    @Test
    fun testDeleteCategory() {
        val repo = DefaultCategoryRepository()
        val addRes = repo.addCategory("Temporary")
        val cat = addRes.getOrThrow()

        repo.batchAssignCategory(setOf("manga-1", "manga-2"), cat.id)
        assertTrue(repo.mangaCategoryMappings.value["manga-1"]?.contains(cat.id) == true)

        val deleteRes = repo.deleteCategory(cat.id)
        assertTrue(deleteRes.isSuccess)
        assertFalse(repo.categories.value.any { it.id == cat.id })

        // Association should be removed
        assertFalse(repo.mangaCategoryMappings.value["manga-1"]?.contains(cat.id) == true)
    }

    @Test
    fun testCannotDeleteAllCategory() {
        val repo = DefaultCategoryRepository()
        val deleteRes = repo.deleteCategory(DefaultCategories.ALL)
        assertTrue(deleteRes.isFailure)
        assertTrue(repo.categories.value.any { it.id == DefaultCategories.ALL })
    }

    @Test
    fun testReorderCategories() {
        val repo = DefaultCategoryRepository()
        val initialList = repo.categories.value.map { it.id }
        val reversed = initialList.reversed()

        repo.reorderCategories(reversed)
        val afterReorder = repo.categories.value.sortedBy { it.order }.map { it.id }
        assertEquals(reversed, afterReorder)
    }

    @Test
    fun testBatchAssignAndRemoveCategories() {
        val repo = DefaultCategoryRepository()
        val targetCatId = DefaultCategories.PLAN_TO_READ
        val mangaSet = setOf("solo-leveling", "aurora-blade")

        repo.batchAssignCategory(mangaSet, targetCatId)
        assertTrue(repo.mangaCategoryMappings.value["solo-leveling"]?.contains(targetCatId) == true)
        assertTrue(repo.mangaCategoryMappings.value["aurora-blade"]?.contains(targetCatId) == true)

        repo.batchRemoveFromCategory(mangaSet, targetCatId)
        assertFalse(repo.mangaCategoryMappings.value["solo-leveling"]?.contains(targetCatId) == true)
        assertFalse(repo.mangaCategoryMappings.value["aurora-blade"]?.contains(targetCatId) == true)
    }
}
