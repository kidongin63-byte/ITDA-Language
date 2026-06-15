package com.itda.language.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.itda.language.data.api.AdminApi
import com.itda.language.data.api.AdminActionRequest
import com.itda.language.data.api.PinVerifyRequest
import com.itda.language.data.api.VoiceChangeRequestDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AdminUiState(
    val isAdmin: Boolean = false,
    val isLoading: Boolean = false,
    val requests: List<VoiceChangeRequestDto> = emptyList(),
    val pinError: String? = null,
    val message: String? = null,
)

@HiltViewModel
class AdminViewModel @Inject constructor(
    private val adminApi: AdminApi,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminUiState())
    val uiState: StateFlow<AdminUiState> = _uiState.asStateFlow()

    fun verifyPin(pin: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, pinError = null)
            try {
                val resp = adminApi.verifyPin(PinVerifyRequest(pin))
                if (resp.isSuccessful && resp.body()?.valid == true) {
                    _uiState.value = _uiState.value.copy(isAdmin = true, isLoading = false)
                    loadRequests()
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        pinError = "PIN이 올바르지 않습니다",
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    pinError = "서버 연결에 실패했습니다",
                )
            }
        }
    }

    fun loadRequests() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val resp = adminApi.getChangeRequests()
                if (resp.isSuccessful) {
                    _uiState.value = _uiState.value.copy(
                        requests = resp.body() ?: emptyList(),
                        isLoading = false,
                    )
                }
            } catch (_: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun approveRequest(requestId: String) {
        viewModelScope.launch {
            try {
                adminApi.approveRequest(requestId)
                _uiState.value = _uiState.value.copy(message = "승인 완료")
                loadRequests()
            } catch (_: Exception) { }
        }
    }

    fun rejectRequest(requestId: String) {
        viewModelScope.launch {
            try {
                adminApi.rejectRequest(requestId)
                _uiState.value = _uiState.value.copy(message = "거부 완료")
                loadRequests()
            } catch (_: Exception) { }
        }
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }
}
