package com.elocho.snooker.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.elocho.snooker.data.repository.SettingsRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class AuthUiState(
    val isLoggedIn: Boolean = false,
    val isLoading: Boolean = true,
    val loginError: String? = null,
    val username: String = "",
    val password: String = ""
)

class AuthViewModel(private val settingsRepository: SettingsRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.isLoggedIn.collect { loggedIn ->
                _uiState.update { it.copy(isLoggedIn = loggedIn, isLoading = false) }
            }
        }
    }

    fun updateUsername(username: String) {
        _uiState.update { it.copy(username = username, loginError = null) }
    }

    fun updatePassword(password: String) {
        _uiState.update { it.copy(password = password, loginError = null) }
    }

    fun login() {
        val state = _uiState.value
        if (state.username.trim() == "admin" && state.password == "12345678") {
            viewModelScope.launch {
                settingsRepository.setLoggedIn(true)
                _uiState.update { it.copy(isLoggedIn = true, loginError = null) }
            }
        } else {
            _uiState.update { it.copy(loginError = "Invalid username or password. Please try again.") }
        }
    }

    fun logout() {
        viewModelScope.launch {
            settingsRepository.setLoggedIn(false)
            _uiState.update {
                AuthUiState(isLoggedIn = false, isLoading = false)
            }
        }
    }

    class Factory(private val settingsRepository: SettingsRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AuthViewModel(settingsRepository) as T
        }
    }
}
