package dimitar.udemy.phonebook.database.cache

import dimitar.udemy.phonebook.cache.PhonebookDatabase
import dimitar.udemy.phonebook.database.dao.ContactsDao
import dimitar.udemy.phonebook.database.dao.PhoneNumbersDao
import dimitar.udemy.phonebook.models.data.ProfileModel

class Database(databaseDriverFactory: DatabaseDriverFactory) {
    private val     database    = PhonebookDatabase(databaseDriverFactory.createDriver())
    internal val    dbQuery     = database.phonebookDatabaseQueries


    internal fun searchByText(text: String, contactsDao: ContactsDao): List<ProfileModel> {
        val people = contactsDao.getByListOfId(dbQuery.searchForText(text).executeAsList())

        return people.map { ProfileModel(it, PhoneNumbersDao().getByContactId(it.id)) }
    }

}