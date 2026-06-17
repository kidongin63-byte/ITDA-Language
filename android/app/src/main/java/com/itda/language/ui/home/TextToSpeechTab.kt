package com.itda.language.ui.home

import android.Manifest
import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.itda.language.R
import com.itda.language.ui.components.EqualizerVisualizer
import com.itda.language.ui.components.HighlightedText
import com.itda.language.ui.components.SpeakerWarningBanner
import com.itda.language.ui.main.MainViewModel
import com.itda.language.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextToSpeechTab(
    viewModel: MainViewModel,
    onNavigateToVoiceSelect: () -> Unit,
    onNavigateToPhrases: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showSliderSheet by remember { mutableStateOf(false) }

    // RECORD_AUDIO 권한 요청
    val audioPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.hasAudioPermission = granted
    }

    // 최초 1회 권한 요청
    LaunchedEffect(Unit) {
        audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

    // 진동 피드백
    LaunchedEffect(uiState.isPlaying) {
        if (uiState.isPlaying) vibrate(context, 100)
    }
    LaunchedEffect(uiState.playbackComplete) {
        if (uiState.playbackComplete) {
            vibrate(context, 50)
            viewModel.clearPlaybackComplete()
        }
    }
    // 스피커 경고 시 이중 진동
    LaunchedEffect(uiState.speakerWarning) {
        if (uiState.speakerWarning != null) {
            vibrate(context, 80)
            kotlinx.coroutines.delay(150)
            vibrate(context, 80)
        }
    }

    // 재생 중 문장 단위 펄스 진동 — 농인이 손으로 "지금 말하고 있다"를 느낄 수 있도록
    val lastSentenceIndex = remember { mutableIntStateOf(-1) }
    LaunchedEffect(uiState.isPlaying) {
        if (!uiState.isPlaying) {
            lastSentenceIndex.intValue = -1
        }
    }
    LaunchedEffect(uiState.isPlaying, uiState.playbackProgress) {
        if (!uiState.isPlaying || uiState.playbackProgress <= 0f) return@LaunchedEffect

        val text = uiState.inputText
        if (text.isBlank()) return@LaunchedEffect

        // 문장 경계 위치를 텍스트 비율(0~1)로 계산
        val sentenceEnds = mutableListOf<Float>()
        text.forEachIndexed { index, char ->
            if (char in listOf('.', '!', '?', '。', '\n')) {
                sentenceEnds.add((index + 1).toFloat() / text.length)
            }
        }
        // 마지막에 문장부호가 없으면 끝을 추가
        if (sentenceEnds.isEmpty() || sentenceEnds.last() < 0.99f) {
            sentenceEnds.add(1f)
        }

        // 현재 진행률이 어느 문장에 해당하는지 계산
        val currentSentence = sentenceEnds.indexOfFirst { it > uiState.playbackProgress }
            .let { if (it == -1) sentenceEnds.lastIndex else it }

        // 새 문장에 진입하면 짧은 펄스 진동 (시작 진동과 중복 방지: index 0은 건너뜀)
        if (currentSentence > 0 && currentSentence != lastSentenceIndex.intValue) {
            lastSentenceIndex.intValue = currentSentence
            vibrate(context, 30)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // ─── 원형 영역 + LIVE AI 배지 ───
            Box(
                modifier = Modifier.size(180.dp),
                contentAlignment = Alignment.Center,
            ) {
                // 원형 배경
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(SurfaceContainerLow),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.itda_logo),
                        contentDescription = "ITDA",
                        modifier = Modifier.size(80.dp),
                        contentScale = ContentScale.Fit,
                    )
                }

                // LIVE AI 배지
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp),
                    shape = RoundedCornerShape(20.dp),
                    color = TertiaryContainer,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(SurfaceWhite),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "LIVE AI",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = SurfaceWhite,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ─── 이퀄라이저 (기존 WaveformIndicator 대체) ───
            EqualizerVisualizer(
                amplitudeLevels = uiState.amplitudeLevels,
                isActive = uiState.isPlaying,
                modifier = Modifier.height(56.dp),
            )

            Spacer(modifier = Modifier.height(12.dp))

            // ─── 스피커 경고 배너 ───
            SpeakerWarningBanner(
                warning = uiState.speakerWarning,
                onBoostVolume = { viewModel.boostVolume() },
                onDismiss = { viewModel.dismissSpeakerWarning() },
            )

            // ─── 음성 선택 칩 ───
            Surface(
                onClick = onNavigateToVoiceSelect,
                shape = RoundedCornerShape(20.dp),
                color = SurfaceContainerHigh,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Default.RecordVoiceOver,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = OnSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = uiState.selectedVoiceName,
                        style = MaterialTheme.typography.labelMedium,
                        color = OnSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = OnSurfaceVariant,
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ─── 텍스트 영역: 입력 ↔ 하이라이트 전환 ───
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .shadow(
                        elevation = 4.dp,
                        shape = RoundedCornerShape(16.dp),
                        ambientColor = Color(0x0F2E2520),
                        spotColor = Color(0x0F2E2520),
                    ),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .animateContentSize(),
                ) {
                    // 재생 중: 하이라이트 텍스트 / 대기: 입력 필드
                    Crossfade(
                        targetState = uiState.isPlaying,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        label = "textMode",
                    ) { isPlaying ->
                        if (isPlaying) {
                            // 카라오케식 하이라이트
                            HighlightedText(
                                text = uiState.inputText,
                                progress = uiState.playbackProgress,
                                modifier = Modifier.fillMaxSize(),
                            )
                        } else {
                            // 텍스트 입력
                            OutlinedTextField(
                                value = uiState.inputText,
                                onValueChange = { newText ->
                                    // 문장 끝(. ! ? 。)에서 자동 줄바꿈
                                    val processed = if (newText.length > uiState.inputText.length) {
                                        val lastChar = newText.lastOrNull()
                                        if (lastChar in listOf('.', '!', '?', '。') && !newText.endsWith("\n")) {
                                            newText + "\n"
                                        } else newText
                                    } else newText
                                    viewModel.updateText(processed)
                                },
                                modifier = Modifier.fillMaxSize(),
                                textStyle = LocalTextStyle.current.copy(
                                    fontSize = 22.sp,
                                    color = BaseText,
                                ),
                                placeholder = {
                                    Text(
                                        text = stringResource(R.string.tts_placeholder),
                                        fontSize = 20.sp,
                                        color = SecondaryText.copy(alpha = 0.5f),
                                    )
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = WarmOrangeCoral,
                                    unfocusedBorderColor = OutlineVariant,
                                    cursorColor = WarmOrangeCoral,
                                ),
                            )
                        }
                    }

                    // 에러 메시지
                    if (uiState.error != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = uiState.error!!,
                            color = Error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                        )
                    }

                    // 합성 중 프로그레스
                    if (uiState.isSynthesizing) {
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth(),
                            color = WarmOrangeCoral,
                            trackColor = SurfaceContainerHigh,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.tts_synthesizing),
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelSmall,
                            color = SecondaryText,
                        )
                    }

                    // 재생 중 진행률 바
                    if (uiState.isPlaying) {
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { uiState.playbackProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = WarmOrangeCoral,
                            trackColor = SurfaceContainerHigh,
                        )
                    }

                    // 하단 도구 행
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // 즐겨찾기 버튼
                        TextButton(onClick = onNavigateToPhrases) {
                            Icon(
                                Icons.Default.Bookmarks,
                                contentDescription = stringResource(R.string.tts_favorites),
                                modifier = Modifier.size(18.dp),
                                tint = SecondaryText,
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.tts_favorites),
                                style = MaterialTheme.typography.labelMedium,
                                color = SecondaryText,
                            )
                        }
                        // 음성 조절 버튼
                        TextButton(onClick = { showSliderSheet = true }) {
                            Icon(
                                Icons.Default.Tune,
                                contentDescription = stringResource(R.string.tts_voice_adjust),
                                modifier = Modifier.size(18.dp),
                                tint = SecondaryText,
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.tts_adjust),
                                style = MaterialTheme.typography.labelMedium,
                                color = SecondaryText,
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(80.dp)) // FAB 공간 확보
        }

        // ─── FAB: 말하기 / 중지 ───
        FloatingActionButton(
            onClick = {
                if (uiState.isPlaying) {
                    viewModel.stopPlaying()
                } else {
                    viewModel.synthesizeAndPlay()
                }
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .size(64.dp),
            shape = CircleShape,
            containerColor = if (uiState.isPlaying) Error else WarmOrangeCoral,
            contentColor = SurfaceWhite,
        ) {
            Icon(
                imageVector = if (uiState.isPlaying) Icons.Default.Stop else Icons.Default.VolumeUp,
                contentDescription = if (uiState.isPlaying) stringResource(R.string.tts_stop) else stringResource(R.string.tts_speak),
                modifier = Modifier.size(28.dp),
            )
        }
    }

    // ─── 슬라이더 바텀시트 ───
    if (showSliderSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSliderSheet = false },
            containerColor = SurfaceWhite,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 16.dp),
            ) {
                Text(
                    text = stringResource(R.string.tts_voice_adjust),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = BaseText,
                )
                Spacer(modifier = Modifier.height(8.dp))
                SliderControl(stringResource(R.string.tts_speed), uiState.speed) { viewModel.updateSpeed(it) }
                SliderControl(stringResource(R.string.tts_pitch), uiState.pitch) { viewModel.updatePitch(it) }
                SliderControl(stringResource(R.string.tts_volume), uiState.volume) { viewModel.updateVolume(it) }
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { showSliderSheet = false },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
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
}

@Composable
private fun SliderControl(label: String, value: Int, onValueChange: (Int) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = BaseText,
            modifier = Modifier.width(48.dp),
        )
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.toInt()) },
            valueRange = -5f..5f,
            steps = 9,
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(
                thumbColor = WarmOrangeCoral,
                activeTrackColor = WarmOrangeCoral,
                inactiveTrackColor = SurfaceContainerHigh,
            ),
        )
        Text(
            text = if (value >= 0) "+$value" else "$value",
            style = MaterialTheme.typography.bodySmall,
            color = SecondaryText,
            modifier = Modifier.width(36.dp),
        )
    }
}

private fun vibrate(context: Context, durationMs: Long) {
    val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    vibrator?.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
}
