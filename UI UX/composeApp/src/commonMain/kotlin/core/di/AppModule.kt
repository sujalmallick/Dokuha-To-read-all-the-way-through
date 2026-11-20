package core.di

import core.download.DownloadManager as CoreDownloadManager
import core.storage.platformStorageModule
import data.download.DefaultDownloadManager
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
import data.settings.SettingsRepository
import data.tracker.ProductionTrackerRepository
import data.tracker.TrackerRepository
import core.extensions.ExtensionManager
import core.extensions.js.AntiBotChallengeManager
import core.extensions.js.AntiBotResolver
import io.panel.reader.db.PanelDatabase
import io.ktor.client.HttpClient
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import org.koin.dsl.module

val appModule = module {
    includes(platformStorageModule)
    single<HttpClient> {
        HttpClient {
            defaultRequest {
                header(HttpHeaders.UserAgent, "Dokuha/1.0.0 (https://github.com/RitwikGupta-0501/dokuha)")
            }
        }
    }
    single { PanelDatabase(get<DatabaseDriverFactory>().createDriver()) }
    single { CoreDownloadManager(downloadStorage = get(), httpClient = getOrNull()) }
    single { AntiBotChallengeManager() }
    single<AntiBotResolver> { get<AntiBotChallengeManager>() }
    single<PanelRepository> { SqlDelightPanelRepository(database = get(), downloadManager = getOrNull(), downloadStorage = getOrNull()) }
    single<ExtensionManager> { ExtensionManager(storage = get(), panelRepository = get(), httpClient = getOrNull(), settingsRepository = getOrNull(), antiBotResolver = getOrNull(), autoSeedDefaults = true) }
    single<TrackerRepository> { ProductionTrackerRepository(getOrNull()) }
    single<SettingsRepository> { PersistentSettingsRepository(get()) }
    single<ProfileRepository> { PersistentProfileRepository(get(), get()) }
    single<DataDownloadManager> { DefaultDownloadManager() }
    single<ExtensionRepository> { DefaultExtensionRepository() }
    single<CategoryRepository> { DefaultCategoryRepository() }
}
