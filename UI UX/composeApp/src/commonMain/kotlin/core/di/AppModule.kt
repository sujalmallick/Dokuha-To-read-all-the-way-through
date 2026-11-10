package core.di

import core.storage.platformStorageModule
import data.mock.FakePanelRepository
import data.mock.PanelRepository
import data.profile.PersistentProfileRepository
import data.profile.ProfileRepository
import data.settings.PersistentSettingsRepository
import data.settings.SettingsRepository
import data.tracker.ProductionTrackerRepository
import data.tracker.TrackerRepository
import core.extensions.ExtensionManager
import io.ktor.client.HttpClient
import org.koin.dsl.module

val appModule = module {
    includes(platformStorageModule)
    single<HttpClient> { HttpClient() }
    single<PanelRepository> { FakePanelRepository() }
    single<ExtensionManager> { ExtensionManager(storage = get(), panelRepository = get(), httpClient = getOrNull()) }
    single<TrackerRepository> { ProductionTrackerRepository(getOrNull()) }
    single<SettingsRepository> { PersistentSettingsRepository(get()) }
    single<ProfileRepository> { PersistentProfileRepository(get(), get()) }
}
