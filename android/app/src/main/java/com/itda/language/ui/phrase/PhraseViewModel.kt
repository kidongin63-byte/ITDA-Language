package com.itda.language.ui.phrase

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.itda.language.data.model.PhraseResponse
import com.itda.language.data.repository.PhraseRepository
import com.itda.language.data.repository.TtsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// 카테고리 한글 매핑
val CATEGORY_LABELS = linkedMapOf(
    null to "전체",
    "greeting" to "인사",
    "request" to "요청",
    "thanks" to "감사",
    "daily" to "일상",
    "emergency" to "긴급",
    "medical" to "의료",
    "order" to "주문",
    "custom" to "내 문구",
)

data class PhraseUiState(
    val phrases: List<PhraseResponse> = emptyList(),
    val selectedCategory: String? = null,
    val isLoading: Boolean = false,
    val isPlaying: Boolean = false,
    val playingPhraseId: String? = null,
    val showAddDialog: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class PhraseViewModel @Inject constructor(
    private val phraseRepository: PhraseRepository,
    private val ttsRepository: TtsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PhraseUiState())
    val uiState: StateFlow<PhraseUiState> = _uiState.asStateFlow()

    var currentSpeaker: String = "nara"

    fun loadPhrases() {
        val category = _uiState.value.selectedCategory
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            phraseRepository.listPhrases(category)
                .onSuccess { _uiState.value = _uiState.value.copy(phrases = it, isLoading = false) }
                .onFailure { _uiState.value = _uiState.value.copy(isLoading = false, error = it.message) }
        }
    }

    fun selectCategory(category: String?) {
        _uiState.value = _uiState.value.copy(selectedCategory = category)
        loadPhrases()
    }

    fun addAllPresets() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            phraseRepository.addAllPresets()
                .onSuccess { loadPhrases() }
                .onFailure { _uiState.value = _uiState.value.copy(isLoading = false, error = it.message) }
        }
    }

    fun addPhrase(text: String, category: String = "custom") {
        if (text.isBlank()) return
        viewModelScope.launch {
            phraseRepository.createPhrase(text, category)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(showAddDialog = false)
                    loadPhrases()
                }
        }
    }

    fun deletePhrase(phraseId: String) {
        viewModelScope.launch {
            phraseRepository.deletePhrase(phraseId)
                .onSuccess { loadPhrases() }
        }
    }

    fun playPhrase(phrase: PhraseResponse) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isPlaying = true, playingPhraseId = phrase.id)
            phraseRepository.incrementUsage(phrase.id)
            ttsRepository.synthesizeAndPlay(
                text = phrase.phrase_text,
                speaker = currentSpeaker,
                onComplete = {
                    _uiState.value = _uiState.value.copy(isPlaying = false, playingPhraseId = null)
                },
            ).onFailure {
                _uiState.value = _uiState.value.copy(isPlaying = false, playingPhraseId = null, error = it.message)
            }
        }
    }

    fun showAddDialog() {
        _uiState.value = _uiState.value.copy(showAddDialog = true)
    }

    fun hideAddDialog() {
        _uiState.value = _uiState.value.copy(showAddDialog = false)
    }
}
