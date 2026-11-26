package data

import core.storage.InMemoryKeyValueStorage
import core.storage.InMemorySecureStorage
import data.backup.BackupEngine
import data.mock.FakePanelRepository
import data.profile.TrackerService
import data.settings.PersistentSettingsRepository
import data.tracker.ProductionTrackerRepository
import data.tracker.TrackBinding
import data.tracker.TrackReadingStatus
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class CoreFeaturesV2Test {

    @Test
    fun testIncognitoModePersistsInSettingsRepository() {
        val storage = InMemoryKeyValueStorage()
        val repo = PersistentSettingsRepository(storage)
        assertFalse(repo.incognitoMode.value)

        repo.setIncognitoMode(true)
        assertTrue(repo.incognitoMode.value)

        val reloaded = PersistentSettingsRepository(storage)
        assertTrue(reloaded.incognitoMode.value)
    }

    @Test
    fun testBackupEngineImportsTachiyomiJsonAndDokuhaV2() = runTest {
        val repo = FakePanelRepository()
        val json = BackupEngine.exportJson(repo, nowMs = 123456L)
        assertTrue(json.contains("\"schema_version\":2"))

        val tachibkJson = """
            {
              "backupManga": [
                {"url": "/manga/omniscient", "title": "Omniscient Reader", "source": 42}
              ]
            }
        """.trimIndent()
        val restored = BackupEngine.importJson(tachibkJson, repo)
        assertEquals(1, restored)

        val invalid = BackupEngine.importJson("{not valid json", repo)
        assertEquals(-1, invalid)
    }

    @Test
    fun testAniListTrackerConnectSearchAndSync() = runTest {
        val mockEngine = MockEngine { request ->
            val url = request.url.toString()
            if (url.contains("graphql.anilist.co")) {
                respond(
                    content = """
                        {
                          "data": {
                            "Viewer": { "id": 1, "name": "dokuha_tester" },
                            "Page": {
                              "media": [
                                {
                                  "id": 99001,
                                  "chapters": 120,
                                  "title": { "romaji": "Solo Leveling", "english": "Solo Leveling" },
                                  "coverImage": { "medium": "https://example.com/cover.jpg" }
                                }
                              ]
                            },
                            "SaveMediaListEntry": { "id": 55, "progress": 42 }
                          }
                        }
                    """.trimIndent(),
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                )
            } else {
                respond("{}", HttpStatusCode.NotFound)
            }
        }
        val httpClient = HttpClient(mockEngine)
        val secureStorage = InMemorySecureStorage()
        val kvStorage = InMemoryKeyValueStorage()
        val trackerRepo = ProductionTrackerRepository(
            secureStorage = secureStorage,
            isDebug = false,
            httpClient = httpClient,
            keyValueStorage = kvStorage
        )

        val connectRes = trackerRepo.connectWithToken(TrackerService.ANILIST, "test_token_123")
        assertTrue(connectRes.isSuccess)
        assertEquals("dokuha_tester", connectRes.getOrNull()?.username)
        assertEquals("test_token_123", secureStorage.getSecret("tracker_token_anilist"))

        val searchRes = trackerRepo.searchMedia(TrackerService.ANILIST, "Solo Leveling")
        assertTrue(searchRes.isSuccess)
        val first = searchRes.getOrNull()?.firstOrNull()
        assertNotNull(first)
        assertEquals(99001L, first.remoteId)

        val binding = TrackBinding(
            mangaUrl = "manga/solo",
            service = TrackerService.ANILIST,
            remoteId = first.remoteId,
            remoteTitle = first.title,
            status = TrackReadingStatus.READING,
            lastChapterRead = 42,
            totalChapters = first.totalChapters
        )
        val syncRes = trackerRepo.syncProgress(binding)
        assertTrue(syncRes.isSuccess)
        assertEquals(42, trackerRepo.getBinding("manga/solo", TrackerService.ANILIST)?.lastChapterRead)
    }
}
