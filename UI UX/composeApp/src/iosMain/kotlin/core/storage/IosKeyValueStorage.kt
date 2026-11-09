package core.storage

import platform.Foundation.NSUserDefaults

/**
 * iOS implementation of KeyValueStorage using standard NSUserDefaults.
 */
class IosKeyValueStorage : KeyValueStorage {
    private val defaults: NSUserDefaults = NSUserDefaults.standardUserDefaults

    override fun getString(key: String, defaultValue: String): String =
        defaults.stringForKey(key) ?: defaultValue

    override fun putString(key: String, value: String) {
        defaults.setObject(value, forKey = key)
    }

    override fun getInt(key: String, defaultValue: Int): Int =
        if (defaults.objectForKey(key) != null) defaults.integerForKey(key).toInt() else defaultValue

    override fun putInt(key: String, value: Int) {
        defaults.setInteger(value.toLong(), forKey = key)
    }

    override fun getBoolean(key: String, defaultValue: Boolean): Boolean =
        if (defaults.objectForKey(key) != null) defaults.boolForKey(key) else defaultValue

    override fun putBoolean(key: String, value: Boolean) {
        defaults.setBool(value, forKey = key)
    }

    override fun getFloat(key: String, defaultValue: Float): Float =
        if (defaults.objectForKey(key) != null) defaults.floatForKey(key) else defaultValue

    override fun putFloat(key: String, value: Float) {
        defaults.setFloat(value, forKey = key)
    }

    override fun getLong(key: String, defaultValue: Long): Long =
        if (defaults.objectForKey(key) != null) defaults.integerForKey(key) else defaultValue

    override fun putLong(key: String, value: Long) {
        defaults.setInteger(value, forKey = key)
    }

    override fun remove(key: String) {
        defaults.removeObjectForKey(key)
    }

    override fun clear() {
        val dict = defaults.dictionaryRepresentation()
        for (key in dict.keys) {
            defaults.removeObjectForKey(key as String)
        }
    }
}
