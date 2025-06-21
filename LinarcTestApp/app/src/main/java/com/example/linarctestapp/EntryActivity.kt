package com.example.linarctestapp

import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.annotation.NonNull
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import com.example.linarctestapp.data.UserViewModel
import com.example.linarctestapp.data.UserViewModelFactory
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel

class EntryActivity : FlutterActivity() {

    private lateinit var viewModel: UserViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_entry)
        val app = applicationContext as MyApp

        viewModel = ViewModelProvider(app, UserViewModelFactory(app))[UserViewModel::class.java]

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val clickMeBtn = findViewById<Button>(R.id.clickMeBtn)
        clickMeBtn.setOnClickListener {
            startActivity(
                FlutterActivity
                    .withNewEngine()
                    .initialRoute("/my_route")
                    .build(this)
            )
        }
    }


    override fun configureFlutterEngine(@NonNull flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)


        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, "user_channel").setMethodCallHandler {
                call, result ->
            when (call.method) {
                "getUsers" -> {
                    viewModel.getAll { users ->
                        val list = users.map {
                            mapOf(
                                "id" to it.id,
                                "name" to it.name,
                                "address" to it.address,
                                "phone" to it.phone,
                                "profilePicUri" to it.profilePic
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