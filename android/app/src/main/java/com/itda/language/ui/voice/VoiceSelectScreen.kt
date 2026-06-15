package com.itda.language.ui.voice

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.itda.language.R
import com.itda.language.data.model.VoicePersona
import com.itda.language.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceSelectScreen(
    currentSpeaker: String,
    isVoiceLocked: Boolean = false,
    onVoiceSelected: (speaker: String, displayName: String) -> Unit,
    onVoiceChangeRequested: (speaker: String, displayName: String, reason: String) -> Unit = { _, _, _ -> },
    onBack: () -> Unit,
    viewModel: VoiceSelectViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    var selectedSpeaker by remember { mutableStateOf(currentSpeaker) }
    var selectedDisplayName by remember { mutableStateOf("") }
    var showRequestDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadCategories()
        viewModel.loadPersonas()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.voice_select_title), fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.voice_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                ),
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 4.dp,
                shadowElevation = 8.dp,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .navigationBarsPadding(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedButton(
                        onClick = onBack,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.phrase_cancel),
                            fontSize = 16.sp,
                        )
                    }

                    if (isVoiceLocked) {
                        // 잠금 상태: "변경 요청" 버튼
                        Button(
                            onClick = { showRequestDialog = true },
                            enabled = selectedSpeaker != currentSpeaker && selectedDisplayName.isNotEmpty(),
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Tertiary,
                                contentColor = SurfaceWhite,
                            ),
                        ) {
                            Icon(Icons.Default.Lock, null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("변경 요청", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        // 최초 선택: "확인" 버튼
                        Button(
                            onClick = {
                                if (selectedSpeaker.isNotEmpty() && selectedDisplayName.isNotEmpty()) {
                                    onVoiceSelected(selectedSpeaker, selectedDisplayName)
                                }
                            },
                            enabled = selectedSpeaker != currentSpeaker && selectedDisplayName.isNotEmpty(),
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = WarmOrangeCoral,
                                contentColor = SurfaceWhite,
                            ),
                        ) {
                            Text(
                                text = stringResource(R.string.btn_confirm),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
        ) {
            // 잠금 안내 배너
            if (isVoiceLocked) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = SurfaceContainerHigh,
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Default.Lock, null, modifier = Modifier.size(18.dp), tint = SecondaryText)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "음성이 확정되었습니다. 변경하려면 관리자 승인이 필요합니다.",
                            style = MaterialTheme.typography.bodySmall,
                            color = SecondaryText,
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            Spacer(modifier = Modifier.height(4.dp))

            FilterSection(
                title = stringResource(R.string.voice_gender),
                options = uiState.genders,
                selected = uiState.selectedGender,
                onSelect = { viewModel.selectGender(it) },
            )
            FilterSection(
                title = stringResource(R.string.voice_age),
                options = uiState.ageGroups,
                selected = uiState.selectedAgeGroup,
                onSelect = { viewModel.selectAgeGroup(it) },
            )
            FilterSection(
                title = stringResource(R.string.voice_region),
                options = uiState.regions,
                selected = uiState.selectedRegion,
                onSelect = { viewModel.selectRegion(it) },
            )
            FilterSection(
                title = stringResource(R.string.voice_tone),
                options = uiState.tones,
                selected = uiState.selectedTone,
                onSelect = { viewModel.selectTone(it) },
            )

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))

            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (uiState.personas.isEmpty()) {
                Text(
                    text = stringResource(R.string.voice_no_match),
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(uiState.personas) { persona ->
                        VoicePersonaItem(
                            persona = persona,
                            isSelected = persona.clova_speaker == selectedSpeaker,
                            onClick = {
                                selectedSpeaker = persona.clova_speaker
                                selectedDisplayName = persona.display_name
                            },
                        )
                    }
                }
            }
        }
    }

    // 변경 요청 사유 입력 다이얼로그
    if (showRequestDialog) {
        VoiceChangeRequestDialog(
            selectedVoiceName = selectedDisplayName,
            onDismiss = { showRequestDialog = false },
            onSubmit = { reason ->
                showRequestDialog = false
                onVoiceChangeRequested(selectedSpeaker, selectedDisplayName, reason)
            },
        )
    }
}

@Composable
private fun VoiceChangeRequestDialog(
    selectedVoiceName: String,
    onDismiss: () -> Unit,
    onSubmit: (reason: String) -> Unit,
) {
    var reason by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("음성 변경 요청", color = BaseText) },
        text = {
            Column {
                Text(
                    text = "변경 희망 음성: $selectedVoiceName",
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("변경 사유") },
                    placeholder = { Text("변경이 필요한 이유를 입력해주세요") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = WarmOrangeCoral,
                        cursorColor = WarmOrangeCoral,
                    ),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSubmit(reason) },
                enabled = reason.isNotBlank(),
            ) {
                Text("요청 제출", color = if (reason.isNotBlank()) WarmOrangeCoral else SecondaryText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.phrase_cancel)) }
        },
    )
}

@Composable
private fun FilterSection(
    title: String,
    options: Map<String, String>,
    selected: String?,
    onSelect: (String?) -> Unit,
) {
    if (options.isEmpty()) return
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(vertical = 4.dp),
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                FilterChip(
                    selected = selected == null,
                    onClick = { onSelect(null) },
                    label = { Text(stringResource(R.string.voice_all)) },
                )
            }
            items(options.entries.toList()) { (key, label) ->
                FilterChip(
                    selected = selected == key,
                    onClick = { onSelect(if (selected == key) null else key) },
                    label = { Text(label) },
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
    }
}

@Composable
private fun VoicePersonaItem(
    persona: VoicePersona,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    ListItem(
        headlineContent = {
            Text(
                text = persona.display_name,
                fontSize = 16.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) WarmOrangeCoral else MaterialTheme.colorScheme.onSurface,
            )
        },
        supportingContent = {
            val tags = listOfNotNull(
                if (persona.emotion_support) stringResource(R.string.voice_emotion) else null,
            ).joinToString(" · ")
            if (tags.isNotEmpty()) Text(tags)
        },
        trailingContent = {
            if (isSelected) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = stringResource(R.string.voice_selected),
                    tint = WarmOrangeCoral,
                )
            }
        },
        modifier = Modifier.clickable(onClick = onClick),
    )
}
