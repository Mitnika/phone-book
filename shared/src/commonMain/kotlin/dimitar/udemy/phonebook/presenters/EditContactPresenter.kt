package dimitar.udemy.phonebook.presenters

import dimitar.udemy.phonebook.datamanagers.ContactManager
import dimitar.udemy.phonebook.datamanagers.ContactManagerProvider
import dimitar.udemy.phonebook.models.InvalidType
import dimitar.udemy.phonebook.models.StateConstants
import dimitar.udemy.phonebook.models.data.PhoneModel
import dimitar.udemy.phonebook.models.data.ProfileModel

class EditContactPresenter(private val view: View) {

    private val contactManager  : ContactManager    = ContactManagerProvider.getInstance()
    private var picture         : String?           = null
    private var contact         : ProfileModel?     = null

    fun getId()             : Long      = contact!!.contactModel.id

    fun getState()          : String    = contact!!.contactModel.state

    fun getPicture()        : String    = picture ?: contact?.contactModel?.baseModel?.picture ?: ""

    fun getExternalId()     : String?   = contact!!.contactModel.baseModel.externalId

    fun subscribe() {
        view.getIdExtra()
    }

    fun unsubscribe() {

    }

    fun detachView() {

    }

    fun getInformation(id: Long) {
        contact = contactManager.getContactById(id)
        view.loadInformationForAContact(contact!!)
    }

    fun onSuccessfulLoadOfPicture(picture: String) {
        this.picture = picture
        view.setImage(picture)
    }

    private fun validateContact(contact: ProfileModel): InvalidType? {
        if (contact.contactModel.baseModel.firstName.isEmpty())
            return InvalidType.EMPTY_FIRST_NAME

        if (contact.contactModel.baseModel.lastName.isEmpty())
            return InvalidType.EMPTY_LAST_NAME

        if (contact.phones.isEmpty())
            return InvalidType.NO_PHONE_NUMBERS

        contact.phones.forEach {
            val res = validatePhoneNumber(it)
            if (res != null) return@validateContact res
        }
        return null
    }

    private fun validatePhoneNumber(phoneNumber: PhoneModel): InvalidType? {
        if (phoneNumber.state == StateConstants.STATE_DELETED)  return null
        if (phoneNumber.baseModel.number.isEmpty())             return InvalidType.EMPTY_PHONE_NUMBER

        val invalidSymbols  = arrayListOf('(', ')', '#', ' ', '-')
        val wholePhoneNum   = phoneNumber.baseModel.number.filter { !invalidSymbols.contains(it) }
        val regex           = Regex("\\+?([0-9])*")

        if (!regex.matches(wholePhoneNum))                      return InvalidType.ILLEGAL_SYMBOLS_IN_PHONE_NUMBER
        return null
    }

    fun onEditClicked(profile: ProfileModel) {
        val error = validateContact(profile)

        if (error != null) {
            view.onInvalidField(error)
            throw IllegalArgumentException(error.errorMessage)
        } else {
            contactManager.updateAProfile(profile)
        }

    }

    fun changeOfImage() {
        view.openDialogToChooseOptionForImage()
    }


    interface View {
        fun startCamera()

        fun getIdExtra()

        fun onSuccessfulRetrievalOfInformation()

        fun onFailedRetrievalOfInformation()

        fun onEditSuccess()

        fun onEditFailure()

        fun onInvalidField(kind: InvalidType)

        fun loadInformationForAContact(contact: ProfileModel)

        fun openDialogToChooseOptionForImage()

        fun setImage(uri: String)
    }
}