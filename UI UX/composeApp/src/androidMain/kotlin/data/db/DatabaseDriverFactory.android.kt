package data.db

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import io.panel.reader.db.PanelDatabase

actual class DatabaseDriverFactory(private val context: Context) {
    actual fun createDriver(): SqlDriver {
        return AndroidSqliteDriver(
            schema = PanelDatabase.Schema,
            context = context,
            name = "dokuha.db",
            callback = object : AndroidSqliteDriver.Callback(PanelDatabase.Schema) {
                override fun onConfigure(db: SupportSQLiteDatabase) {
                    super.onConfigure(db)
                    db.enableWriteAheadLogging()
                    db.setForeignKeyConstraintsEnabled(true)
                }
            }
        )
    }
}
