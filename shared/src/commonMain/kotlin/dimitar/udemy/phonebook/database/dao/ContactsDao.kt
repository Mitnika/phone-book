package dimitar.udemy.phonebook.database.dao

import com.squareup.sqldelight.Query
import dimitar.udemy.phonebook.database.cache.Database
import dimitar.udemy.phonebook.database.cache.DatabaseProvider
import dimitar.udemy.phonebook.models.StateConstants
import dimitar.udemy.phonebook.models.base.BaseContactModel
import dimitar.udemy.phonebook.models.base.BasePhoneModel
import dimitar.udemy.phonebook.models.data.ContactModel
import dimitar.udemy.phonebook.models.data.ExternalContactModel
import dimitar.udemy.phonebook.models.data.PhoneModel
import dimitar.udemy.phonebook.models.data.ProfileModel
import dimitar.udemy.phonebook.presenters.MainPresenter

class ContactsDao {
    private var database: Database = DatabaseProvider.getInstance()
    private var dbQueries = database.dbQuery
    private var phoneNumbersDao = PhoneNumbersDao()

    internal fun getById(id: Long): ProfileModel {
        return ProfileModel(
            dbQueries.retrieveAContactById(id, ::mapToContactModel).executeAsOne(),
            phoneNumbersDao.getByContactId(id)
        )
    }

    private fun mapToContactModel(
        id: Long,
        firstName: String,
        lastName: String,
        picture: String,
        externalId: String?,
        state: String
    ) : ContactModel {
        return ContactModel(
            id          = id,
            baseModel   = BaseContactModel(
                firstName   = firstName,
                lastName    = lastName,
                picture     = picture,
                externalId  = externalId
            ),
            state       = state
        )
    }

    internal fun getByListOfId(ids: List<Long>): List<ContactModel> {
        return dbQueries.retrieveAllContactsForDisplayByListOfId(ids, ::mapToContactModel).executeAsList()
    }

    internal fun deleteById(id: Long) {
        dbQueries.transaction {
            dbQueries.deleteContactFromTheDatabase(id)
            dbQueries.deletePhoneNumbersOfAContact(id)
        }
    }

    internal fun insert(model: ExternalContactModel, state: String) {
        dbQueries.transaction {
            dbQueries.insertContact(
                model.baseContact.firstName,
                model.baseContact.lastName,
                model.baseContact.picture,
                model.baseContact.externalId,
                state
            )
            val contactId = dbQueries.lastIndexRowId().executeAsOne()
            model.phoneNumbers.forEach {
                phoneNumbersDao.insert(it, contactId, state)
            }
        }
    }

    internal fun getAllForDisplay(presenter: MainPresenter) {
        dbQueries.retrieveAllContactsForDisplay(::mapToContactModel).addListener(object : Query.Listener {
            override fun queryResultsChanged() {
                val contacts = dbQueries.retrieveAllContactsForDisplay(::mapToContactModel).executeAsList()
                val result = ArrayList<ProfileModel>()
                contacts.forEach {
                    result.add(
                        ProfileModel(
                            it,
                            phoneNumbersDao.getByContactId(it.id)
                        )
                    )
                }
                presenter.setNewContacts(result)
            }
        })
        val contacts = dbQueries.retrieveAllContactsForDisplay(::mapToContactModel).executeAsList()
        val result = ArrayList<ProfileModel>()
        contacts.forEach {
            result.add(
                ProfileModel(
                    it,
                    phoneNumbersDao.getByContactId(it.id)
                )
            )
        }
        presenter.setNewContacts(result)
    }

    internal fun getByExternalId(externalId: String): ProfileModel? {
        val contact = dbQueries.retrieveAnImportedContactByExtrenalId(externalId, ::mapToContactModel).executeAsOneOrNull()
        return if (contact == null) null else ProfileModel(contact, phoneNumbersDao.getByContactId(contact.id))
    }

