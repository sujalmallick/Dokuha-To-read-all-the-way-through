package core.di

import data.mock.FakePanelRepository
import data.mock.PanelRepository
import data.profile.FakeProfileRepository
import data.profile.ProfileRepository
import data.settings.FakeSettingsRepository
import data.settings.SettingsRepository
import org.koin.dsl.module

val appModule = module {
    single<PanelRepository> { FakePanelRepository() }
    single<SettingsRepository> { FakeSettingsRepository() }
    single<ProfileRepository> { FakeProfileRepository() }
}
