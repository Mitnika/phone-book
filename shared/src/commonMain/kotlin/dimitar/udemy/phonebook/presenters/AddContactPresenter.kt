package dimitar.udemy.phonebook.presenters

import dimitar.udemy.phonebook.models.InvalidType
import dimitar.udemy.phonebook.models.StateConstants
import dimitar.udemy.phonebook.database.dao.ContactsDao
import dimitar.udemy.phonebook.models.base.BasePhoneModel
import dimitar.udemy.phonebook.models.data.ExternalContactModel

class AddContactPresenter(private val view: View) {

    private val contactsDao = ContactsDao()
    private var picture: String? = null

    fun saveContact(contact: ExternalContactModel) {
        val error = validateContact(contact)
        if (error != null) {
            view.onInvalidField(error)
            throw IllegalArgumentException(error.errorMessage)
        }else {
            try {
                contactsDao.insert(contact, StateConstants.STATE_UNEDITABLE)
                view.onSuccessfulSafeOfContact()
            } catch (e: Exception) {
                view.onFailedSafeOfContact()
            }
        }

    }

    fun getPicture() = picture ?: ""

    private fun validateContact(contact: ExternalContactModel): InvalidType? {
        if (contact.baseContact.firstName.isEmpty()) {
            return InvalidType.EMPTY_FIRST_NAME
        }
        if (contact.baseContact.lastName.isEmpty()) {
            return InvalidType.EMPTY_LAST_NAME
        }
        if (contact.phoneNumbers.isEmpty()) {
            return InvalidType.NO_PHONE_NUMBERS
        }
        contact.phoneNumbers.forEach {
            val res = validatePhoneNumber(it)
            if (res != null) return@validateContact res
        }
        return null
    }

    private fun validatePhoneNumber(phoneNumber: BasePhoneModel): InvalidType? {
        if (phoneNumber.number.isEmpty()) return InvalidType.EMPTY_PHONE_NUMBER
        val regex = Regex("\\+?([0-9])*")
        val invalidSymbols = arrayListOf('(', ')', '#', ' ', '-')
        if (!regex.matches(phoneNumber.number.trim { !invalidSymbols.contains(it) })) return InvalidType.ILLEGAL_SYMBOLS_IN_PHONE_NUMBER
        return null
    }

    fun choosePicture() {
        view.openDialogToChooseOptionForImage()
    }


    fun onSuccessfulLoadOfPicture(picture: String) {
        this.picture = picture
    }

    fun subscribe() {

    }

    fun unsubscribe() {

    }

    fun detachView() {

    }

    interface View {
        fun onInvalidField(kind: InvalidType)

        fun openDialogToChooseOptionForImage()

        fun onSuccessfulSafeOfContact()

        fun onFailedSafeOfContact()
    }

}