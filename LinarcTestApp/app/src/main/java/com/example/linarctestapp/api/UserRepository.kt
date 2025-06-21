package com.example.linarctestapp.api

import com.example.linarctestapp.data.User

class UserRepository(private val dao: UserDao) {
    suspend fun insert(user: User) = dao.insert(user)
    suspend fun getAll() = dao.getAll()
    suspend fun delete(user: User) = dao.delete(user)
}