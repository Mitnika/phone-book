package dimitar.udemy.phonebook.datamanagers

import dimitar.udemy.phonebook.database.cache.Database
import dimitar.udemy.phonebook.database.dao.ContactsDao
import dimitar.udemy.phonebook.database.dao.PhoneNumbersDao
import dimitar.udemy.phonebook.models.StateConstants
import dimitar.udemy.phonebook.models.data.ExternalContactModel
import dimitar.udemy.phonebook.models.data.ProfileModel
import dimitar.udemy.phonebook.presenters.MainPresenter

class ContactManager(
    private val contactsDao     : ContactsDao,
    private val phoneNumbersDao : PhoneNumbersDao,
    private val database        : Database
) {
    fun syncContacts(contacts: List<ExternalContactModel>) {
        val phoneList   : ArrayList<String> = ArrayList()
        val contactList : ArrayList<String> = ArrayList()

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

    fun insertAContact(contact: ExternalContactModel, state: String) {
        contactsDao.insert(contact, state)
    }

    fun getContactById(id: Long): ProfileModel {
        return contactsDao.getById(id)
    }

    fun getAllForDisplay(presenter: MainPresenter) {
        contactsDao.getAllForDisplay(presenter)
    }

    fun searchByText(text: String): List<ProfileModel> {
        return database.searchByText(text, contactsDao)
    }
}