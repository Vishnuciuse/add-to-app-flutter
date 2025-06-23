package com.example.linarctestapp

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.annotation.NonNull
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import com.example.linarctestapp.data.UserViewModel
import com.example.linarctestapp.data.UserViewModelFactory
import com.example.linarctestapp.databinding.ActivityEntryBinding
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel
import com.example.linarctestapp.data.User
import com.example.linarctestapp.utils.Constants
import io.flutter.embedding.engine.FlutterEngineCache

class EntryActivity : FlutterActivity() {

    private lateinit var binding: ActivityEntryBinding
    private lateinit var viewModel: UserViewModel

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
    }


    private fun onClickListeners() {

        binding.displayBtn.setOnClickListener {
            viewModel.getAll() { users ->
                binding.dataListTV.text = users.toString()
                println("database@#$ the user list is $users")
                Toast.makeText(this, users.toString(), Toast.LENGTH_LONG).show()
            }
        }

        binding.clickMeBtn.setOnClickListener {

            val userData = User(
                0, binding.nameET.text.toString(), binding.addressET.text.toString(),
                binding.mobileET.text.toString(), "", "")

            if ( binding.nameET.text.toString().isNotEmpty())
            viewModel.insert(userData)
            else
                Toast.makeText(this, "Enter the values", Toast.LENGTH_LONG).show()

        }

        binding.navMeBtn.setOnClickListener {
            startActivity(
                FlutterActivity
                    .withCachedEngine(Constants.FLUTTER_ENGINE_ID)
                    .build(this)
            )
//            viewModel.getAll { users ->
//                binding.dataListTV.text = users.toString()
//                Toast.makeText(this, users.toString(), Toast.LENGTH_LONG).show()
//               users.forEach {
//                   if (it.id ==3){
//                       viewModel.delete(it)
//                   }
//               }
//
//            }

        }
    }

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)

        val app = applicationContext as MyApp
        val viewModel = ViewModelProvider(app, UserViewModelFactory(app))[UserViewModel::class.java]

        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, "user_channel").setMethodCallHandler {
                call, result ->
            when (call.method) {
                "getUsers" -> {
                    Log.d("vis@#$","check the call back triggers")
                    viewModel.getAll { users ->
                        Log.d("channel_debug@#$", "Fetched users: $users")
                        val list = users.map {
                            mapOf(
                                "id" to it.id,
                                "name" to it.name,
                                "address" to it.address,
                                "phone" to it.phone,
                                "signature" to it.signature,
                                "profilePic" to it.profilePic
                            )
                        }
                        result.success(list)
                    }
                }
                "deleteUser" -> {
                    val id = call.argument<Int>("id")!!
                    viewModel.getAll { users ->
                        val user = users.firstOrNull { it.id == id }
                        user?.let {
                            viewModel.delete(it)
                            result.success(true)
                        } ?: result.error("404", "User not found", null)
                    }
                }
                else -> result.notImplemented()
            }
        }
    }

}