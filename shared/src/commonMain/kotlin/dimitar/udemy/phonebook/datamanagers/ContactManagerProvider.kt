package dimitar.udemy.phonebook.datamanagers

import dimitar.udemy.phonebook.database.dao.ContactsDaoProvider
import dimitar.udemy.phonebook.database.dao.PhoneNumbersDaoProvider

object ContactManagerProvider {

    private var contactManager: ContactManager? = null

    fun getInstance(): ContactManager {
        if (contactManager == null) {
            contactManager = ContactManager(ContactsDaoProvider.getInstance(), PhoneNumbersDaoProvider.getInstance())
        }
        return contactManager!!
    }
}