package core.di

import core.storage.platformStorageModule
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
import io.panel.reader.db.PanelDatabase
import io.ktor.client.HttpClient
import org.koin.dsl.module

val appModule = module {
    includes(platformStorageModule)
    single<HttpClient> { HttpClient() }
    single { PanelDatabase(get<DatabaseDriverFactory>().createDriver()) }
    single<PanelRepository> { SqlDelightPanelRepository(database = get()) }
    single<ExtensionManager> { ExtensionManager(storage = get(), panelRepository = get(), httpClient = getOrNull(), settingsRepository = getOrNull()) }
    single<TrackerRepository> { ProductionTrackerRepository(getOrNull()) }
    single<SettingsRepository> { PersistentSettingsRepository(get()) }
    single<ProfileRepository> { PersistentProfileRepository(get(), get()) }
}
