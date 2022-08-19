package dimitar.udemy.phonebook.database.dao

object ContactsDaoProvider {

    private var contactsDao: ContactsDao? = null

    fun getInstance(): ContactsDao {
        if (contactsDao == null) {
            contactsDao = ContactsDao()
        }
        return contactsDao!!
    }
}