package dimitar.udemy.phonebook.android.activities

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import dimitar.udemy.phonebook.android.R
import dimitar.udemy.phonebook.android.adapters.RecyclerViewAdapterOverview
import dimitar.udemy.phonebook.android.databinding.ActivityOverviewContactBinding
import dimitar.udemy.phonebook.models.data.ProfileModel
import dimitar.udemy.phonebook.presenters.OverviewContactPresenter

class OverviewContactActivity : AppCompatActivity(), OverviewContactPresenter.View {

    private var binding     : ActivityOverviewContactBinding?   = null
    private var itemAdapter : RecyclerViewAdapterOverview?      = null
    private val presenter   : OverviewContactPresenter          = OverviewContactPresenter(this)

    companion object {
        const val ID_EXTRA = "id_extra"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOverviewContactBinding.inflate(layoutInflater)
        setContentView(binding?.root)

        binding?.editContact?.setOnClickListener {
            presenter.goToEditView()
        }

        binding?.deleteContact?.setOnClickListener {
            presenter.deleteAContact()
        }
    }

    override fun onResume() {
        super.onResume()
        presenter.subscribe()
    }

    override fun onPause() {
        super.onPause()
        presenter.unsubscribe()
    }

    override fun onDestroy() {
        super.onDestroy()
        presenter.detachView()
    }

    override fun loadInformationForContact(contact: ProfileModel) {
        if (contact.contactModel.baseModel.picture.isNotEmpty()) {
            Glide
                .with(this)
                .load(Uri.parse(contact.contactModel.baseModel.picture))
                .centerCrop()
                .placeholder(R.drawable.ic_baseline_person_24)
                .into(binding?.civProfilePic!!)
        }

        binding?.tvFName?.text = contact.contactModel.baseModel.firstName
        binding?.tvLName?.text = contact.contactModel.baseModel.lastName

        binding?.rvPhoneNumberList?.layoutManager = LinearLayoutManager(
            this,
            LinearLayoutManager.VERTICAL,
            false
        )
        itemAdapter = RecyclerViewAdapterOverview(contact.phones, this)
        binding?.rvPhoneNumberList?.adapter = itemAdapter

    }

    override fun loadEditView(id: Long) {
        val intent = Intent(this, EditContactActivity::class.java)
        intent.putExtra(ID_EXTRA, id)
        startActivity(intent)
    }

    override fun onSuccessfulRetrievalOfInformation() {
        Toast.makeText(
            this,
            resources.getString(R.string.on_success_information),
            Toast.LENGTH_SHORT
        ).show()
    }

    override fun onFailedRetrievalOfInformation() {
        Toast.makeText(
            this,
            resources.getString(R.string.on_failed_retrieval),
            Toast.LENGTH_LONG
        ).show()
    }

    override fun getIdExtra(): Long {
        return intent.getLongExtra(MainActivity.ID_EXTRA, -1)
    }

    override fun onSuccessfulDelete() {
        Toast.makeText(
            applicationContext,
            resources.getString(R.string.successful_deletion),
            Toast.LENGTH_SHORT
        ).show()
        onBackPressed()
    }

    override fun onFailedDelete() {
        Toast.makeText(
            this,
            resources.getString(R.string.failed_deletion),
            Toast.LENGTH_LONG
        ).show()
    }
}