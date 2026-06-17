package com.itda.language.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.itda.language.data.repository.AuthRepository
import com.itda.language.ui.auth.AuthScreen
import com.itda.language.ui.home.HomeScreen
import com.itda.language.ui.main.MainViewModel
import com.itda.language.ui.onboarding.OnboardingScreen
import com.itda.language.ui.phrase.PhraseScreen
import com.itda.language.ui.theme.ITDATheme
import com.itda.language.ui.theme.WarmIvory
import com.itda.language.ui.voice.VoiceSelectScreen
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StartupViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _startDestination = MutableStateFlow<String?>(null)
    val startDestination: StateFlow<String?> = _startDestination.asStateFlow()

    // 스플래시 유지 여부
    val isReady: Boolean get() = _startDestination.value != null

    init {
        viewModelScope.launch {
            val destination = if (authRepository.isLoggedIn()) "home" else "onboarding"
            _startDestination.value = destination
        }
    }
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            val startupViewModel: StartupViewModel = hiltViewModel()

            // 토큰 확인 완료될 때까지 스플래시 유지
            splashScreen.setKeepOnScreenCondition { !startupViewModel.isReady }

            ITDATheme {
                val startDest by startupViewModel.startDestination.collectAsState()
                if (startDest != null) {
                    ITDANavigation(startDestination = startDest!!)
                } else {
                    // 스플래시가 덮고 있으므로 빈 배경만
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(WarmIvory),
                    )
                }
            }
        }
    }
}

@Composable
fun ITDANavigation(startDestination: String) {
    val navController = rememberNavController()

    // MainViewModel을 네비게이션 레벨에서 공유 (음성 선택 결과 반영)
    val mainViewModel: MainViewModel = hiltViewModel()

    NavHost(navController = navController, startDestination = startDestination) {

        // 온보딩 (최초 1회만)
        composable("onboarding") {
            OnboardingScreen(
                onStartClick = {
                    navController.navigate("auth") {
                        popUpTo("onboarding") { inclusive = true }
                    }
                },
            )
        }

        // 인증 (로그인/회원가입)
        composable("auth") {
            AuthScreen(
                onAuthSuccess = {
                    navController.navigate("home") {
                        popUpTo("auth") { inclusive = true }
                    }
                },
            )
        }

        // 홈 (탭 구조: 수어→음성 | 텍스트→음성)
        composable("home") {
            HomeScreen(
                mainViewModel = mainViewModel,
                onNavigateToVoiceSelect = {
                    navController.navigate("voice_select")
                },
                onNavigateToPhrases = {
                    navController.navigate("phrases")
                },
                onNavigateToAdmin = {
                    navController.navigate("admin")
                },
                onLogout = {
                    navController.navigate("auth") {
                        popUpTo("home") { inclusive = true }
                    }
                },
            )
        }

        composable("phrases") {
            val uiState by mainViewModel.uiState.collectAsState()
            PhraseScreen(
                currentSpeaker = uiState.selectedSpeaker,
                onBack = { navController.popBackStack() },
            )
        }

        composable("admin") {
            com.itda.language.ui.admin.AdminScreen(
                onBack = { navController.popBackStack() },
            )
        }

        composable("voice_select") {
            val uiState by mainViewModel.uiState.collectAsState()
            VoiceSelectScreen(
                currentSpeaker = uiState.selectedSpeaker,
                isVoiceLocked = uiState.voiceLocked,
                onVoiceSelected = { speaker, displayName ->
                    mainViewModel.selectVoiceAndLock(speaker, displayName)
                    navController.popBackStack()
                },
                onVoiceChangeRequested = { speaker, displayName, reason ->
                    mainViewModel.requestVoiceChange(speaker, displayName, reason)
                    navController.popBackStack()
                },
                onBack = { navController.popBackStack() },
            )
        }
    }
}
