package core.storage

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

class DesktopKeyValueStorage(
    baseDir: File = File(System.getProperty("user.home"), ".config/dokuha")
) : KeyValueStorage {
    private val file = File(baseDir, "settings.json")
    private val data = mutableMapOf<String, String>()
    private val json = Json { prettyPrint = true }

    init {
        baseDir.mkdirs()
        if (file.exists()) {
            try {
                val map = json.decodeFromString<Map<String, String>>(file.readText())
                data.putAll(map)
            } catch (_: Exception) {}
        }
    }

    private fun persist() {
        try {
            file.writeText(json.encodeToString(data))
        } catch (_: Exception) {}
    }

    override fun getString(key: String, defaultValue: String): String = data[key] ?: defaultValue

    override fun putString(key: String, value: String) {
        data[key] = value
        persist()
    }

    override fun getInt(key: String, defaultValue: Int): Int = data[key]?.toIntOrNull() ?: defaultValue

    override fun putInt(key: String, value: Int) {
        data[key] = value.toString()
        persist()
    }

    override fun getBoolean(key: String, defaultValue: Boolean): Boolean = data[key]?.toBooleanStrictOrNull() ?: defaultValue

    override fun putBoolean(key: String, value: Boolean) {
        data[key] = value.toString()
        persist()
    }

    override fun getFloat(key: String, defaultValue: Float): Float = data[key]?.toFloatOrNull() ?: defaultValue

    override fun putFloat(key: String, value: Float) {
        data[key] = value.toString()
        persist()
    }

    override fun getLong(key: String, defaultValue: Long): Long = data[key]?.toLongOrNull() ?: defaultValue

    override fun putLong(key: String, value: Long) {
        data[key] = value.toString()
        persist()
    }

    override fun remove(key: String) {
        data.remove(key)
        persist()
    }

    override fun clear() {
        data.clear()
        persist()
    }
}
