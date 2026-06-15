package com.itda.language.ui.phrase

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.itda.language.R
import com.itda.language.data.model.PhraseResponse

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhraseScreen(
    currentSpeaker: String,
    onBack: () -> Unit,
    viewModel: PhraseViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(currentSpeaker) {
        viewModel.currentSpeaker = currentSpeaker
        viewModel.loadPhrases()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.phrase_title), fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.voice_back))
                    }
                },
                actions = {
                    // 프리셋 일괄 추가
                    IconButton(onClick = { viewModel.addAllPresets() }) {
                        Icon(Icons.Default.PlaylistAdd, stringResource(R.string.phrase_add_preset))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                    actionIconContentColor = MaterialTheme.colorScheme.onBackground,
                ),
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.showAddDialog() }) {
                Icon(Icons.Default.Add, stringResource(R.string.phrase_add))
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            // 카테고리 필터 칩
            LazyRow(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(CATEGORY_LABELS.entries.toList()) { (key, label) ->
                    FilterChip(
                        selected = uiState.selectedCategory == key,
                        onClick = { viewModel.selectCategory(key) },
                        label = { Text(label) },
                    )
                }
            }

            // 문구 목록
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            } else if (uiState.phrases.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(stringResource(R.string.phrase_empty), fontSize = 18.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(onClick = { viewModel.addAllPresets() }) {
                            Icon(Icons.Default.PlaylistAdd, null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.phrase_add_preset_btn))
                        }
                    }
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(uiState.phrases, key = { it.id }) { phrase ->
                        PhraseItem(
                            phrase = phrase,
                            isPlaying = uiState.playingPhraseId == phrase.id,
                            onPlay = { viewModel.playPhrase(phrase) },
                            onDelete = { viewModel.deletePhrase(phrase.id) },
                        )
                    }
                }
            }
        }

        // 문구 추가 다이얼로그
        if (uiState.showAddDialog) {
            AddPhraseDialog(
                onDismiss = { viewModel.hideAddDialog() },
                onAdd = { text -> viewModel.addPhrase(text) },
            )
        }
    }
}

@Composable
private fun PhraseItem(
    phrase: PhraseResponse,
    isPlaying: Boolean,
    onPlay: () -> Unit,
    onDelete: () -> Unit,
) {
    ListItem(
        headlineContent = {
            Text(text = phrase.phrase_text, fontSize = 20.sp)
        },
        supportingContent = {
            val categoryLabel = CATEGORY_LABELS.entries
                .firstOrNull { it.key == phrase.category }?.value ?: phrase.category
            Text("$categoryLabel · 사용 ${phrase.usage_count}회")
        },
        leadingContent = {
            IconButton(onClick = onPlay, enabled = !isPlaying) {
                if (isPlaying) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                } else {
                    Icon(
                        Icons.Default.PlayCircle,
                        contentDescription = stringResource(R.string.phrase_play),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp),
                    )
                }
            }
        },
        trailingContent = {
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, stringResource(R.string.phrase_delete), tint = MaterialTheme.colorScheme.error)
            }
        },
    )
}

@Composable
private fun AddPhraseDialog(
    onDismiss: () -> Unit,
    onAdd: (String) -> Unit,
) {
    var text by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.phrase_new_title)) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text(stringResource(R.string.phrase_input_label)) },
                modifier = Modifier.fillMaxWidth(),
                textStyle = LocalTextStyle.current.copy(fontSize = 18.sp),
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onAdd(text) },
                enabled = text.isNotBlank(),
            ) {
                Text(stringResource(R.string.phrase_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.phrase_cancel)) }
        },
    )
}
