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

        flutterEngine = FlutterEngine(this)

        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, Constants.CHANNEL_NAME)
            .setMethodCallHandler { call, result ->
                when (call.method) {
                    getString(R.string.get_users) -> {
                    }
                }
            }

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
