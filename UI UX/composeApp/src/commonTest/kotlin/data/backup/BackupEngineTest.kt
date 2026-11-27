package data.backup

import data.library.DefaultCategories
import data.library.DefaultCategoryRepository
import data.mock.FakePanelRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class BackupEngineTest {

    @Test
    fun importJsonRemapsCustomCategoryIdsToNewlyCreatedIds() = runTest {
        val repo = FakePanelRepository()
        val categoryRepo = DefaultCategoryRepository()

        val backupJson = """
            {
              "schema_version": 2,
              "created_at": 1700000000,
              "library": [
                {"url": "/manga/remapped-1", "title": "Remapped Title", "source_id": 1}
              ],
              "categories": [
                {"id": "legacy-cat-999", "name": "Dark Fantasy", "order": 10}
              ],
              "category_mappings": {
                "/manga/remapped-1": ["legacy-cat-999", "unknown-stale-id"]
              }
            }
        """.trimIndent()

        val importedCount = BackupEngine.importJson(backupJson, repo, categoryRepo)
        assertEquals(1, importedCount)

        val createdCat = categoryRepo.categories.value.find { it.name == "Dark Fantasy" }
        assertNotNull(createdCat, "Expected custom category 'Dark Fantasy' to be created")
        assertFalse(createdCat.id == "legacy-cat-999")

        val assigned = categoryRepo.mangaCategoryMappings.value["/manga/remapped-1"].orEmpty()
        assertEquals(setOf(createdCat.id, DefaultCategories.ALL), assigned)
    }

    @Test
    fun importJsonMatchesExistingCategoriesCaseInsensitivelyAndPreservesDefaults() = runTest {
        val repo = FakePanelRepository()
        val categoryRepo = DefaultCategoryRepository()

        val backupJson = """
            {
              "schema_version": 2,
              "created_at": 1700000000,
              "library": [
                {"url": "/manga/existing-cat", "title": "Existing Cat Title", "source_id": 1}
              ],
              "categories": [
                {"id": "old-fav-id", "name": "favorites", "order": 5}
              ],
              "category_mappings": {
                "/manga/existing-cat": ["old-fav-id", "${DefaultCategories.READING}"]
              }
            }
        """.trimIndent()

        val importedCount = BackupEngine.importJson(backupJson, repo, categoryRepo)
        assertEquals(1, importedCount)

        val assigned = categoryRepo.mangaCategoryMappings.value["/manga/existing-cat"].orEmpty()
        assertEquals(setOf("cat-favorites", DefaultCategories.READING, DefaultCategories.ALL), assigned)
    }

    @Test
    fun exportAndImportRoundTripPreservesCategoriesAndMappings() = runTest {
        val sourceRepo = FakePanelRepository()
        val sourceCategories = DefaultCategoryRepository()
        val customCat = sourceCategories.addCategory("Mecha").getOrThrow()
        sourceCategories.setMangaCategories("solo-leveling", setOf(DefaultCategories.READING, customCat.id))

        val json = BackupEngine.exportJson(sourceRepo, nowMs = 999L, categoryRepository = sourceCategories)

        val targetRepo = FakePanelRepository()
        val targetCategories = DefaultCategoryRepository()
        val count = BackupEngine.importJson(json, targetRepo, targetCategories)
        assertTrue(count >= 0)

        val targetMecha = targetCategories.categories.value.find { it.name == "Mecha" }
        assertNotNull(targetMecha)
        val targetAssigned = targetCategories.mangaCategoryMappings.value["solo-leveling"].orEmpty()
        assertTrue(targetMecha.id in targetAssigned)
        assertTrue(DefaultCategories.READING in targetAssigned)
    }
}
