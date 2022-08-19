package dimitar.udemy.phonebook.android.activities

import android.Manifest
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.ContactsContract
import android.util.Log
import android.view.View
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import dimitar.udemy.phonebook.android.R
import dimitar.udemy.phonebook.android.adapters.RecyclerViewAdapterMain
import dimitar.udemy.phonebook.android.databinding.ActivityMainBinding
import dimitar.udemy.phonebook.android.databinding.DialogProgressBinding
import dimitar.udemy.phonebook.database.cache.DatabaseDriverFactory
import dimitar.udemy.phonebook.models.base.BaseContactModel
import dimitar.udemy.phonebook.models.base.BasePhoneModel
import dimitar.udemy.phonebook.models.data.ExternalContactModel
import dimitar.udemy.phonebook.models.visuals.MainContactVisualization
import dimitar.udemy.phonebook.presenters.MainPresenter
import kotlinx.coroutines.launch
import kotlinx.coroutines.*
import me.zhanghai.android.fastscroll.FastScrollerBuilder


class MainActivity : AppCompatActivity(), MainPresenter.View {

    private var binding         : ActivityMainBinding?      = null
    private var itemAdapter     : RecyclerViewAdapterMain?  = null
    private val presenter       : MainPresenter             = MainPresenter(this, DatabaseDriverFactory(this))
    private var progressDialog  : Dialog?                   = null
    private val unconfinedScope                             = CoroutineScope(Dispatchers.IO)


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding?.root)
        setUpActionBar()
    }

    override fun onResume() {
        super.onResume()
        presenter.subscribe()
    }

    override fun onDestroy() {
        super.onDestroy()
        presenter.detachView()
        unconfinedScope.cancel()
        binding = null
    }

    override fun onPause() {
        super.onPause()
        binding?.searchViewContact?.setQuery("", true)
        presenter.unsubscribe()
    }

    override fun visualizeContacts(contacts: List<MainContactVisualization>) {
        if (itemAdapter == null) {
            runOnUiThread {
                setUpRecyclerView(contacts)
                itemAdapter!!.refresh()
                setUpSearchView()
            }
        } else {
            itemAdapter!!.addNew(ArrayList(contacts))
        }

    }

    private fun setUpSearchView() {
        binding?.searchViewContact?.setOnQueryTextListener(object : SearchView.OnQueryTextListener{
            override fun onQueryTextSubmit(query: String?): Boolean {
                itemAdapter?.getFilter()?.filter(query)
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                itemAdapter?.getFilter()?.filter(newText)
                return true
            }

        })
    }

    private fun setUpRecyclerView(contacts: List<MainContactVisualization>) {
        binding?.rvAllContacts?.layoutManager = LinearLayoutManager(
            this,
            LinearLayoutManager.VERTICAL,
            false
        )
        itemAdapter = RecyclerViewAdapterMain(ArrayList(contacts), this@MainActivity, presenter)

        binding?.rvAllContacts?.adapter = itemAdapter
        itemAdapter!!.refresh()

        FastScrollerBuilder(binding?.rvAllContacts!!)
            .setPopupStyle{
                it.textSize = 20f
                it.setBackgroundColor(Color.parseColor("#03fce3"))
            }
            .setThumbDrawable(ContextCompat.getDrawable(this, R.drawable.ic_sideways_arrow)!!)
            .build()

        setUpSearchView()
    }

    override fun onRequestPermissionsResult(
        requestCode     : Int,
        permissions     : Array<out String>,
        grantResults    : IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_PERMISSIONS -> {
                presenter.setAllPermissions(grantResults[PERMISSION_READ_STORAGE] == PackageManager.PERMISSION_GRANTED
                        && grantResults[PERMISSION_READ_CONTACTS] == PackageManager.PERMISSION_GRANTED
                        && grantResults[PERMISSION_CAMERA] == PackageManager.PERMISSION_GRANTED
                        && grantResults[PERMISSION_WRITE_STORAGE] == PackageManager.PERMISSION_GRANTED)
            }
        }
    }


    override fun requestAllPermissions() {
        this.requestPermissions(arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ), REQUEST_PERMISSIONS
        )
    }

    override fun areAllPermissionsGranted(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
    }

    override fun showPermissionError() {
        binding?.permissionNotGranted?.visibility = View.VISIBLE
    }

    override fun hidePermissionError() {
        binding?.permissionNotGranted?.visibility = View.GONE
    }

    override fun loadAddNewContact() {

        val intent = Intent(this, AddContactActivity::class.java)
        startActivity(intent)

    }

    override fun loadOverviewContact(id: Long) {
       val intent = Intent(this, OverviewContactActivity::class.java)
        intent.putExtra(ID_EXTRA, id)
        startActivity(intent)
    }

    override fun showLoadingDialog() {
        runOnUiThread {
            progressDialog = Dialog(this)
            progressDialog!!.setContentView(DialogProgressBinding.inflate(layoutInflater).root)
            progressDialog!!.show()
        }

    }

    override fun hideLoadingDialog() {
        runOnUiThread {
            if (progressDialog != null) {
                progressDialog!!.dismiss()
            }
        }
    }

    override fun requestInformation() {
        unconfinedScope.launch {
            kotlin.runCatching {
                presenter.accessInformation()
            }.onSuccess {
                presenter.onSuccessfulRetrievalOfInformation()
            }.onFailure {
                presenter.onFailedRetrievalOfInformation()
            }
        }
    }

    override fun requestContactsFromPhone() {
        unconfinedScope.launch {
            kotlin.runCatching {
                presenter.synchronizeDatabases(getContactsFromPhone())
            }.onSuccess {
                presenter.onSuccessfulUpdateOfImports()
            }.onFailure {
                presenter.onFailedUpdateOfImports()
            }
        }
    }

    private fun setUpActionBar() {
        binding?.addContact?.setOnClickListener {
            presenter.addNewContactRequest()
        }
    }


    private fun getContactsFromPhone(): ArrayList<ExternalContactModel> {
        val result = ArrayList<ExternalContactModel>()
        val contactsCursor = contentResolver?.query(
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
        val nameCursor = contentResolver.query(
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

        val phoneCursor = contentResolver.query(
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

    companion object {
        const val PERMISSION_READ_STORAGE   = 0
        const val PERMISSION_READ_CONTACTS  = 1
        const val PERMISSION_CAMERA         = 2
        const val PERMISSION_WRITE_STORAGE  = 3
        const val REQUEST_PERMISSIONS       = 4
        const val ID_EXTRA                  = "id_extra"
    }
}
