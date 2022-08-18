package dimitar.udemy.phonebook.database.cache

import android.content.Context
import com.squareup.sqldelight.android.AndroidSqliteDriver
import com.squareup.sqldelight.db.SqlDriver
import dimitar.udemy.phonebook.cache.PhonebookDatabase

actual class DatabaseDriverFactory(private val context: Context) {
    companion object {
        private var driver: SqlDriver? = null
    }


    actual fun createDriver(): SqlDriver {
        synchronized(this) {
            if (driver == null) {
                driver = AndroidSqliteDriver(PhonebookDatabase.Schema, context, "phonebook.db")
            }
            return driver!!
        }
    }
}
