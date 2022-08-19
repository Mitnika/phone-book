package dimitar.udemy.phonebook.android.activities

import android.Manifest
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import dimitar.udemy.phonebook.android.R
import dimitar.udemy.phonebook.android.adapters.RecyclerViewAdapterMain
import dimitar.udemy.phonebook.android.databinding.ActivityMainBinding
import dimitar.udemy.phonebook.android.databinding.DialogProgressBinding
import dimitar.udemy.phonebook.database.cache.DatabaseDriverFactory
import dimitar.udemy.phonebook.models.visuals.MainContactVisualization
import dimitar.udemy.phonebook.phonedata.ContactRetriever
import dimitar.udemy.phonebook.presenters.MainPresenter
import kotlinx.coroutines.launch
import kotlinx.coroutines.*
import me.zhanghai.android.fastscroll.FastScrollerBuilder


class MainActivity : AppCompatActivity(), MainPresenter.View {

    private var binding         : ActivityMainBinding?      = null
    private var itemAdapter     : RecyclerViewAdapterMain?  = null
    private val presenter       : MainPresenter             = MainPresenter(this, DatabaseDriverFactory(this), ContactRetriever(this))
    private var progressDialog  : Dialog?                   = null
    private val ioScope                                     = CoroutineScope(Dispatchers.IO)
    private val mainScope                                   = CoroutineScope(Dispatchers.Main)

    companion object {
        const val PERMISSION_READ_STORAGE   = 0
        const val PERMISSION_READ_CONTACTS  = 1
        const val PERMISSION_CAMERA         = 2
        const val PERMISSION_WRITE_STORAGE  = 3
        const val REQUEST_PERMISSIONS       = 4
        const val ID_EXTRA                  = "id_extra"
    }

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
        ioScope.cancel()
        mainScope.cancel()
        binding = null
    }

    override fun onPause() {
        super.onPause()
        binding?.searchViewContact?.setQuery("", true)
        presenter.unsubscribe()
    }

    override fun visualizeContacts(contacts: List<MainContactVisualization>) {
        if (itemAdapter == null) {
            mainScope.launch {
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
                it.setBackgroundColor(ContextCompat.getColor(applicationContext, R.color.background_color))
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
        mainScope.launch {
            binding?.permissionNotGranted?.visibility = View.VISIBLE
        }
    }

    override fun hidePermissionError() {
        mainScope.launch {
            binding?.permissionNotGranted?.visibility = View.GONE
        }
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
        mainScope.launch {
            progressDialog = Dialog(this@MainActivity)
            progressDialog!!.setContentView(DialogProgressBinding.inflate(layoutInflater).root)
            progressDialog!!.show()
        }

    }

    override fun hideLoadingDialog() {
        mainScope.launch {
            if (progressDialog != null) {
                progressDialog!!.dismiss()
            }
        }
    }

    override fun requestInformation() {
        ioScope.launch {
            kotlin.runCatching {
                presenter.accessInformation()
            }.onSuccess {
                presenter.onSuccessfulRetrievalOfInformation()
            }.onFailure {
                presenter.onFailedRetrievalOfInformation()
            }
        }
    }

    private fun setUpActionBar() {
        binding?.addContact?.setOnClickListener {
            presenter.addNewContactRequest()
        }
    }

}
