package data.db

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import io.panel.reader.db.PanelDatabase
import java.io.File

actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver {
        val userHome = System.getProperty("user.home") ?: "."
        val appDir = File(userHome, ".dokuha").apply { mkdirs() }
        val dbFile = File(appDir, "dokuha.db")
        val isNewDb = !dbFile.exists()
        val driver = JdbcSqliteDriver("jdbc:sqlite:${dbFile.absolutePath}")
        if (isNewDb) {
            PanelDatabase.Schema.create(driver)
        }
        return driver
    }
}
