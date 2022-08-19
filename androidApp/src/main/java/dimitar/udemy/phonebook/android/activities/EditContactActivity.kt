package dimitar.udemy.phonebook.android.activities

import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import dimitar.udemy.phonebook.android.R
import dimitar.udemy.phonebook.android.adapters.RecyclerViewAdapterEdit
import dimitar.udemy.phonebook.android.databinding.ActivityAddEditContactBinding
import dimitar.udemy.phonebook.models.InvalidType
import dimitar.udemy.phonebook.models.base.BaseContactModel
import dimitar.udemy.phonebook.models.base.BasePhoneModel
import dimitar.udemy.phonebook.models.data.ContactModel
import dimitar.udemy.phonebook.models.data.ProfileModel
import dimitar.udemy.phonebook.presenters.EditContactPresenter
import kotlinx.coroutines.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.collections.ArrayList

class EditContactActivity : AppCompatActivity(), EditContactPresenter.View {

    private var binding         : ActivityAddEditContactBinding?    = null
    private var presenter       : EditContactPresenter              = EditContactPresenter(this)
    private var itemAdapter     : RecyclerViewAdapterEdit?          = null
    private val ioScope                                             = CoroutineScope(Dispatchers.IO)
    private val mainScope                                           = CoroutineScope(Dispatchers.Main)

    private var             imageCapture    : ImageCapture? = null
    private lateinit var    outputDirectory : File
    private lateinit var    cameraExecutor  : ExecutorService

    companion object {
        private const val TAG               = "PhonebookApp"
        private const val FILENAME_FORMAT   = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val GALLERY           = 0
        private const val CAMERA            = 1
    }

    private val selectImageFromGalleryResult = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let {
            presenter.onSuccessfulLoadOfPicture(uri.toString())
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddEditContactBinding.inflate(layoutInflater)
        setContentView(binding?.root)

        outputDirectory = getOutputDirectory()
        cameraExecutor  = Executors.newSingleThreadExecutor()

        binding?.civProfilePic?.setOnClickListener {
            presenter.changeOfImage()
        }

        binding?.ibDoneNumberInput?.setOnClickListener {
            itemAdapter?.addAPhoneNumber(BasePhoneModel(binding?.etNumber?.text?.toString() ?: "", null))
            binding?.etNumber?.setText("")
        }

        binding?.btnSaveChanges?.setOnClickListener {
            onSavePressed()
        }
    }

    override fun onResume() {
        super.onResume()
        presenter.subscribe()
    }

    override fun onDestroy() {
        super.onDestroy()
        presenter.detachView()
        binding = null
        ioScope.cancel()
        mainScope.cancel()
    }

    override fun onPause() {
        super.onPause()
        presenter.unsubscribe()
    }

    override fun setImage(uri: String) {
        Glide
            .with(this)
            .load(uri)
            .centerCrop()
            .placeholder(R.drawable.ic_baseline_person_24)
            .into(binding?.civProfilePic!!)
    }

