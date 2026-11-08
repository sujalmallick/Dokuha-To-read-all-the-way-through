package core.di

import data.mock.FakePanelRepository
import data.mock.PanelRepository
import org.koin.dsl.module

val appModule = module {
    single<PanelRepository> { FakePanelRepository() }
}
