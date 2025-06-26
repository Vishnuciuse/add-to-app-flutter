package com.example.linarctestapp

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import com.example.linarctestapp.data.User
import com.example.linarctestapp.data.UserViewModel
import com.example.linarctestapp.data.UserViewModelFactory
import com.example.linarctestapp.databinding.ActivityEntryBinding
import com.example.linarctestapp.ui.custom.SignatureView
import com.example.linarctestapp.utils.Constants
import com.google.android.material.bottomsheet.BottomSheetDialog
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.embedding.engine.FlutterEngineCache
import io.flutter.plugin.common.MethodChannel
import java.io.File
import java.io.FileOutputStream

class EntryActivity : FlutterActivity() {

    private val GALLERY_REQUEST_CODE = 101
    private val CAMERA_REQUEST_CODE = 102
    private val CAMERA_PERMISSION_CODE = 103
    private lateinit var binding: ActivityEntryBinding
    private lateinit var viewModel: UserViewModel
    private lateinit var cameraImageFile: File
    var imageUri: String? = null
    var signUri: String? = null

    override fun provideFlutterEngine(context: Context): FlutterEngine {
        return FlutterEngineCache.getInstance().get(Constants.FLUTTER_ENGINE_ID)!!
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEntryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val app = applicationContext as MyApp
        viewModel = ViewModelProvider(app, UserViewModelFactory(app))[UserViewModel::class.java]

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        onClickListeners()
        onObservers()
    }

    private fun onObservers() {
        viewModel.insertComplete.observe(this) { isDone ->
            if (isDone) {
                binding.uploadSIV.setImageDrawable(null)
                binding.viewSignIV.setImageDrawable(null)
                imageUri = null
                signUri = null
                binding.nameET.text?.clear()
                binding.addressET.text?.clear()
                binding.mobileET.text?.clear()
                viewModel.resetInsertFlag()
                clearFocusAndHideKeyboard()
                toastMessage(getString(R.string.saved_successfully))
            }
        }

    }

    private fun onClickListeners() {
        binding.uploadSIV.setOnClickListener {
            showBottomSheetDialog()
        }

        binding.signBtn.setOnClickListener {
           signatureDialog()
        }

        binding.clickMeBtn.setOnClickListener {
            val userData = User(
                0, binding.nameET.text.toString(), binding.addressET.text.toString(),
                binding.mobileET.text.toString(), signUri.toString(), imageUri.toString()
            )
            if (binding.nameET.text.toString().isNotEmpty()) // here only checking the name is mandatory, can add more validations
                viewModel.insert(userData)
            else
                toastMessage(getString(R.string.enter_values))
        }

        binding.navMeBtn.setOnClickListener {
            startActivity(withCachedEngine(Constants.FLUTTER_ENGINE_ID).build(this))
        }
    }

