package com.theextramile.admin.data.repository

import com.theextramile.admin.data.api.ApiClient
import com.theextramile.admin.data.api.IdRequest
import com.theextramile.admin.data.api.SaveUserRequest
import com.theextramile.admin.data.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.IOException

class UserRepository {

    private val _users = MutableStateFlow<List<User>>(emptyList())
    val users: StateFlow<List<User>> = _users.asStateFlow()

    sealed class Result<out T> {
        data class Success<T>(val data: T) : Result<T>()
        object NoConnection : Result<Nothing>()
        object Forbidden : Result<Nothing>()
        data class Error(val message: String) : Result<Nothing>()
    }

    suspend fun refresh(): Result<List<User>> {
        return try {
            val response = ApiClient.service.listUsers()
            when {
                response.isSuccessful -> {
                    val list = response.body() ?: emptyList()
                    _users.value = list
                    Result.Success(list)
                }
                response.code() == 403 -> Result.Forbidden
                else -> Result.Error("Error ${response.code()}")
            }
        } catch (e: IOException) { Result.NoConnection }
        catch (e: Exception) { Result.Error(e.message ?: "Error") }
    }

    suspend fun saveUser(
        id: String?, name: String, email: String, password: String?, role: String
    ): Result<Unit> {
        return try {
            val response = ApiClient.service.saveUser(
                user = SaveUserRequest(
                    id = id,
                    name = name,
                    email = email,
                    password = password?.takeIf { it.isNotBlank() },
                    role = role
                )
            )
            if (response.isSuccessful) {
                refresh()  // recargar lista
                Result.Success(Unit)
            } else Result.Error("Error ${response.code()}")
        } catch (e: IOException) { Result.NoConnection }
        catch (e: Exception) { Result.Error(e.message ?: "Error") }
    }

    suspend fun deleteUser(id: String): Result<Unit> {
        return try {
            val response = ApiClient.service.deleteUser(
                request = IdRequest(id)
            )
            if (response.isSuccessful) {
                refresh()
                Result.Success(Unit)
            } else Result.Error("Error ${response.code()}")
        } catch (e: IOException) { Result.NoConnection }
        catch (e: Exception) { Result.Error(e.message ?: "Error") }
    }
}
