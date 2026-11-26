package data.tracker

import core.platform.BuildEnvironment
import core.storage.KeyValueStorage
import core.storage.SecureStorage
import data.profile.TrackerAccount
import data.profile.TrackerService
import io.ktor.client.HttpClient
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.http.parameters
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put

enum class TrackReadingStatus(
    val label: String,
    val anilistValue: String,
    val malValue: String
) {
    READING("Reading", "CURRENT", "reading"),
    COMPLETED("Completed", "COMPLETED", "completed"),
    ON_HOLD("On Hold", "PAUSED", "on_hold"),
    DROPPED("Dropped", "DROPPED", "dropped"),
    PLAN_TO_READ("Plan to Read", "PLANNING", "plan_to_read")
}

@Serializable
data class TrackSearchResult(
    val remoteId: Long,
    val title: String,
    val totalChapters: Int? = null,
    val coverUrl: String? = null,
    val service: TrackerService
)

@Serializable
data class TrackBinding(
    val mangaUrl: String,
    val service: TrackerService,
    val remoteId: Long,
    val remoteTitle: String,
    val status: TrackReadingStatus = TrackReadingStatus.READING,
    val lastChapterRead: Int = 0,
    val totalChapters: Int? = null
)

/**
 * Repository interface for managing manga/novel tracking services (AniList, MyAnimeList, Kitsu).
 */
interface TrackerRepository {
    val trackers: StateFlow<List<TrackerAccount>>
    val isDebugStubEnabled: Boolean

    fun toggleDebugConnection(service: TrackerService)
    fun disconnect(service: TrackerService)

    suspend fun connectWithToken(service: TrackerService, accessToken: String): Result<TrackerAccount> =
        Result.failure(UnsupportedOperationException("Not implemented"))

    suspend fun searchMedia(service: TrackerService, query: String): Result<List<TrackSearchResult>> =
        Result.success(emptyList())

    suspend fun syncProgress(binding: TrackBinding): Result<TrackBinding> =
        Result.success(binding)

    fun getBinding(mangaUrl: String, service: TrackerService): TrackBinding? = null
    fun saveBinding(binding: TrackBinding) {}
}

/**
 * Production implementation of TrackerRepository supporting AniList GraphQL and MyAnimeList v2 REST.
 */
