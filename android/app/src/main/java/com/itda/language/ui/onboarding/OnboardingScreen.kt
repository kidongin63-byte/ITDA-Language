package com.itda.language.ui.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.itda.language.R
import com.itda.language.ui.theme.*

@Composable
fun OnboardingScreen(
    onStartClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WarmIvory)
            .padding(horizontal = 32.dp)
            .systemBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Spacer(modifier = Modifier.weight(1f))

        // ITDA 로고
        Image(
            painter = painterResource(id = R.drawable.itda_logo),
            contentDescription = "ITDA 로고",
            modifier = Modifier.size(160.dp),
        )

        Spacer(modifier = Modifier.height(32.dp))

        // 메인 태그라인
        Text(
            text = stringResource(R.string.onboarding_tagline),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center,
            color = BaseText,
            lineHeight = 42.sp,
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 서브 태그라인
        Text(
            text = stringResource(R.string.onboarding_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = SecondaryText,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.weight(1f))

        // 시작하기 버튼
        Button(
            onClick = onStartClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = WarmOrangeCoral,
                contentColor = SurfaceWhite,
            ),
        ) {
            Text(
                text = stringResource(R.string.onboarding_start),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 언어 선택 (장식)
        Text(
            text = stringResource(R.string.onboarding_language),
            style = MaterialTheme.typography.bodySmall,
            color = SecondaryText,
        )

        Spacer(modifier = Modifier.height(32.dp))
    }
}