    internal fun deleteRedundant(externalIds: List<String>) {
        dbQueries.transaction {
            dbQueries.deleteContactsWhichWereDeletedFromTheDevice(externalIds)
        }
    }

    private fun updateFirstName(id: Long, name: String, state: String) {
        dbQueries.updateFirstNameOfAContact(
            name,
            state,
            id
        )
    }

    private fun updateLastName(id: Long, name: String, state: String) {
        dbQueries.updateLastNameOfAContact(
            name,
            state,
            id
        )
    }

    private fun updatePicture(id: Long, picture: String, state: String) {
        dbQueries.updatePictureOfAContact(
            picture,
            state,
            id
        )
    }

    internal fun syncFromOutside(contactFromDB: ProfileModel, contact: ExternalContactModel) {
        dbQueries.transaction {
            if (contactFromDB.contactModel.state == StateConstants.STATE_DELETED) return@transaction
            contact.phoneNumbers.forEach {
                syncPhoneNumber(it, contactFromDB.contactModel.id)
            }
            if (contactFromDB.contactModel.state == StateConstants.STATE_UNEDITABLE) return@transaction
            if (contactFromDB.contactModel.baseModel.firstName != contact.baseContact.firstName) {
                updateFirstName(contactFromDB.contactModel.id, contact.baseContact.firstName, StateConstants.STATE_EDITABLE)
            }
            if (contactFromDB.contactModel.baseModel.lastName != contact.baseContact.lastName) {
                updateLastName(contactFromDB.contactModel.id, contact.baseContact.lastName, StateConstants.STATE_EDITABLE)
            }
            if (contactFromDB.contactModel.baseModel.picture != contact.baseContact.picture) {
                updatePicture(contactFromDB.contactModel.id, contact.baseContact.picture, StateConstants.STATE_EDITABLE)
            }
        }
    }

    private fun syncPhoneNumber(number: BasePhoneModel, contactId: Long) {
        val phoneFromDB = phoneNumbersDao.getByExternalId(number.externalId!!)
        if (phoneFromDB == null) {
            phoneNumbersDao.insert(number, contactId, StateConstants.STATE_EDITABLE)
        } else {
            if (phoneFromDB.state == StateConstants.STATE_UNEDITABLE
                || phoneFromDB.state == StateConstants.STATE_DELETED) return
            if (phoneFromDB.baseModel.number != number.number) {
                phoneNumbersDao.update(
                    PhoneModel(
                    phoneFromDB.id,
                    BasePhoneModel(
                        number.number,
                        phoneFromDB.baseModel.externalId
                    ),
                    StateConstants.STATE_EDITABLE
                )
                )
            }
        }
    }

    internal fun update(profile: ProfileModel, profileFromDB: ProfileModel) {
        val contact = profile.contactModel
        dbQueries.transaction {
            if (profileFromDB.contactModel.baseModel.firstName != contact.baseModel.firstName) {
                updateFirstName(profileFromDB.contactModel.id, contact.baseModel.firstName, StateConstants.STATE_UNEDITABLE)
            }
            if (profileFromDB.contactModel.baseModel.lastName != contact.baseModel.lastName) {
                updateLastName(profileFromDB.contactModel.id, contact.baseModel.lastName, StateConstants.STATE_UNEDITABLE)
            }
            if (profileFromDB.contactModel.baseModel.picture != contact.baseModel.picture) {
                updatePicture(profileFromDB.contactModel.id, contact.baseModel.picture, StateConstants.STATE_UNEDITABLE)
            }
            profile.phones.forEach {
                phoneNumbersDao.update(it, profile.contactModel.id)
            }
        }
    }



    internal fun markDeletedById(id: Long) {
        dbQueries.transaction {
            dbQueries.markAsDeletedAContact(id)
            dbQueries.markAsDeletedPhoneNumbersOfAContact(id)
        }
    }

    internal fun getLastInsertedId(): Long {
        return dbQueries.lastIndexRowId().executeAsOne()

    }
}