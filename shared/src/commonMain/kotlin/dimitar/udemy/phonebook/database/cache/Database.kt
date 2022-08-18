package dimitar.udemy.phonebook.database.cache

import dimitar.udemy.phonebook.cache.PhonebookDatabase
import dimitar.udemy.phonebook.database.dao.ContactsDao
import dimitar.udemy.phonebook.database.dao.PhoneNumbersDao
import dimitar.udemy.phonebook.models.data.ContactModel
import dimitar.udemy.phonebook.models.data.ProfileModel
import dimitar.udemy.phonebook.models.visuals.MainContactVisualization

class Database(databaseDriverFactory: DatabaseDriverFactory) {
    private val database = PhonebookDatabase(databaseDriverFactory.createDriver())
    internal val dbQuery = database.phonebookDatabaseQueries


    private fun getPeopleById(ids: List<Long>) : ArrayList<ProfileModel> {
        val result = ArrayList<ProfileModel>()
        val contactsDao = ContactsDao()
        ids.forEach {
            result.add(contactsDao.getById(it))
        }
        return result
    }

    internal fun searchByText(text: String): List<ProfileModel> {

        val people = ContactsDao().getByListOfId(dbQuery.searchForText(text).executeAsList())

        return people.map { ProfileModel(it, PhoneNumbersDao().getByContactId(it.id)) }
    }

}