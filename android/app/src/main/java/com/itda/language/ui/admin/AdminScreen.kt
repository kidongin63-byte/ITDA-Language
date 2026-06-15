package com.itda.language.ui.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.itda.language.data.api.VoiceChangeRequestDto
import com.itda.language.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(
    onBack: () -> Unit,
    viewModel: AdminViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    // PIN 입력이 안 된 상태면 PIN 다이얼로그 표시
    if (!uiState.isAdmin) {
        AdminPinDialog(
            onDismiss = onBack,
            onConfirm = { pin -> viewModel.verifyPin(pin) },
            isLoading = uiState.isLoading,
            error = uiState.pinError,
        )
        return
    }

    LaunchedEffect(uiState.message) {
        if (uiState.message != null) {
            kotlinx.coroutines.delay(2000)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("관리자", color = BaseText) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "뒤로가기",
                            tint = BaseText,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = WarmIvory,
                ),
            )
        },
        containerColor = WarmIvory,
        snackbarHost = {
            if (uiState.message != null) {
                Snackbar(
                    modifier = Modifier.padding(16.dp),
                ) {
                    Text(uiState.message!!)
                }
            }
        },
    ) { padding ->
        if (uiState.isLoading && uiState.requests.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = WarmOrangeCoral)
            }
        } else if (uiState.requests.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "대기 중인 요청이 없습니다",
                        fontSize = 16.sp,
                        color = SecondaryText,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    TextButton(onClick = { viewModel.loadRequests() }) {
                        Text("새로고침", color = WarmOrangeCoral)
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(uiState.requests, key = { it.id }) { request ->
                    RequestCard(
                        request = request,
                        onApprove = { viewModel.approveRequest(request.id) },
                        onReject = { viewModel.rejectRequest(request.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun RequestCard(
    request: VoiceChangeRequestDto,
    onApprove: () -> Unit,
    onReject: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = request.user_nickname,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = BaseText,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = request.user_email,
                fontSize = 14.sp,
                color = SecondaryText,
            )
            Spacer(modifier = Modifier.height(12.dp))

            DetailRow(label = "현재 음성", value = request.current_voice ?: "없음")
            DetailRow(label = "요청 음성", value = "${request.requested_speaker} / ${request.requested_voice_name}")

            if (request.reason.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "사유: ${request.reason}",
                    fontSize = 14.sp,
                    color = SecondaryText,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedButton(
                    onClick = onReject,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Error),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text("거부")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onApprove,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50),
                        contentColor = Color.White,
                    ),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text("승인")
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(modifier = Modifier.padding(vertical = 2.dp)) {
        Text(
            text = "$label: ",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = BaseText,
        )
        Text(
            text = value,
            fontSize = 14.sp,
            color = SecondaryText,
        )
    }
}