class ProductionTrackerRepository(
    private val secureStorage: SecureStorage? = null,
    private val isDebug: Boolean = BuildEnvironment.isDebug,
    private val httpClient: HttpClient? = null,
    private val keyValueStorage: KeyValueStorage? = null
) : TrackerRepository {

    private val json = Json { ignoreUnknownKeys = true }
    override val isDebugStubEnabled: Boolean = isDebug

    private val initialTrackers: List<TrackerAccount> = listOf(
        TrackerAccount(
            service = TrackerService.ANILIST,
            isConnected = !secureStorage?.getSecret(tokenKey(TrackerService.ANILIST)).isNullOrBlank(),
            username = keyValueStorage?.getString(usernameKey(TrackerService.ANILIST), "")?.takeIf { it.isNotBlank() }
        ),
        TrackerAccount(
            service = TrackerService.MY_ANIME_LIST,
            isConnected = !secureStorage?.getSecret(tokenKey(TrackerService.MY_ANIME_LIST)).isNullOrBlank(),
            username = keyValueStorage?.getString(usernameKey(TrackerService.MY_ANIME_LIST), "")?.takeIf { it.isNotBlank() }
        ),
        TrackerAccount(
            service = TrackerService.KITSU,
            isConnected = !secureStorage?.getSecret(tokenKey(TrackerService.KITSU)).isNullOrBlank(),
            username = keyValueStorage?.getString(usernameKey(TrackerService.KITSU), "")?.takeIf { it.isNotBlank() }
        )
    )

    private val _trackers = MutableStateFlow(initialTrackers)
    override val trackers: StateFlow<List<TrackerAccount>> = _trackers.asStateFlow()

    override fun toggleDebugConnection(service: TrackerService) {
        if (!isDebugStubEnabled) return

        val updated = _trackers.value.map { account ->
            if (account.service == service) {
                val nextConnected = !account.isConnected
                account.copy(
                    isConnected = nextConnected,
                    username = if (nextConnected) "dev_user_${service.name.lowercase()}" else null
                )
            } else {
                account
            }
        }
        _trackers.value = updated
    }

    override fun disconnect(service: TrackerService) {
        secureStorage?.removeSecret(tokenKey(service))
        keyValueStorage?.putString(usernameKey(service), "")

        _trackers.value = _trackers.value.map { account ->
            if (account.service == service) {
                account.copy(isConnected = false, username = null)
            } else {
                account
            }
        }
    }

    override suspend fun connectWithToken(service: TrackerService, accessToken: String): Result<TrackerAccount> {
        val cleanToken = accessToken.trim()
        if (cleanToken.isBlank()) {
            return Result.failure(IllegalArgumentException("Token cannot be empty"))
        }
        val client = httpClient ?: return Result.failure(IllegalStateException("Network client unavailable"))

        return runCatching {
            val username = when (service) {
                TrackerService.ANILIST -> {
                    val body = buildJsonObject {
                        put("query", "query { Viewer { id name } }")
                    }.toString()
                    val response = client.post("https://graphql.anilist.co") {
                        header(HttpHeaders.Authorization, "Bearer $cleanToken")
                        contentType(ContentType.Application.Json)
                        setBody(body)
                    }
                    if (!response.status.isSuccess()) {
                        error("AniList authentication failed (${response.status.value})")
                    }
                    val root = json.parseToJsonElement(response.bodyAsText()).jsonObject
                    root["data"]?.jsonObject?.get("Viewer")?.jsonObject?.get("name")?.jsonPrimitive?.content
                        ?: error("Invalid AniList viewer response")
                }
                TrackerService.MY_ANIME_LIST -> {
                    val response = client.get("https://api.myanimelist.net/v2/users/@me") {
                        header(HttpHeaders.Authorization, "Bearer $cleanToken")
                    }
                    if (!response.status.isSuccess()) {
                        error("MyAnimeList authentication failed (${response.status.value})")
                    }
                    val root = json.parseToJsonElement(response.bodyAsText()).jsonObject
                    root["name"]?.jsonPrimitive?.content ?: error("Invalid MyAnimeList user response")
                }
                TrackerService.KITSU -> error("Kitsu token sync is not enabled yet")
            }

            secureStorage?.putSecret(tokenKey(service), cleanToken)
            keyValueStorage?.putString(usernameKey(service), username)
            val connectedAccount = TrackerAccount(service = service, isConnected = true, username = username)
            _trackers.value = _trackers.value.map { if (it.service == service) connectedAccount else it }
            connectedAccount
        }
    }

    override suspend fun searchMedia(service: TrackerService, query: String): Result<List<TrackSearchResult>> {
        val cleanQuery = query.trim()
        if (cleanQuery.isBlank()) return Result.success(emptyList())
        val client = httpClient ?: return Result.failure(IllegalStateException("Network client unavailable"))

        return runCatching {
            when (service) {
                TrackerService.ANILIST -> {
                    val gql = """
                        query (${'$'}search: String) {
                          Page(perPage: 10) {
                            media(search: ${'$'}search, type: MANGA) {
                              id
                              chapters
                              title { romaji english }
                              coverImage { medium }
                            }
                          }
                        }
                    """.trimIndent()
                    val payload = buildJsonObject {
                        put("query", gql)
                        put("variables", buildJsonObject { put("search", cleanQuery) })
                    }.toString()
                    val response = client.post("https://graphql.anilist.co") {
                        secureStorage?.getSecret(tokenKey(service))?.takeIf { it.isNotBlank() }?.let {
                            header(HttpHeaders.Authorization, "Bearer $it")
                        }
                        contentType(ContentType.Application.Json)
                        setBody(payload)
                    }
                    if (!response.status.isSuccess()) error("AniList search failed (${response.status.value})")
                    val root = json.parseToJsonElement(response.bodyAsText()).jsonObject
                    val mediaArray = root["data"]?.jsonObject?.get("Page")?.jsonObject?.get("media")?.jsonArray
                        ?: JsonArray(emptyList())
                    mediaArray.mapNotNull { elem ->
                        val obj = elem.jsonObject
                        val id = obj["id"]?.jsonPrimitive?.longOrNull ?: return@mapNotNull null
                        val titleObj = obj["title"]?.jsonObject
                        val title = titleObj?.get("english")?.jsonPrimitive?.content
                            ?: titleObj?.get("romaji")?.jsonPrimitive?.content
                            ?: return@mapNotNull null
                        TrackSearchResult(
                            remoteId = id,
                            title = title,
                            totalChapters = obj["chapters"]?.jsonPrimitive?.intOrNull,
                            coverUrl = obj["coverImage"]?.jsonObject?.get("medium")?.jsonPrimitive?.content,
                            service = TrackerService.ANILIST
                        )
                    }
                }
                TrackerService.MY_ANIME_LIST -> {
                    val token = secureStorage?.getSecret(tokenKey(service))
                        ?.takeIf { it.isNotBlank() }
                        ?: error("Connect MyAnimeList before searching")
                    val response = client.get("https://api.myanimelist.net/v2/manga") {
                        header(HttpHeaders.Authorization, "Bearer $token")
                        parameter("q", cleanQuery)
                        parameter("limit", 10)
                        parameter("fields", "id,title,num_chapters,main_picture")
                    }
                    if (!response.status.isSuccess()) error("MAL search failed (${response.status.value})")
                    val root = json.parseToJsonElement(response.bodyAsText()).jsonObject
                    val data = root["data"]?.jsonArray ?: JsonArray(emptyList())
                    data.mapNotNull { elem ->
                        val node = elem.jsonObject["node"]?.jsonObject ?: return@mapNotNull null
                        val id = node["id"]?.jsonPrimitive?.longOrNull ?: return@mapNotNull null
                        val title = node["title"]?.jsonPrimitive?.content ?: return@mapNotNull null
                        TrackSearchResult(
                            remoteId = id,
                            title = title,
                            totalChapters = node["num_chapters"]?.jsonPrimitive?.intOrNull?.takeIf { it > 0 },
                            coverUrl = node["main_picture"]?.jsonObject?.get("medium")?.jsonPrimitive?.content,
                            service = TrackerService.MY_ANIME_LIST
                        )
                    }
                }
                TrackerService.KITSU -> emptyList()
            }
        }
    }

    override suspend fun syncProgress(binding: TrackBinding): Result<TrackBinding> {
        val client = httpClient ?: return Result.failure(IllegalStateException("Network client unavailable"))
        val token = secureStorage?.getSecret(tokenKey(binding.service))
            ?.takeIf { it.isNotBlank() }
            ?: return Result.failure(IllegalStateException("${binding.service.name} is not connected"))

        return runCatching {
            when (binding.service) {
                TrackerService.ANILIST -> {
                    val mutation = """
                        mutation (${'$'}mediaId: Int, ${'$'}status: MediaListStatus, ${'$'}progress: Int) {
                          SaveMediaListEntry(mediaId: ${'$'}mediaId, status: ${'$'}status, progress: ${'$'}progress) {
                            id
                            progress
                          }
                        }
                    """.trimIndent()
                    val payload = buildJsonObject {
                        put("query", mutation)
                        put("variables", buildJsonObject {
                            put("mediaId", binding.remoteId)
                            put("status", binding.status.anilistValue)
                            put("progress", binding.lastChapterRead)
                        })
                    }.toString()
                    val response = client.post("https://graphql.anilist.co") {
                        header(HttpHeaders.Authorization, "Bearer $token")
                        contentType(ContentType.Application.Json)
                        setBody(payload)
                    }
                    if (!response.status.isSuccess()) error("AniList sync failed (${response.status.value})")
                }
                TrackerService.MY_ANIME_LIST -> {
                    val response = client.patch("https://api.myanimelist.net/v2/manga/${binding.remoteId}/my_list_status") {
                        header(HttpHeaders.Authorization, "Bearer $token")
                        setBody(
                            io.ktor.client.request.forms.FormDataContent(
                                parameters {
                                    append("status", binding.status.malValue)
                                    append("num_chapters_read", binding.lastChapterRead.toString())
                                }
                            )
                        )
                    }
                    if (!response.status.isSuccess()) error("MAL sync failed (${response.status.value})")
                }
                TrackerService.KITSU -> error("Kitsu sync not enabled")
            }
            saveBinding(binding)
            binding
        }
    }

    override fun getBinding(mangaUrl: String, service: TrackerService): TrackBinding? {
        val raw = keyValueStorage?.getString(bindingKey(mangaUrl, service), "") ?: return null
        if (raw.isBlank()) return null
        return runCatching { json.decodeFromString<TrackBinding>(raw) }.getOrNull()
    }

    override fun saveBinding(binding: TrackBinding) {
        keyValueStorage?.putString(
            bindingKey(binding.mangaUrl, binding.service),
            json.encodeToString(binding)
        )
    }

    private fun tokenKey(service: TrackerService) = "tracker_token_${service.name.lowercase()}"
    private fun usernameKey(service: TrackerService) = "tracker_username_${service.name.lowercase()}"
    private fun bindingKey(mangaUrl: String, service: TrackerService): String {
        val hash = (mangaUrl.hashCode() and 0x7FFFFFFF).toString(16)
        return "tracker_binding_${service.name.lowercase()}_$hash"
    }
}
