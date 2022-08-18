package dimitar.udemy.phonebook.datamanagers

import dimitar.udemy.phonebook.database.dao.ContactsDao
import dimitar.udemy.phonebook.database.dao.PhoneNumbersDao
import dimitar.udemy.phonebook.models.StateConstants
import dimitar.udemy.phonebook.models.data.ExternalContactModel
import dimitar.udemy.phonebook.models.data.ProfileModel

class ContactManager(
    private val contactsDao: ContactsDao,
    private val phoneNumbersDao: PhoneNumbersDao
) {
    fun syncContacts(contacts: List<ExternalContactModel>) {
        val phoneList: ArrayList<String> = ArrayList()
        val contactList: ArrayList<String> = ArrayList()

        contacts.forEach { externalContactModel ->
            contactList.add(externalContactModel.baseContact.externalId!!)
            phoneList.addAll(externalContactModel.phoneNumbers.map { it.externalId!! })
            syncContact(externalContactModel)
        }

        contactsDao.deleteRedundant(contactList)
        phoneNumbersDao.deleteRedundant(phoneList)
    }

    private fun syncContact(contact: ExternalContactModel) {
        val contactFromDB = contactsDao.getByExternalId(contact.baseContact.externalId!!)

        if (contact.phoneNumbers.isEmpty()) return
        if (contactFromDB == null) {
            try {
                contactsDao.insert(contact, StateConstants.STATE_EDITABLE)
            } catch (e: Exception) {

            }

        } else {
            contactsDao.syncFromOutside(contactFromDB, contact)
        }
    }

    fun updateAProfile(profile: ProfileModel) {
        val profileFromDB = contactsDao.getById(profile.contactModel.id)
        contactsDao.update(profile, profileFromDB)
    }

    fun deleteAContact(contact: ProfileModel) {
        if (contact.contactModel.baseModel.externalId == null) {
            contactsDao.deleteById(contact.contactModel.id)
        } else {
            contactsDao.markDeletedById(contact.contactModel.id)
        }
    }
}