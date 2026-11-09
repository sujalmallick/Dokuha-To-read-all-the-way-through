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
import org.koin.dsl.module

val appModule = module {
    includes(platformStorageModule)
    single<PanelRepository> { FakePanelRepository() }
    single<TrackerRepository> { ProductionTrackerRepository(getOrNull()) }
    single<SettingsRepository> { PersistentSettingsRepository(get()) }
    single<ProfileRepository> { PersistentProfileRepository(get(), get()) }
}
