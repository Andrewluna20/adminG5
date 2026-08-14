package com.theextramile.admin.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.theextramile.admin.BuildConfig
import com.theextramile.admin.data.api.ApiClient
import com.theextramile.admin.data.api.EmailRequest
import com.theextramile.admin.data.repository.ApiResult
import com.theextramile.admin.data.repository.apiCall
import com.theextramile.admin.data.model.User
import com.theextramile.admin.data.repository.AuthRepository
import com.theextramile.admin.util.passwordPolicyError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val passwordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val loginSuccess: Boolean = false,
    val loggedUser: User? = null,
    /** Aviso de "te mandamos una contraseña temporal", en verde */
    val infoMessage: String? = null,
    val isRecovering: Boolean = false,
    /**
     * Entró con la contraseña temporal: la sesión ya está abierta pero no
     * se le deja pasar hasta que cree la definitiva.
     */
    val mustChangePassword: Boolean = false,
    val isChangingPassword: Boolean = false,
    val changeError: String? = null
)

class LoginViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    init {
        // Si la app se cerró con el cambio a medias, se retoma aquí
        viewModelScope.launch {
            authRepository.pendingPasswordChange()?.let { u ->
                _uiState.update { it.copy(mustChangePassword = true, loggedUser = u) }
            }
        }
    }

    fun onEmailChange(email: String) {
        _uiState.update { it.copy(email = email, errorMessage = null) }
    }

    fun onPasswordChange(password: String) {
        _uiState.update { it.copy(password = password, errorMessage = null) }
    }

    fun togglePasswordVisibility() {
        _uiState.update { it.copy(passwordVisible = !it.passwordVisible) }
    }

    /**
     * "¿Olvidaste tu contraseña?" — mismo camino que recoverPassword()
     * del panel: se lo pide al HUB, que manda una contraseña TEMPORAL al
     * correo. Con esa se entra normal y después toca cambiarla.
     *
     * Va al HUB y no a la API del sitio, por eso usa la URL completa.
     */
    fun recoverPassword() {
        val email = _uiState.value.email.trim().lowercase()
        if (email.isBlank()) {
            _uiState.update {
                it.copy(errorMessage = "Escribe tu correo arriba para poder recuperarla.")
            }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isRecovering = true, errorMessage = null, infoMessage = null) }
            val r = apiCall {
                ApiClient.service.hubRecoverPassword(
                    url = BuildConfig.HUB_URL + "?action=recover_password",
                    request = EmailRequest(email)
                )
            }
            _uiState.update {
                when {
                    r is ApiResult.Success && r.data.success -> it.copy(
                        isRecovering = false,
                        infoMessage = "Te enviamos una contraseña temporal a $email. Entra con ella y luego cámbiala."
                    )
                    r is ApiResult.Success -> it.copy(
                        isRecovering = false,
                        errorMessage = r.data.error ?: "No se pudo recuperar la contraseña."
                    )
                    else -> it.copy(
                        isRecovering = false,
                        errorMessage = r.errorMessage ?: "No se pudo recuperar la contraseña."
                    )
                }
            }
        }
    }

    fun login() {
        val state = _uiState.value
        if (state.email.isBlank() || state.password.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Completa ambos campos") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = authRepository.login(state.email, state.password)
            _uiState.update {
                when (result) {
                    /*
                     * Con contraseña temporal NO se marca loginSuccess: el
                     * usuario se queda en el login con la hoja de cambio
                     * delante. La sesión ya está abierta, que es lo que
                     * change_own_password necesita, pero no llega al panel
                     * hasta que la cambia. Igual que adminLogin() del panel.
                     */
                    is AuthRepository.LoginResult.Success -> it.copy(
                        isLoading = false,
                        loginSuccess = !result.mustChangePassword,
                        mustChangePassword = result.mustChangePassword,
                        loggedUser = result.user
                    )
                    AuthRepository.LoginResult.InvalidCredentials -> it.copy(
                        isLoading = false,
                        errorMessage = "Correo o contraseña incorrectos"
                    )
                    AuthRepository.LoginResult.NoConnection -> it.copy(
                        isLoading = false,
                        errorMessage = "Sin conexión a internet. Verifica tu red."
                    )
                    is AuthRepository.LoginResult.Error -> it.copy(
                        isLoading = false,
                        errorMessage = result.message
                    )
                }
            }
        }
    }

    /**
     * Guarda la contraseña definitiva del que entró con una temporal.
     *
     * Se valida con la MISMA política que el panel antes de mandarla, para
     * que el usuario sepa exactamente qué le falta en vez de recibir un
     * "no se pudo" del servidor.
     */
    fun submitForcedPassword(newPassword: String, confirm: String) {
        val polErr = passwordPolicyError(newPassword)
        if (polErr != null) {
            _uiState.update { it.copy(changeError = polErr) }
            return
        }
        if (newPassword != confirm) {
            _uiState.update { it.copy(changeError = "Las contraseñas no coinciden.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isChangingPassword = true, changeError = null) }
            val r = authRepository.changeOwnPassword(newPassword)
            _uiState.update {
                if (r is ApiResult.Success) {
                    it.copy(
                        isChangingPassword = false,
                        mustChangePassword = false,
                        loginSuccess = true
                    )
                } else {
                    it.copy(
                        isChangingPassword = false,
                        changeError = r.errorMessage ?: "No se pudo cambiar la contraseña."
                    )
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
