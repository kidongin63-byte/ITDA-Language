package com.itda.language.ui.voice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.itda.language.data.model.VoicePersona
import com.itda.language.data.repository.VoiceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class VoiceSelectUiState(
    val isLoading: Boolean = false,
    val personas: List<VoicePersona> = emptyList(),
    // 카테고리 옵션 (서버에서 로드)
    val genders: Map<String, String> = emptyMap(),
    val ageGroups: Map<String, String> = emptyMap(),
    val regions: Map<String, String> = emptyMap(),
    val tones: Map<String, String> = emptyMap(),
    // 선택된 필터
    val selectedGender: String? = null,
    val selectedAgeGroup: String? = null,
    val selectedRegion: String? = null,
    val selectedTone: String? = null,
)

@HiltViewModel
class VoiceSelectViewModel @Inject constructor(
    private val voiceRepository: VoiceRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(VoiceSelectUiState())
    val uiState: StateFlow<VoiceSelectUiState> = _uiState.asStateFlow()

    fun loadCategories() {
        viewModelScope.launch {
            voiceRepository.getCategories().onSuccess { cats ->
                _uiState.value = _uiState.value.copy(
                    genders = cats.genders,
                    ageGroups = cats.age_groups,
                    regions = cats.regions,
                    tones = cats.tones,
                )
            }
        }
    }

    fun loadPersonas() {
        val state = _uiState.value
        viewModelScope.launch {
            _uiState.value = state.copy(isLoading = true)
            voiceRepository.getPersonas(
                gender = state.selectedGender,
                ageGroup = state.selectedAgeGroup,
                region = state.selectedRegion,
                tone = state.selectedTone,
            ).onSuccess {
                _uiState.value = _uiState.value.copy(personas = it, isLoading = false)
            }.onFailure {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun selectGender(value: String?) {
        _uiState.value = _uiState.value.copy(selectedGender = value)
        loadPersonas()
    }

    fun selectAgeGroup(value: String?) {
        _uiState.value = _uiState.value.copy(selectedAgeGroup = value)
        loadPersonas()
    }

    fun selectRegion(value: String?) {
        _uiState.value = _uiState.value.copy(selectedRegion = value)
        loadPersonas()
    }

    fun selectTone(value: String?) {
        _uiState.value = _uiState.value.copy(selectedTone = value)
        loadPersonas()
    }
}
