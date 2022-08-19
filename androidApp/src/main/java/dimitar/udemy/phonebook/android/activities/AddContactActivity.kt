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
import dimitar.udemy.phonebook.android.R
import dimitar.udemy.phonebook.android.adapters.RecyclerViewAdapterAdd
import dimitar.udemy.phonebook.android.databinding.ActivityAddEditContactBinding
import dimitar.udemy.phonebook.models.InvalidType
import dimitar.udemy.phonebook.models.base.BaseContactModel
import dimitar.udemy.phonebook.models.base.BasePhoneModel
import dimitar.udemy.phonebook.models.data.ExternalContactModel
import dimitar.udemy.phonebook.presenters.AddContactPresenter
import kotlinx.coroutines.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.collections.ArrayList

class AddContactActivity : AppCompatActivity(), AddContactPresenter.View {

    private var binding     : ActivityAddEditContactBinding?    = null
    private var presenter   : AddContactPresenter               = AddContactPresenter(this)
    private var itemAdapter : RecyclerViewAdapterAdd?           = null
    private val scope                                           = CoroutineScope(Dispatchers.IO)

    private var             imageCapture        : ImageCapture? = null
    private lateinit var    outputDirectory     : File
    private lateinit var    cameraExecutor      : ExecutorService

    private val selectImageFromGalleryResult = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let {
            binding?.civProfilePic?.setImageURI(uri)
            presenter.onSuccessfulLoadOfPicture(uri.toString())
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddEditContactBinding.inflate(layoutInflater)
        setContentView(binding?.root)

        setUpRV()


        outputDirectory     = getOutputDirectory()
        cameraExecutor      = Executors.newSingleThreadExecutor()

        binding?.civProfilePic?.setOnClickListener {
            presenter.choosePicture()
        }

        binding?.ibDoneNumberInput?.setOnClickListener {
            itemAdapter?.addAPhoneNumber(BasePhoneModel(binding?.etNumber?.text?.toString()!!, null))
            binding?.etNumber?.setText("")
        }

        binding?.btnSaveChanges?.setOnClickListener {
            onSavePressed()
        }
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
        return  if (mediaDir != null && mediaDir.exists()) mediaDir
                else filesDir
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
        binding = null
        presenter.detachView()
        cameraExecutor.shutdown()
        scope.cancel()
    }

    override fun onInvalidField(kind: InvalidType) {
        Toast.makeText(
            this,
            kind.errorMessage,
            Toast.LENGTH_LONG
        ).show()
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
                0 -> chooseImageFromGallery()
                1 -> goToCamera()
            }
        }
        picDialog.show()
    }

    private fun onSavePressed() {
        val contact = ExternalContactModel(
            BaseContactModel(
                binding!!.etFName.text.toString(),
                binding!!.etLName.text.toString(),
                presenter.getPicture(),
                null
            ),
            itemAdapter!!.getPhoneNumbers()
        )
        scope.launch {
            kotlin.runCatching {
                presenter.saveContact(contact)
            }.onFailure {
                showError(it.message!!)
            }
        }

    }

    private fun showError(error: String) {
        Toast.makeText(
            this,
            error,
            Toast.LENGTH_LONG
        ).show()
    }

    private fun chooseImageFromGallery() {
        selectImageFromGalleryResult.launch(null)
    }

    private fun goToCamera() {
        binding?.viewFinder?.visibility = View.VISIBLE
        binding?.cardView?.visibility = View.GONE
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
            SimpleDateFormat(FILENAME_FORMAT, Locale.US
            ).format(System.currentTimeMillis()) + ".jpg")

        // Create output options object which contains file + metadata
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        // Set up image capture listener, which is triggered after photo has
        // been taken
        imageCapture.takePicture(
            outputOptions, ContextCompat.getMainExecutor(this), object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)

                    binding?.viewFinder?.visibility     = View.GONE
                    binding?.cardView?.visibility       = View.VISIBLE
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val savedUri    = Uri.fromFile(photoFile)
                    val msg         = "Photo capture succeeded: $savedUri"

                    Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()

                    Log.d(TAG, msg)

                    presenter.onSuccessfulLoadOfPicture(savedUri.toString())
                    binding?.civProfilePic?.setImageURI(savedUri)

                    binding?.viewFinder?.visibility     = View.GONE
                    binding?.cardView?.visibility       = View.VISIBLE
                }
            })
    }

    override fun onSuccessfulSafeOfContact() {
        runOnUiThread {
            Toast.makeText(
                applicationContext,
                "Contact has been saved successfully",
                Toast.LENGTH_LONG
            ).show()
            onBackPressed()
        }
    }

    override fun onFailedSafeOfContact() {
        runOnUiThread {
            Toast.makeText(
                this,
                "Contact has not been saved successfully",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun setUpRV() {
        binding?.rvPhoneNumberList?.layoutManager = LinearLayoutManager(
            this,
            LinearLayoutManager.VERTICAL,
            false
        )

        itemAdapter = RecyclerViewAdapterAdd(ArrayList())
        binding?.rvPhoneNumberList?.adapter = itemAdapter

    }

    companion object {
        private const val TAG               = "PhonebookApp"
        private const val FILENAME_FORMAT   = "yyyy-MM-dd-HH-mm-ss-SSS"
    }

}