package com.example.linarctestapp

import android.app.Application
import android.util.Log
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import com.example.linarctestapp.utils.Constants
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.embedding.engine.FlutterEngineCache
import io.flutter.embedding.engine.dart.DartExecutor
import io.flutter.plugin.common.MethodChannel

class MyApp : Application(), ViewModelStoreOwner {

    lateinit var flutterEngine: FlutterEngine

    override fun onCreate() {
        super.onCreate()

        // Init Flutter engine manually
        flutterEngine = FlutterEngine(this)

        // Register method channel
        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, "user_channel")
            .setMethodCallHandler { call, result ->
                Log.d("vis@#$", "From MyApp check the call back triggers")
                when (call.method) {
                    "getUsers" -> {
                        Log.d("vis@#$", "the user list ")
                    }
                }
            }

        // Pre-warm Flutter engine
        flutterEngine.navigationChannel.setInitialRoute("/")
        flutterEngine.dartExecutor.executeDartEntrypoint(
            DartExecutor.DartEntrypoint.createDefault()
        )
        FlutterEngineCache.getInstance().put(Constants.FLUTTER_ENGINE_ID, flutterEngine)
    }

    private val appViewModelStore: ViewModelStore by lazy {
        ViewModelStore()
    }

    override val viewModelStore: ViewModelStore
        get() = appViewModelStore
}
