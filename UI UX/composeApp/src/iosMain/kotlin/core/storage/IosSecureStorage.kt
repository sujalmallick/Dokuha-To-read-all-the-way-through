package core.storage

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.value
import platform.CoreFoundation.CFDictionaryAddValue
import platform.CoreFoundation.CFDictionaryCreateMutable
import platform.CoreFoundation.CFDictionaryRef
import platform.CoreFoundation.CFRelease
import platform.CoreFoundation.CFTypeRefVar
import platform.CoreFoundation.kCFBooleanTrue
import platform.CoreFoundation.kCFTypeDictionaryKeyCallBacks
import platform.CoreFoundation.kCFTypeDictionaryValueCallBacks
import platform.Foundation.CFBridgingRelease
import platform.Foundation.CFBridgingRetain
import platform.Foundation.NSData
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.dataUsingEncoding
import platform.Security.SecItemAdd
import platform.Security.SecItemCopyMatching
import platform.Security.SecItemDelete
import platform.Security.errSecSuccess
import platform.Security.kSecAttrAccount
import platform.Security.kSecAttrService
import platform.Security.kSecClass
import platform.Security.kSecClassGenericPassword
import platform.Security.kSecMatchLimit
import platform.Security.kSecMatchLimitOne
import platform.Security.kSecReturnData
import platform.Security.kSecValueData

/**
 * iOS SecureStorage backed by Apple Keychain Services.
 */
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
class IosSecureStorage : SecureStorage {
    private val serviceName = "com.dokuha.reader.secure"

    override fun getSecret(key: String): String? = memScoped {
        val query = CFDictionaryCreateMutable(
            null,
            5,
            kCFTypeDictionaryKeyCallBacks.ptr,
            kCFTypeDictionaryValueCallBacks.ptr
        ) ?: return null
        val keyRef = CFBridgingRetain(key)
        val serviceRef = CFBridgingRetain(serviceName)
        try {
            CFDictionaryAddValue(query, kSecClass, kSecClassGenericPassword)
            CFDictionaryAddValue(query, kSecAttrService, serviceRef)
            CFDictionaryAddValue(query, kSecAttrAccount, keyRef)
            CFDictionaryAddValue(query, kSecReturnData, kCFBooleanTrue)
            CFDictionaryAddValue(query, kSecMatchLimit, kSecMatchLimitOne)

            val result = alloc<CFTypeRefVar>()
            val status = SecItemCopyMatching(query, result.ptr)
            if (status != errSecSuccess) return null

            val data = CFBridgingRelease(result.value) as? NSData ?: return null
            return NSString.create(data = data, encoding = NSUTF8StringEncoding)?.toString()
        } finally {
            CFRelease(query)
            if (keyRef != null) CFRelease(keyRef)
            if (serviceRef != null) CFRelease(serviceRef)
        }
    }

    override fun putSecret(key: String, secret: String) {
        removeSecret(key)
        val data = NSString.create(string = secret).dataUsingEncoding(NSUTF8StringEncoding) ?: return
        val query = CFDictionaryCreateMutable(
            null,
            4,
            kCFTypeDictionaryKeyCallBacks.ptr,
            kCFTypeDictionaryValueCallBacks.ptr
        ) ?: return
        val keyRef = CFBridgingRetain(key)
        val serviceRef = CFBridgingRetain(serviceName)
        val dataRef = CFBridgingRetain(data)
        try {
            CFDictionaryAddValue(query, kSecClass, kSecClassGenericPassword)
            CFDictionaryAddValue(query, kSecAttrService, serviceRef)
            CFDictionaryAddValue(query, kSecAttrAccount, keyRef)
            CFDictionaryAddValue(query, kSecValueData, dataRef)
            SecItemAdd(query, null)
        } finally {
            CFRelease(query)
            if (keyRef != null) CFRelease(keyRef)
            if (serviceRef != null) CFRelease(serviceRef)
            if (dataRef != null) CFRelease(dataRef)
        }
    }

    override fun removeSecret(key: String) {
        val query = CFDictionaryCreateMutable(
            null,
            3,
            kCFTypeDictionaryKeyCallBacks.ptr,
            kCFTypeDictionaryValueCallBacks.ptr
        ) ?: return
        val keyRef = CFBridgingRetain(key)
        val serviceRef = CFBridgingRetain(serviceName)
        try {
            CFDictionaryAddValue(query, kSecClass, kSecClassGenericPassword)
            CFDictionaryAddValue(query, kSecAttrService, serviceRef)
            CFDictionaryAddValue(query, kSecAttrAccount, keyRef)
            SecItemDelete(query)
        } finally {
            CFRelease(query)
            if (keyRef != null) CFRelease(keyRef)
            if (serviceRef != null) CFRelease(serviceRef)
        }
    }

    override fun clearSecrets() {
        val query = CFDictionaryCreateMutable(
            null,
            2,
            kCFTypeDictionaryKeyCallBacks.ptr,
            kCFTypeDictionaryValueCallBacks.ptr
        ) ?: return
        val serviceRef = CFBridgingRetain(serviceName)
        try {
            CFDictionaryAddValue(query, kSecClass, kSecClassGenericPassword)
            CFDictionaryAddValue(query, kSecAttrService, serviceRef)
            SecItemDelete(query)
        } finally {
            CFRelease(query)
            if (serviceRef != null) CFRelease(serviceRef)
        }
    }
}
