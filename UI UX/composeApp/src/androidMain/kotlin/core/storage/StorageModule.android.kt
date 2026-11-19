package core.storage

import data.db.DatabaseDriverFactory
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformStorageModule: Module = module {
    single { DatabaseDriverFactory(androidContext()) }
    single<KeyValueStorage> { AndroidKeyValueStorage(androidContext()) }
    single<SecureStorage> { AndroidSecureStorage(androidContext()) }
}
