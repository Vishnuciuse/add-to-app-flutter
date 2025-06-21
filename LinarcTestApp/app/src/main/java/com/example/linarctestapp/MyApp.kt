package com.example.linarctestapp

import android.app.Application
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner

class MyApp : Application(), ViewModelStoreOwner {

    // Don't pass ViewModelStore in the constructor

    private val appViewModelStore: ViewModelStore by lazy {
        ViewModelStore()
    }

    override val viewModelStore: ViewModelStore
        get() = appViewModelStore
}
