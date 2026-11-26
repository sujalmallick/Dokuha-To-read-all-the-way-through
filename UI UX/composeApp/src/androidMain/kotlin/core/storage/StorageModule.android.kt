package core.storage

import core.download.AndroidDownloadStorage
import core.download.DownloadStorage
import core.extensions.js.HeadlessCloudflareSolver
import core.extensions.js.HeadlessSolver
import data.db.DatabaseDriverFactory
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformStorageModule: Module = module {
    single { DatabaseDriverFactory(androidContext()) }
    single<KeyValueStorage> { AndroidKeyValueStorage(androidContext()) }
    single<SecureStorage> { AndroidSecureStorage(androidContext()) }
    single<DownloadStorage> { AndroidDownloadStorage(androidContext()) }
    single { StorageProvider(androidContext()) }
    single<HeadlessSolver> { HeadlessCloudflareSolver(androidContext()) }
}
