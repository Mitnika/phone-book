package dimitar.udemy.phonebook.database.cache

object DatabaseProvider {

    private var database: Database? = null

    fun initializeDatabase(databaseDriverFactory: DatabaseDriverFactory): Database {
        if (database == null) {
            database = Database(databaseDriverFactory)
        }

        return database!!
    }

    fun getInstance(): Database {
        return database!!
    }
}