package core.storage

import java.io.File
import java.util.Base64

class DesktopSecureStorage(
    baseDir: File = File(System.getProperty("user.home"), ".config/dokuha")
) : SecureStorage {
    private val file = File(baseDir, "secure_vault.dat")
    private val data = mutableMapOf<String, String>()

    init {
        baseDir.mkdirs()
        if (file.exists()) {
            try {
                val lines = file.readLines()
                for (line in lines) {
                    val idx = line.indexOf('=')
                    if (idx != -1) {
                        val key = line.substring(0, idx)
                        val b64 = line.substring(idx + 1)
                        data[key] = String(Base64.getDecoder().decode(b64))
                    }
                }
            } catch (_: Exception) {}
        }
    }

    private fun persist() {
        try {
            val content = data.entries.joinToString("\n") { (k, v) ->
                val b64 = Base64.getEncoder().encodeToString(v.toByteArray())
                "$k=$b64"
            }
            file.writeText(content)
        } catch (_: Exception) {}
    }

    override fun getSecret(key: String): String? = data[key]

    override fun putSecret(key: String, value: String) {
        data[key] = value
        persist()
    }

    override fun removeSecret(key: String) {
        data.remove(key)
        persist()
    }

    override fun clearSecrets() {
        data.clear()
        persist()
    }
}
