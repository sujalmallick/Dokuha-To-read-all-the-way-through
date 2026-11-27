package core.di

import core.download.DownloadManager
import core.storage.platformStorageModule
import data.download.CoreBackedDownloadManager
import data.download.DownloadManager as DataDownloadManager
import data.extension.DefaultExtensionRepository
import data.extension.ExtensionRepository
import data.library.CategoryRepository
import data.library.DefaultCategoryRepository
import data.db.DatabaseDriverFactory
import data.mock.PanelRepository
import data.profile.PersistentProfileRepository
import data.profile.ProfileRepository
import data.repository.SqlDelightPanelRepository
import data.settings.PersistentSettingsRepository
import data.settings.ReaderPrefsRepository
import data.settings.SettingsRepository
import data.settings.SqlDelightReaderPrefsRepository
import data.tracker.ProductionTrackerRepository
import data.tracker.TrackerRepository
import core.extensions.ExtensionManager
import core.extensions.js.AntiBotChallengeManager
import core.extensions.js.AntiBotResolver
import io.panel.reader.db.PanelDatabase
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import org.koin.dsl.module

internal fun HttpClientConfig<*>.configureAppHttpClient() {
    install(HttpTimeout) {
        connectTimeoutMillis = 15_000L
        requestTimeoutMillis = 30_000L
        socketTimeoutMillis = 30_000L
    }
    install(HttpRequestRetry) {
        maxRetries = 2
        retryOnServerErrors(maxRetries = 2)
        retryOnException(maxRetries = 2, retryOnTimeout = true)
        exponentialDelay(base = 2.0, maxDelayMs = 4_000L)
    }
    defaultRequest {
        header(HttpHeaders.UserAgent, "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36")
    }
}

val appModule = module {
    includes(platformStorageModule)
    single<HttpClient> {
        HttpClient {
            configureAppHttpClient()
        }
    }
    single { PanelDatabase(get<DatabaseDriverFactory>().createDriver()) }
    single {
        DownloadManager(
            downloadStorage = get(),
            httpClient = getOrNull(),
            storageProvider = getOrNull()
        )
    }
    single { AntiBotChallengeManager(headlessSolver = getOrNull()) }
    single<AntiBotResolver> { get<AntiBotChallengeManager>() }
    single<PanelRepository> {
        val repo = SqlDelightPanelRepository(
            database = get(),
            downloadManager = getOrNull(),
            downloadStorage = getOrNull(),
            settingsRepository = getOrNull()
        )
        getOrNull<DownloadManager>()?.setSourceProvider { manga, chapter ->
            val sid = manga.sourceId.takeIf { it != 0L }?.toString()
            repo.resolveSource(sid, manga.url, chapter.url)
        }
        repo
    }
    single<ExtensionManager> {
        val manager = ExtensionManager(
            storage = get(),
            panelRepository = get(),
            httpClient = getOrNull(),
            settingsRepository = getOrNull(),
            antiBotResolver = getOrNull(),
            autoSeedDefaults = false,
            deferInit = true
        )
        (getOrNull<PanelRepository>() as? SqlDelightPanelRepository)?.bindExtensionFlows(
            installed = manager.installedExtensions,
            available = manager.availableExtensions
        )
        manager
    }
    single<TrackerRepository> {
        ProductionTrackerRepository(
            secureStorage = getOrNull(),
            httpClient = getOrNull(),
            keyValueStorage = getOrNull()
        )
    }
    single<SettingsRepository> { PersistentSettingsRepository(get()) }
    single<ReaderPrefsRepository> { SqlDelightReaderPrefsRepository(database = get(), settingsRepository = getOrNull()) }
    single<ProfileRepository> { PersistentProfileRepository(get(), get()) }
    single<DataDownloadManager> {
        CoreBackedDownloadManager(
            coreDownloadManager = get(),
            downloadStorage = get(),
            storageProvider = getOrNull(),
            panelRepository = getOrNull(),
            keyValueStorage = getOrNull()
        )
    }
    single<ExtensionRepository> { DefaultExtensionRepository(httpClient = getOrNull(), extensionManager = getOrNull()) }
    single<CategoryRepository> { DefaultCategoryRepository() }
}
