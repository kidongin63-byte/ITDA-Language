package com.itda.language.ui.main

import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onNavigateToVoiceSelect: () -> Unit,
    onNavigateToPhrases: () -> Unit = {},
    viewModel: MainViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // 재생 시작/완료 시 진동 피드백
    LaunchedEffect(uiState.isPlaying) {
        if (uiState.isPlaying) vibrate(context, 100)
    }
    LaunchedEffect(uiState.playbackComplete) {
        if (uiState.playbackComplete) {
            vibrate(context, 50)
            viewModel.clearPlaybackComplete()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("잇다", fontSize = 22.sp) },
                actions = {
                    IconButton(onClick = onNavigateToPhrases) {
                        Icon(Icons.Default.Bookmarks, "즐겨찾기")
                    }
                    IconButton(onClick = { viewModel.toggleSliders() }) {
                        Icon(Icons.Default.Tune, "음성 조절")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp),
        ) {
            // 현재 선택된 화자 표시
            OutlinedCard(
                onClick = onNavigateToVoiceSelect,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "🎤",
                        fontSize = 24.sp,
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = uiState.selectedVoiceName,
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = "탭하여 음성 변경",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            // 속도/피치/볼륨 슬라이더 (접이식)
            if (uiState.showSliders) {
                Spacer(modifier = Modifier.height(8.dp))
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        SliderControl("속도", uiState.speed) { viewModel.updateSpeed(it) }
                        SliderControl("피치", uiState.pitch) { viewModel.updatePitch(it) }
                        SliderControl("볼륨", uiState.volume) { viewModel.updateVolume(it) }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 텍스트 입력 영역 (농인 사용성: 큰 글씨)
            OutlinedTextField(
                value = uiState.inputText,
                onValueChange = { viewModel.updateText(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                textStyle = LocalTextStyle.current.copy(fontSize = 22.sp),
                placeholder = {
                    Text(
                        text = "여기에 말할 내용을 입력하세요",
                        fontSize = 20.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    )
                },
                shape = RoundedCornerShape(16.dp),
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 에러 메시지
            if (uiState.error != null) {
                Text(
                    text = uiState.error!!,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // 상태 표시 (시각적 피드백)
            if (uiState.isSynthesizing) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "음성 합성 중...",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (uiState.isPlaying) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "🔊 재생 중...",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    fontSize = 18.sp,
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // 말하기 / 중지 버튼
            Button(
                onClick = {
                    if (uiState.isPlaying) {
                        viewModel.stopPlaying()
                    } else {
                        viewModel.synthesizeAndPlay()
                    }
                },
                enabled = !uiState.isSynthesizing && uiState.inputText.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                shape = RoundedCornerShape(16.dp),
                colors = if (uiState.isPlaying) {
                    ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                } else {
                    ButtonDefaults.buttonColors()
                },
            ) {
                Icon(
                    imageVector = if (uiState.isPlaying) Icons.Default.Stop else Icons.Default.RecordVoiceOver,
                    contentDescription = if (uiState.isPlaying) "중지" else "말하기",
                    modifier = Modifier.size(28.dp),
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = if (uiState.isPlaying) "중지" else "말하기",
                    fontSize = 22.sp,
                )
            }
        }
    }
}

@Composable
private fun SliderControl(label: String, value: Int, onValueChange: (Int) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.width(48.dp),
        )
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.toInt()) },
            valueRange = -5f..5f,
            steps = 9,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = if (value >= 0) "+$value" else "$value",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.width(36.dp),
        )
    }
}

private fun vibrate(context: Context, durationMs: Long) {
    val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    vibrator?.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
}
