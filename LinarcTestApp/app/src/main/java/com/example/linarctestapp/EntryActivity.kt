package com.example.linarctestapp

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import com.example.linarctestapp.data.User
import com.example.linarctestapp.data.UserViewModel
import com.example.linarctestapp.data.UserViewModelFactory
import com.example.linarctestapp.databinding.ActivityEntryBinding
import com.example.linarctestapp.utils.Constants
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.embedding.engine.FlutterEngineCache
import io.flutter.plugin.common.MethodChannel
import java.io.File
import java.io.FileOutputStream

class EntryActivity : FlutterActivity() {

    private lateinit var binding: ActivityEntryBinding
    private lateinit var viewModel: UserViewModel
    var imageUri: String? = null

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
                imageUri = null
                binding.nameET.text?.clear()
                binding.addressET.text?.clear()
                binding.mobileET.text?.clear()
                viewModel.resetInsertFlag()
                clearFocusAndHideKeyboard()
                Toast.makeText(this, getString(R.string.saved_successfully), Toast.LENGTH_LONG)
                    .show()
            }
        }

    }

    private fun onClickListeners() {
        binding.uploadSIV.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = getString(R.string.image_mime_type)
            startActivityForResult(intent, 101)
        }

        binding.clickMeBtn.setOnClickListener {
            val userData = User(
                0, binding.nameET.text.toString(), binding.addressET.text.toString(),
                binding.mobileET.text.toString(), "", imageUri.toString()
            )
            if (binding.nameET.text.toString().isNotEmpty()) // here only checking the name is mandatory, can add more validations
                viewModel.insert(userData)
            else
                Toast.makeText(this, getString(R.string.enter_values), Toast.LENGTH_LONG).show()
        }

        binding.navMeBtn.setOnClickListener {
            startActivity(withCachedEngine(Constants.FLUTTER_ENGINE_ID).build(this))
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 101 && resultCode == RESULT_OK && data?.data != null) {
            imageUri = copyUriToTempFile(this, data.data!!)
            val tempFilePath = Uri.fromFile(File(imageUri!!))
            binding.uploadSIV.setImageURI(tempFilePath)
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

    fun copyUriToTempFile(context: Context, uri: Uri): String {
        val inputStream = context.contentResolver.openInputStream(uri)
        val file = File(context.cacheDir, getString(R.string.shared_image_)+System.currentTimeMillis().toString()+".jpg")
        val outputStream = FileOutputStream(file)
        inputStream?.copyTo(outputStream)
        outputStream.close()
        inputStream?.close()
        return file.absolutePath
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