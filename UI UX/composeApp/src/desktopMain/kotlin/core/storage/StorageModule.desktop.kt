package core.storage

import data.db.DatabaseDriverFactory
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformStorageModule: Module = module {
    single { DatabaseDriverFactory() }
    single<KeyValueStorage> { DesktopKeyValueStorage() }
    single<SecureStorage> { DesktopSecureStorage() }
}
