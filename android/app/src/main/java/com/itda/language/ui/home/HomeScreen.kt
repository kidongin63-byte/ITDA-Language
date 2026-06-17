package com.itda.language.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.itda.language.R
import com.itda.language.ui.main.MainViewModel
import com.itda.language.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    mainViewModel: MainViewModel,
    onNavigateToVoiceSelect: () -> Unit,
    onNavigateToPhrases: () -> Unit,
    onNavigateToAdmin: () -> Unit = {},
    onLogout: () -> Unit = {},
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var selectedTab by remember { mutableIntStateOf(1) } // 기본: 텍스트→음성 탭

    // 관리자 접근용 탭 카운터
    var settingsTapCount by remember { mutableIntStateOf(0) }
    var lastSettingsTapTime by remember { mutableLongStateOf(0L) }
    val uiState by mainViewModel.uiState.collectAsState()

    // 탭별 상태 텍스트
    val statusText = when {
        selectedTab == 0 -> stringResource(R.string.status_analyzing)
        uiState.isSynthesizing -> stringResource(R.string.status_synthesizing)
        uiState.isPlaying -> stringResource(R.string.status_playing)
        else -> null
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Image(
                        painter = painterResource(id = R.drawable.itda_bridge),
                        contentDescription = "ITDA",
                        modifier = Modifier.height(28.dp),
                        contentScale = ContentScale.Fit,
                    )
                },
                navigationIcon = {
                    // 웹앱 버튼
                    IconButton(onClick = {
                        val intent = android.content.Intent(context, com.itda.language.ui.webview.WebViewActivity::class.java)
                        context.startActivity(intent)
                    }) {
                        Icon(
                            Icons.Default.Home,
                            contentDescription = "웹앱",
                            tint = OnSurface,
                        )
                    }
                },
                actions = {
                    // 상태 표시
                    if (statusText != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(end = 8.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Error),
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = statusText,
                                style = MaterialTheme.typography.labelMedium,
                                color = OnSurface,
                            )
                        }
                    }
                    // 설정 버튼
                    IconButton(onClick = onNavigateToVoiceSelect) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = stringResource(R.string.btn_settings),
                            tint = OnSurface,
                        )
                    }
                    // 로그아웃 버튼
                    IconButton(onClick = onLogout) {
                        Icon(
                            Icons.Default.ExitToApp,
                            contentDescription = "로그아웃",
                            tint = OnSurface,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = WarmIvory,
                ),
            )
        },
        containerColor = WarmIvory,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            // ─── 탭 바 ───
            TabSelector(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it },
                modifier = Modifier.padding(horizontal = 20.dp),
            )

            // ─── 탭 콘텐츠 ───
            when (selectedTab) {
                0 -> SignToVoiceTab()
                1 -> TextToSpeechTab(
                    viewModel = mainViewModel,
                    onNavigateToVoiceSelect = onNavigateToVoiceSelect,
                    onNavigateToPhrases = onNavigateToPhrases,
                )
            }
        }
    }
}

@Composable
private fun TabSelector(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tabs = listOf(
        stringResource(R.string.tab_sign_to_voice) to 0,
        stringResource(R.string.tab_text_to_speech) to 1,
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceContainerHigh)
            .padding(4.dp),
    ) {
        tabs.forEach { (label, index) ->
            val isSelected = selectedTab == index
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (isSelected) WarmOrangeCoral else SurfaceContainerHigh
                    )
                    .let { mod ->
                        mod
                    },
                contentAlignment = Alignment.Center,
            ) {
                TextButton(
                    onClick = { onTabSelected(index) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = label,
                        color = if (isSelected) SurfaceWhite else SecondaryText,
                        fontSize = 14.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    )
                }
            }
        }
    }
}
