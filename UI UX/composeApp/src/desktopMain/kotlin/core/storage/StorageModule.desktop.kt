package core.storage

import core.download.DesktopDownloadStorage
import core.download.DownloadStorage
import core.extensions.js.HeadlessCloudflareSolver
import data.db.DatabaseDriverFactory
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformStorageModule: Module = module {
    single { DatabaseDriverFactory() }
    single<KeyValueStorage> { DesktopKeyValueStorage() }
    single<SecureStorage> { DesktopSecureStorage() }
    single<DownloadStorage> { DesktopDownloadStorage() }
    single { HeadlessCloudflareSolver() }
}
