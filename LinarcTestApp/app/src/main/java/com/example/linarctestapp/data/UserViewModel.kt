package com.example.linarctestapp.data

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.example.linarctestapp.api.AppDatabase
import com.example.linarctestapp.api.UserRepository
import kotlinx.coroutines.launch

class UserViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = Room.databaseBuilder(application, AppDatabase::class.java, "db").build().userDao()
    private val repo = UserRepository(dao)

    private val _insertComplete = MutableLiveData<Boolean>()
    val insertComplete: LiveData<Boolean> get() = _insertComplete

    fun insert(user: User) = viewModelScope.launch {
        repo.insert(user)
        _insertComplete.postValue(true)}
    fun getAll(callback: (List<User>) -> Unit) = viewModelScope.launch {
        callback(repo.getAll())
    }
    fun delete(user: User) = viewModelScope.launch { repo.delete(user) }

    fun resetInsertFlag() {
        _insertComplete.value = false
    }
}

class UserViewModelFactory(private val app: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(UserViewModel::class.java)) {
            return UserViewModel(app) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}