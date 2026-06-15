package com.itda.language.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.itda.language.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val isLoading: Boolean = false,
    val isAuthenticated: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "이메일과 비밀번호를 입력해주세요")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            authRepository.login(email, password)
                .onSuccess { _uiState.value = AuthUiState(isAuthenticated = true) }
                .onFailure { _uiState.value = AuthUiState(error = it.message) }
        }
    }

    fun register(
        email: String, nickname: String, password: String,
        phone: String = "", gender: String = "", birthId: String = "",
    ) {
        if (email.isBlank() || nickname.isBlank() || password.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "모든 항목을 입력해주세요")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            authRepository.register(email, nickname, password, phone, gender, birthId)
                .onSuccess { _uiState.value = AuthUiState(isAuthenticated = true) }
                .onFailure { _uiState.value = AuthUiState(error = it.message) }
        }
    }
}
