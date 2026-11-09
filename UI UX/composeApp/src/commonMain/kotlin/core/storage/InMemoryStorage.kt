package core.storage

/**
 * In-memory implementation of KeyValueStorage for tests and previews.
 */
class InMemoryKeyValueStorage : KeyValueStorage {
    private val data = mutableMapOf<String, Any>()

    override fun getString(key: String, defaultValue: String): String =
        (data[key] as? String) ?: defaultValue

    override fun putString(key: String, value: String) {
        data[key] = value
    }

    override fun getInt(key: String, defaultValue: Int): Int =
        (data[key] as? Int) ?: defaultValue

    override fun putInt(key: String, value: Int) {
        data[key] = value
    }

    override fun getBoolean(key: String, defaultValue: Boolean): Boolean =
        (data[key] as? Boolean) ?: defaultValue

    override fun putBoolean(key: String, value: Boolean) {
        data[key] = value
    }

    override fun getFloat(key: String, defaultValue: Float): Float =
        (data[key] as? Float) ?: defaultValue

    override fun putFloat(key: String, value: Float) {
        data[key] = value
    }

    override fun getLong(key: String, defaultValue: Long): Long =
        (data[key] as? Long) ?: defaultValue

    override fun putLong(key: String, value: Long) {
        data[key] = value
    }

    override fun remove(key: String) {
        data.remove(key)
    }

    override fun clear() {
        data.clear()
    }
}

/**
 * In-memory implementation of SecureStorage for tests and previews.
 */
class InMemorySecureStorage : SecureStorage {
    private val secrets = mutableMapOf<String, String>()

    override fun getSecret(key: String): String? = secrets[key]

    override fun putSecret(key: String, secret: String) {
        secrets[key] = secret
    }

    override fun removeSecret(key: String) {
        secrets.remove(key)
    }

    override fun clearSecrets() {
        secrets.clear()
    }
}
