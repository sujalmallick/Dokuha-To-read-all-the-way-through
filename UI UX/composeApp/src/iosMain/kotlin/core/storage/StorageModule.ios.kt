package core.storage

import core.download.DownloadStorage
import core.download.IosDownloadStorage
import data.db.DatabaseDriverFactory
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformStorageModule: Module = module {
    single { DatabaseDriverFactory() }
    single<KeyValueStorage> { IosKeyValueStorage() }
    single<SecureStorage> { IosSecureStorage() }
    single<DownloadStorage> { IosDownloadStorage() }
}
