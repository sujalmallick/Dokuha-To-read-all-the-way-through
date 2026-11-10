package core.storage

import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformStorageModule: Module = module {
    single<KeyValueStorage> { DesktopKeyValueStorage() }
    single<SecureStorage> { DesktopSecureStorage() }
}