    private fun signatureDialog() {
        val dialogView = layoutInflater.inflate(R.layout.signature_dialog, null)

        val signatureView = dialogView.findViewById<SignatureView>(R.id.dialog_signature_view)
        val clearButton = dialogView.findViewById<Button>(R.id.btn_clear)
        val saveButton = dialogView.findViewById<Button>(R.id.btn_save)
        val closeImage = dialogView.findViewById<ImageView>(R.id.closeIV)

        val dialog = AlertDialog.Builder(this,  androidx.appcompat.R.style.Theme_AppCompat_Light_Dialog_Alert)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        clearButton.setOnClickListener {
            signatureView.clearSignature()
        }

        saveButton.setOnClickListener {
            val signatureBitmap = signatureView.getSignatureBitmap()
            binding.viewSignIV.setImageBitmap(signatureBitmap)
            val file = saveSignatureToCache(signatureBitmap)
            if (file != null) {
                signUri = file.absolutePath
                toastMessage(getString(R.string.signature_saved))
            } else {
                toastMessage(getString(R.string.failed_to_save_signature))
            }
            dialog.dismiss()
        }

        closeImage.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == GALLERY_REQUEST_CODE && resultCode == RESULT_OK && data?.data != null) {
            imageUri = copyUriToTempFile(this, data.data!!)
            val tempFilePath = Uri.fromFile(File(imageUri!!))
            binding.uploadSIV.setImageURI(tempFilePath)
        }
        if (requestCode == CAMERA_REQUEST_CODE && resultCode == RESULT_OK) {
            val tempFilePath = Uri.fromFile(cameraImageFile)
            binding.uploadSIV.setImageURI(tempFilePath)
            imageUri = cameraImageFile.absolutePath
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera()
            } else {
                toastMessage(getString(R.string.camera_permission_denied))
                }
        }
    }

    private fun checkPermissionAndOpenCamera() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                CAMERA_PERMISSION_CODE
            )
        } else {
            openCamera()
        }
    }

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)

        val app = applicationContext as MyApp
        val viewModel = ViewModelProvider(app, UserViewModelFactory(app))[UserViewModel::class.java]

        MethodChannel(
            flutterEngine.dartExecutor.binaryMessenger,
            Constants.CHANNEL_NAME
        ).setMethodCallHandler { call, result ->
            when (call.method) {

                getString(R.string.get_users) -> {
                    viewModel.getAll { users ->
                        val list = users.map { it.toMap() }
                        result.success(list)
                    }
                }

                getString(R.string.delete_user) -> {
                    val id = call.argument<Int>("id")!!
                    viewModel.getAll { users ->
                        val user = users.firstOrNull { it.id == id }
                        user?.let {
                            viewModel.delete(it)
                            result.success(true)
                        } ?: result.error("404", getString(R.string.user_not_found), null)
                    }
                }

                else -> result.notImplemented()
            }
        }
    }

    private fun openCamera() {
        cameraImageFile = File.createTempFile(getString(R.string.user_photo), ".jpg", cacheDir)
        val cameraImageUri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", cameraImageFile)

        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
            putExtra(MediaStore.EXTRA_OUTPUT, cameraImageUri)
            putExtra("android.intent.extras.CAMERA_FACING", 1)
            putExtra("android.intent.extras.LENS_FACING_FRONT", 1)
            putExtra("android.intent.extra.USE_FRONT_CAMERA", true)
        }
        startActivityForResult(intent, CAMERA_REQUEST_CODE)
    }

    private fun showBottomSheetDialog() {
        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.bottom_sheet_layout, null)
        dialog.setContentView(view)

        val cameraTV = view.findViewById<TextView>(R.id.cameraTV)
        val galleryTV = view.findViewById<TextView>(R.id.galleryTV)
        val closeIV = view.findViewById<ImageView>(R.id.bottom_sheet_closeIV)
        cameraTV.setOnClickListener {
            checkPermissionAndOpenCamera()
            dialog.dismiss()
        }
        galleryTV.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = getString(R.string.image_mime_type)
            startActivityForResult(intent, GALLERY_REQUEST_CODE)
            dialog.dismiss()
        }
        closeIV.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    fun copyUriToTempFile(context: Context, uri: Uri): String {
        val inputStream = context.contentResolver.openInputStream(uri)
        val file = File(context.cacheDir, getString(R.string.shared_image_)+System.currentTimeMillis().toString()+".jpg")
        val outputStream = FileOutputStream(file)
        inputStream?.copyTo(outputStream)
        outputStream.close()
        inputStream?.close()
        return file.absolutePath
    }

    private fun saveSignatureToCache(signatureBitmap: Bitmap): File? {
        return try {
            val fileName = getString(R.string.user_signature_)+System.currentTimeMillis()+".png"
            val cacheDir = File(cacheDir, getString(R.string.signature))
            if (!cacheDir.exists()) {
                cacheDir.mkdirs()
            }
            val file = File(cacheDir, fileName)
            val outputStream = FileOutputStream(file)

            signatureBitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            outputStream.flush()
            outputStream.close()

            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun toastMessage(msg:String){
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
    }

    fun User.toMap(): Map<String, Any?> = mapOf(
        getString(R.string._id) to id,
        getString(R.string.small_name) to name,
        getString(R.string.small_address) to address,
        getString(R.string.phone) to phone,
        getString(R.string.signature) to signature,
        getString(R.string.profilePic) to profilePic
    )

    fun clearFocusAndHideKeyboard() {
        val view = this.currentFocus
        view?.clearFocus()
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view?.windowToken, 0)
    }

}