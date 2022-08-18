package dimitar.udemy.phonebook.presenters

import dimitar.udemy.phonebook.database.cache.DatabaseDriverFactory
import dimitar.udemy.phonebook.database.cache.DatabaseProvider
import dimitar.udemy.phonebook.database.dao.ContactsDao
import dimitar.udemy.phonebook.database.dao.PhoneNumbersDao
import dimitar.udemy.phonebook.datamanagers.ContactManager
import dimitar.udemy.phonebook.models.data.ExternalContactModel
import dimitar.udemy.phonebook.models.data.ProfileModel
import dimitar.udemy.phonebook.models.visuals.MainContactVisualization
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainPresenter(private val view: View, databaseDriverFactory: DatabaseDriverFactory) {

    private var contacts: List<ProfileModel>? = null
    private val database = DatabaseProvider.initializeDatabase(databaseDriverFactory)
    private val phoneNumbersDao = PhoneNumbersDao()
    private val contactsDao = ContactsDao()
    private var entirely = true


    fun synchronizeDatabases(contacts: List<ExternalContactModel>) {
        entirely = false
        ContactManager(contactsDao, phoneNumbersDao).syncContacts(contacts)
    }


    suspend fun accessInformation() {
        getContacts()
    }

    fun setAllPermissions(permissionGranted: Boolean) {
        if (permissionGranted) {
            view.hidePermissionError()
            subscribe()
        }
    }

    fun unsubscribe() {

    }

    fun detachView() {

    }

    fun subscribe() {
        if (!view.areAllPermissionsGranted()) {
            view.requestAllPermissions()
            view.showPermissionError()
        } else {
            view.showLoadingDialog()
            if (entirely) {
                entirely = false
                view.requestContactsFromPhone()
            }
            view.requestInformation()
            view.hideLoadingDialog()
        }
    }

    fun contactChosen(id: Long) {
        view.loadOverviewContact(id)
    }

    fun addNewContactRequest() {
        view.loadAddNewContact()
    }

    fun onSuccessfulRetrievalOfInformation() {
        if (contacts != null) {
            view.visualizeContacts(contacts!!.map(::mapToMainVisualModel))
        }
        view.hideLoadingDialog()
    }

    fun setNewContacts(newModels: List<ProfileModel>) {
        contacts = newModels
        view.visualizeContacts(newModels.map(::mapToMainVisualModel))
    }

    private fun mapToMainVisualModel(contact: ProfileModel) : MainContactVisualization {
        val fName = contact.contactModel.baseModel.firstName
        val lName = contact.contactModel.baseModel.lastName
        val displayName = if(fName.isEmpty()
            && lName.isEmpty()) contact.phones[0].baseModel.number
            else "$fName $lName"
        return MainContactVisualization(
            contact.contactModel.id,
            displayName,
            contact.contactModel.baseModel.picture
        )
    }

    fun onFailedRetrievalOfInformation() {
        view.hideLoadingDialog()
    }

    fun onSuccessfulUpdateOfImports() {
        view.hideLoadingDialog()
        subscribe()
    }

    fun onFailedUpdateOfImports() {
        view.hideLoadingDialog()
    }

    fun searchForContacts(text: String) : List<MainContactVisualization>{
        return database.searchByText(text).map(::mapToMainVisualModel)
    }

    private suspend fun getContacts() = withContext(Dispatchers.Default) {
        contactsDao.getAllForDisplay(this@MainPresenter)
    }

    interface View {
        fun visualizeContacts(contacts: List<MainContactVisualization>)

        fun requestAllPermissions()

        fun areAllPermissionsGranted(): Boolean

        fun showPermissionError()

        fun hidePermissionError()

        fun loadAddNewContact()

        fun loadOverviewContact(id: Long)

        fun showLoadingDialog()

        fun hideLoadingDialog()

        fun requestContactsFromPhone()

        fun requestInformation()
    }

}