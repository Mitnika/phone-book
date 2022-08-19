package dimitar.udemy.phonebook.presenters

import dimitar.udemy.phonebook.database.dao.ContactsDao
import dimitar.udemy.phonebook.database.dao.PhoneNumbersDao
import dimitar.udemy.phonebook.datamanagers.ContactManager
import dimitar.udemy.phonebook.datamanagers.ContactManagerProvider
import dimitar.udemy.phonebook.models.data.ProfileModel

class OverviewContactPresenter(private val view: View) {

    private val contactManager  : ContactManager    = ContactManagerProvider.getInstance()
    private var contact         : ProfileModel?     = null

    private fun requestInformationForContact(id: Long) {
        try {
            contact = contactManager.getContactById(id)

            view.loadInformationForContact(contact!!)
            view.onSuccessfulRetrievalOfInformation()
        } catch (e: Exception) {
            view.onFailedRetrievalOfInformation()
        }
    }

    fun subscribe() {
        requestInformationForContact(view.getIdExtra())
    }

    fun unsubscribe() {

    }

    fun detachView() {

    }

    fun deleteAContact() {
        if (contact != null) {
            try {
                contactManager.deleteAContact(contact!!)
                view.onSuccessfulDelete()
            } catch (e: Exception) {
                view.onFailedDelete()
            }

        } else {
            view.onFailedDelete()
        }
    }

    fun goToEditView() {
        view.loadEditView(contact!!.contactModel.id)
    }

    interface View {
        fun loadInformationForContact(contact: ProfileModel)

        fun loadEditView(id: Long)

        fun onSuccessfulRetrievalOfInformation()

        fun onFailedRetrievalOfInformation()

        fun getIdExtra(): Long

        fun onSuccessfulDelete()

        fun onFailedDelete()
    }

}