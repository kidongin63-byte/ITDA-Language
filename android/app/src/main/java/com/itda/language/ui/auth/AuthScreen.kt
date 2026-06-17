package com.itda.language.ui.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.itda.language.R
import com.itda.language.ui.theme.*

@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    var isLoginMode by remember { mutableStateOf(true) }
    var emailId by remember { mutableStateOf("") }
    var emailDomain by remember { mutableStateOf("gmail.com") }
    var customDomain by remember { mutableStateOf(false) }
    var customDomainText by remember { mutableStateOf("") }
    val email = "$emailId@${if (customDomain) customDomainText else emailDomain}"
    var nickname by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("") }
    var birthId by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    // 인증 성공 시 홈 화면으로 이동
    LaunchedEffect(uiState.isAuthenticated) {
        if (uiState.isAuthenticated) onAuthSuccess()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WarmIvory)
            .systemBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // 로고
        Image(
            painter = painterResource(id = R.drawable.itda_logo),
            contentDescription = "ITDA 로고",
            modifier = Modifier.size(80.dp),
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.auth_app_title),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Black,
            color = BaseText,
        )
        Text(
            text = stringResource(R.string.auth_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = SecondaryText,
        )

        Spacer(modifier = Modifier.height(40.dp))

        // 이메일 (아이디 + @ + 도메인)
        var domainExpanded by remember { mutableStateOf(false) }
        val domainOptions = listOf("gmail.com", "naver.com", "daum.net", "kakao.com", "hanmail.net", "nate.com", "icloud.com", "outlook.com", "직접입력")

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = emailId,
                onValueChange = { newValue ->
                    emailId = newValue.filter { it.isLetterOrDigit() || it in "._-" }
                        .filter { !it.isLetter() || it in 'a'..'z' || it in 'A'..'Z' }
                },
                label = { Text(stringResource(R.string.auth_email)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.weight(1f),
                textStyle = LocalTextStyle.current.copy(fontSize = 16.sp),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = WarmOrangeCoral,
                    cursorColor = WarmOrangeCoral,
                ),
            )
            Text(" @ ", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = SecondaryText)
            Box(modifier = Modifier.weight(1f)) {
                if (customDomain) {
                    OutlinedTextField(
                        value = customDomainText,
                        onValueChange = { v ->
                            customDomainText = v.filter { it.isLetterOrDigit() || it in ".-" }
                        },
                        placeholder = { Text("도메인 입력", fontSize = 14.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = LocalTextStyle.current.copy(fontSize = 14.sp),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        trailingIcon = {
                            IconButton(onClick = { customDomain = false; customDomainText = "" }) {
                                Icon(Icons.Default.KeyboardArrowDown, null, tint = SecondaryText)
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = WarmOrangeCoral,
                            cursorColor = WarmOrangeCoral,
                        ),
                    )
                } else {
                    OutlinedTextField(
                        value = emailDomain,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = LocalTextStyle.current.copy(fontSize = 14.sp),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        trailingIcon = {
                            IconButton(onClick = { domainExpanded = !domainExpanded }) {
                                Icon(
                                    if (domainExpanded) Icons.Default.KeyboardArrowUp
                                    else Icons.Default.KeyboardArrowDown,
                                    null, tint = SecondaryText,
                                )
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = WarmOrangeCoral,
                            cursorColor = WarmOrangeCoral,
                        ),
                    )
                    DropdownMenu(
                        expanded = domainExpanded,
                        onDismissRequest = { domainExpanded = false },
                    ) {
                        domainOptions.forEach { domain ->
                            DropdownMenuItem(
                                text = { Text(domain) },
                                onClick = {
                                    if (domain == "직접입력") {
                                        customDomain = true
                                    } else {
                                        emailDomain = domain
                                    }
                                    domainExpanded = false
                                },
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 회원가입 추가 필드
        if (!isLoginMode) {
            // 이름 (실명)
            OutlinedTextField(
                value = nickname,
                onValueChange = { nickname = it },
                label = { Text("이름") },
                modifier = Modifier.fillMaxWidth(),
                textStyle = LocalTextStyle.current.copy(fontSize = 18.sp),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = WarmOrangeCoral, cursorColor = WarmOrangeCoral,
                ),
            )
            Spacer(modifier = Modifier.height(10.dp))

            // 전화번호
            OutlinedTextField(
                value = phone,
                onValueChange = { v -> phone = v.filter { it.isDigit() || it == '-' }.take(13) },
                label = { Text("전화번호") },
                placeholder = { Text("010-1234-5678") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth(),
                textStyle = LocalTextStyle.current.copy(fontSize = 18.sp),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = WarmOrangeCoral, cursorColor = WarmOrangeCoral,
                ),
            )
            Spacer(modifier = Modifier.height(10.dp))

            // 성별 + 주민번호 앞자리
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // 성별 선택
                var genderExpanded by remember { mutableStateOf(false) }
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = when (gender) {
                            "male" -> "남성"
                            "female" -> "여성"
                            else -> ""
                        },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("성별") },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = LocalTextStyle.current.copy(fontSize = 18.sp),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        trailingIcon = {
                            IconButton(onClick = { genderExpanded = !genderExpanded }) {
                                Icon(Icons.Default.KeyboardArrowDown, null, tint = SecondaryText)
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = WarmOrangeCoral, cursorColor = WarmOrangeCoral,
                        ),
                    )
                    DropdownMenu(
                        expanded = genderExpanded,
                        onDismissRequest = { genderExpanded = false },
                    ) {
                        DropdownMenuItem(text = { Text("남성") }, onClick = { gender = "male"; genderExpanded = false })
                        DropdownMenuItem(text = { Text("여성") }, onClick = { gender = "female"; genderExpanded = false })
                    }
                }

                // 주민번호 앞자리 (생년월일 6자리)
                OutlinedTextField(
                    value = birthId,
                    onValueChange = { v -> birthId = v.filter { it.isDigit() }.take(6) },
                    label = { Text("생년월일") },
                    placeholder = { Text("YYMMDD") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    textStyle = LocalTextStyle.current.copy(fontSize = 18.sp),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = WarmOrangeCoral, cursorColor = WarmOrangeCoral,
                    ),
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        // 비밀번호
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text(stringResource(R.string.auth_password)) },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth(),
            textStyle = LocalTextStyle.current.copy(fontSize = 18.sp),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = WarmOrangeCoral,
                cursorColor = WarmOrangeCoral,
            ),
        )

        Spacer(modifier = Modifier.height(24.dp))

        // 에러 메시지
        if (uiState.error != null) {
            Text(
                text = uiState.error!!,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        // 로그인/회원가입 버튼
        Button(
            onClick = {
                if (isLoginMode) {
                    viewModel.login(email, password)
                } else {
                    viewModel.register(email, nickname, password, phone, gender, birthId)
                }
            },
            enabled = !uiState.isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = WarmOrangeCoral,
                contentColor = SurfaceWhite,
            ),
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = SurfaceWhite,
                )
            } else {
                Text(
                    text = if (isLoginMode) stringResource(R.string.auth_login) else stringResource(R.string.auth_register),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 모드 전환
        TextButton(onClick = { isLoginMode = !isLoginMode }) {
            Text(
                text = if (isLoginMode) stringResource(R.string.auth_no_account) else stringResource(R.string.auth_has_account),
                fontSize = 16.sp,
                color = SecondaryText,
            )
        }
    }
}
