package dimitar.udemy.phonebook.database.dao

import dimitar.udemy.phonebook.database.cache.Database
import dimitar.udemy.phonebook.database.cache.DatabaseProvider
import dimitar.udemy.phonebook.models.StateConstants
import dimitar.udemy.phonebook.models.base.BasePhoneModel
import dimitar.udemy.phonebook.models.data.PhoneModel

class PhoneNumbersDao {

    private var database: Database = DatabaseProvider.getInstance()
    private var dbQueries = database.dbQuery

    internal fun update(phone: PhoneModel) {
        dbQueries.transaction {
            dbQueries.updatePhoneNumber(
                phone.baseModel.number,
                phone.state,
                phone.id
            )
        }
    }

    internal fun insert(phone: BasePhoneModel, contactId: Long, state: String) {
        dbQueries.insertPhone(
            number =        phone.number,
            contactId =     contactId,
            externalId =    phone.externalId,
            state =         state
        )
    }

    internal fun getById(id: Long): PhoneModel? {
        return dbQueries.retrieveAPhoneById(id, ::mapToPhoneModel).executeAsOneOrNull()
    }

    internal fun getByExternalId(externalId: String): PhoneModel? {
        return dbQueries.retrieveAPhoneNumberByExternalId(externalId, ::mapToPhoneModel).executeAsOneOrNull()
    }

    private fun mapToPhoneModel(
        id: Long,
        number: String,
        state: String,
        contactId: Long,
        externalId: String?
    ) : PhoneModel {
        return PhoneModel(
            id = id,
            BasePhoneModel(
                number = number,
                externalId = externalId
            ),
            state = state
        )
    }

    internal fun getByContactId(contactId: Long): List<PhoneModel> {
        return dbQueries.retrieveAllTelephoneNumbersAvailableForAUser(contactId, ::mapToPhoneModel).executeAsList()
    }

    internal fun deleteRedundant(externalIds: List<String>) {
        dbQueries.transaction {
            dbQueries.deletePhoneNumbersWhichWereDeletedFromDevice(externalIds)
        }
    }

    internal fun markAsDeletedById(id: Long) {
        dbQueries.markAsDeletedAPhoneNumber(id)
    }

    internal fun markDeletedByContactId(contactId: Long) {
        dbQueries.markAsDeletedPhoneNumbersOfAContact(contactId)
    }

    internal fun deleteById(id: Long) {
        dbQueries.deletePhoneFromTheDatabase(id)
    }

    internal fun getLastInsertedId(): Long {
        return dbQueries.lastIndexRowId().executeAsOne()
    }

    internal fun update(number: PhoneModel, contactId: Long) {

        val phoneFromDB = getById(number.id)

        if (phoneFromDB != null) {
            if (number.state == StateConstants.STATE_DELETED) {
                deleteByModel(phoneFromDB)
                return
            }
            if (phoneFromDB.baseModel.number != number.baseModel.number) {
                update(PhoneModel(
                    number.id,
                    number.baseModel,
                    StateConstants.STATE_UNEDITABLE
                ))
            }
        } else {
            if (number.state == StateConstants.STATE_DELETED) {
                if (number.baseModel.externalId != null) {
                    markAsDeletedById(number.id)
                }
                return
            }
            insert(
                number.baseModel,
                contactId,
                StateConstants.STATE_UNEDITABLE
            )
        }
    }

    private fun deleteByModel(phoneNumber: PhoneModel) {
        if (phoneNumber.baseModel.externalId == null) {
            deleteById(phoneNumber.id)
        } else {
            markAsDeletedById(phoneNumber.id)
        }
    }

}