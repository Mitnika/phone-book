package dimitar.udemy.phonebook.phonedata

import android.content.ContentResolver
import android.content.Context
import android.provider.ContactsContract
import android.util.Log
import dimitar.udemy.phonebook.models.base.BaseContactModel
import dimitar.udemy.phonebook.models.base.BasePhoneModel
import dimitar.udemy.phonebook.models.data.ExternalContactModel
import dimitar.udemy.phonebook.presenters.MainPresenter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

actual class ContactRetriever(private val context: Context) {

    internal actual fun requestContactsFromPhone(mainPresenter: MainPresenter) {
        CoroutineScope(Dispatchers.IO).launch {
            kotlin.runCatching {
                mainPresenter.synchronizeDatabases(getContactsFromPhone())
            }.onSuccess {
                mainPresenter.onSuccessfulUpdateOfImports()
            }.onFailure {
                mainPresenter.onFailedUpdateOfImports()
            }
        }
    }

    private fun getContactsFromPhone(): ArrayList<ExternalContactModel> {
        val result = ArrayList<ExternalContactModel>()
        val contactsCursor = context.contentResolver.query(
            ContactsContract.Contacts.CONTENT_URI,
            null,
            null,
            null,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
        )

        if (contactsCursor != null && contactsCursor.count > 0) {

            val idIndex         = contactsCursor.getColumnIndex(ContactsContract.Contacts._ID)
            val pictureIndex    = contactsCursor.getColumnIndex(ContactsContract.Contacts.PHOTO_URI)

            while (contactsCursor.moveToNext()) {
                val externalId  = contactsCursor.getString(idIndex)
                val names       = getNamesOfUser(externalId)
                val picture     = contactsCursor.getString(pictureIndex) ?: ""
                val cont        = BaseContactModel(names?.first ?: "", names?.second ?: "", picture, externalId)
                result.add(ExternalContactModel(cont, getContactNumbers(externalId)))
            }
        }
        contactsCursor?.close()
        return result
    }


    private fun getNamesOfUser(contactId: String): Pair<String?, String?>? {
        val nameCursor = context.contentResolver.query(
            ContactsContract.Data.CONTENT_URI,
            null,
            ContactsContract.Data.MIMETYPE + " = ? AND " + ContactsContract.Data.CONTACT_ID + "= ?",
            arrayOf(ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE ,contactId),
            null
        )

        var result: Pair<String, String>? = null

        if (nameCursor != null && nameCursor.count > 0)  {
            val givenNameIndex      = nameCursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME)
            val familyNameIndex     = nameCursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME)

            while (nameCursor.moveToNext()) {
                val firstName   = nameCursor.getString(givenNameIndex) ?: ""
                val lastName    = nameCursor.getString(familyNameIndex) ?: ""

                Log.i("Given name: ", firstName)
                Log.i("Family name: ", lastName)

                if (result == null) {
                    result = Pair(firstName, lastName)
                }
            }
        }
        nameCursor?.close()
        return result
    }

    private fun getContactNumbers(contactId: String): ArrayList<BasePhoneModel> {
        val result = ArrayList<BasePhoneModel>()

        val phoneCursor = context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            null,
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID + "= ?",
            arrayOf(contactId),
            null
        )

        if (phoneCursor != null && phoneCursor.count > 0) {
            val phoneIdIndex    = phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone._ID)
            val numberIndex     = phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            val contactIdIndex  = phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)

            while (phoneCursor.moveToNext()) {
                val phoneNumber         = phoneCursor.getString(numberIndex)
                val idPhone             = phoneCursor.getString(phoneIdIndex)
                val contactExternalId   = phoneCursor.getString(contactIdIndex)

                Log.i("Phone Number", "Id: $idPhone Phone Number: $phoneNumber Contact Id: $contactExternalId")

                result.add(BasePhoneModel(phoneNumber, idPhone))
            }
        }
        phoneCursor?.close()
        return result
    }
}