    override fun startCamera() {
        binding?.viewFinder?.preferredImplementationMode = PreviewView.ImplementationMode.TEXTURE_VIEW

        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener( {
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Preview
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(binding!!.viewFinder.createSurfaceProvider())
                }

            imageCapture = ImageCapture.Builder()
                .build()

            // Select back camera as a default
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture)

            } catch(exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun getOutputDirectory(): File {
        val mediaDir = externalMediaDirs.firstOrNull()?.let {
            File(it, resources.getString(R.string.app_name)).apply { mkdirs() }
        }
        return  if (mediaDir != null && mediaDir.exists())  mediaDir
                else                                        filesDir
    }

    private fun onSavePressed() {
        val contact = ProfileModel(
            contactModel = ContactModel(
                id          = presenter.getId(),
                baseModel   = BaseContactModel(
                    firstName   = binding!!.etFName.text.toString(),
                    lastName    = binding!!.etLName.text.toString(),
                    picture     = presenter.getPicture(),
                    externalId  = presenter.getExternalId()
                ),
                state       = presenter.getState()
            ),
            phones      = itemAdapter!!.getPhoneNumbers()
        )
        ioScope.launch {
            kotlin.runCatching {
                presenter.onEditClicked(contact)
            }.onFailure {
                onEditFailure()
            }.onSuccess {
                onEditSuccess()
            }
        }
    }

    override fun getIdExtra() {
        ioScope.runCatching {
            presenter.getInformation(intent.getLongExtra(OverviewContactActivity.ID_EXTRA, -1))
        }.onFailure {
            onFailedRetrievalOfInformation()
        }.onSuccess {
            onSuccessfulRetrievalOfInformation()
        }
    }

    override fun onSuccessfulRetrievalOfInformation() {
        mainScope.launch {
            Toast.makeText(this@EditContactActivity, resources.getString(R.string.on_success_information), Toast.LENGTH_SHORT).show()
        }
    }

    override fun onFailedRetrievalOfInformation() {
        mainScope.launch {
            Toast.makeText(this@EditContactActivity, resources.getString(R.string.on_failed_retrieval), Toast.LENGTH_LONG).show()
        }
    }

    override fun onEditSuccess() {
        mainScope.launch {
            Toast.makeText(applicationContext, resources.getString(R.string.edit_success), Toast.LENGTH_SHORT).show()
            onBackPressed()
        }
    }

    override fun onEditFailure() {
        mainScope.launch { Toast.makeText(this@EditContactActivity, resources.getString(R.string.edit_failure), Toast.LENGTH_LONG).show()
        }
    }

    override fun onInvalidField(kind: InvalidType) {
        mainScope.launch{
            Toast.makeText(this@EditContactActivity, kind.errorMessage, Toast.LENGTH_LONG).show()
        }
    }

    override fun loadInformationForAContact(contact: ProfileModel) {
        mainScope.launch {
            if (contact.contactModel.baseModel.picture.isNotEmpty()) {
                binding?.civProfilePic?.setImageURI(Uri.parse(contact.contactModel.baseModel.picture))
            }

            binding?.etFName?.setText(contact.contactModel.baseModel.firstName)
            binding?.etLName?.setText(contact.contactModel.baseModel.lastName)

            binding?.rvPhoneNumberList?.layoutManager = LinearLayoutManager(this@EditContactActivity, LinearLayoutManager.VERTICAL, false)

            itemAdapter = RecyclerViewAdapterEdit(ArrayList(contact.phones))

            binding?.rvPhoneNumberList?.adapter = itemAdapter

            itemAdapter!!.visualize()
        }
    }

    override fun openDialogToChooseOptionForImage() {
        val picDialog = AlertDialog.Builder(this)
        picDialog.setTitle(resources.getString(R.string.select_action))
        val pictureDialogOptions = arrayOf(
            resources.getString(R.string.library_choice),
            resources.getString(R.string.camera_choice)
        )

        picDialog.setItems(pictureDialogOptions) {
                _, which ->
            when (which) {
                GALLERY -> chooseImageFromGallery()
                CAMERA -> goToCamera()
            }
        }
        picDialog.show()
    }


    private fun chooseImageFromGallery() {
        selectImageFromGalleryResult.launch(null)
    }

    private fun goToCamera() {
        startCamera()
        binding?.viewFinder?.visibility     = View.VISIBLE
        binding?.cardView?.visibility       = View.GONE

        binding?.viewFinder?.setOnClickListener {
            takePhotoFromCamera()
        }
    }

    private fun takePhotoFromCamera() {
        // Get a stable reference of the modifiable image capture use case
        val imageCapture = imageCapture ?: return

        // Create time-stamped output file to hold the image
        val photoFile = File(
            outputDirectory,
            SimpleDateFormat(
                FILENAME_FORMAT, Locale.US
            ).format(System.currentTimeMillis()) + ".jpg")

        // Create output options object which contains file + metadata
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        // Set up image capture listener, which is triggered after photo has
        // been taken
        imageCapture.takePicture(
            outputOptions, ContextCompat.getMainExecutor(this), object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, resources.getString(R.string.msg_image_save, exc.message), exc)
                    binding?.viewFinder?.visibility = View.GONE
                    binding?.cardView?.visibility = View.VISIBLE
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val savedUri    = Uri.fromFile(photoFile)
                    val msg         = resources.getString(R.string.successful_capture, savedUri)
                    Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()

                    Log.d(TAG, msg)

                    presenter.onSuccessfulLoadOfPicture(savedUri.toString())
                    binding?.civProfilePic?.setImageURI(savedUri)

                    binding?.viewFinder?.visibility = View.GONE
                    binding?.cardView?.visibility   = View.VISIBLE
                }
            })
    }

}