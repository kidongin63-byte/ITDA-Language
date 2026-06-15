package com.itda.language.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.SpeakerPhone
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.itda.language.R
import com.itda.language.audio.SpeakerWarning
import com.itda.language.ui.theme.*

@Composable
fun SpeakerWarningBanner(
    warning: SpeakerWarning?,
    onBoostVolume: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = warning != null,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
        modifier = modifier,
    ) {
        if (warning != null) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                shape = RoundedCornerShape(12.dp),
                color = ErrorContainer,
                tonalElevation = 2.dp,
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = when (warning) {
                            is SpeakerWarning.VolumeMuted -> Icons.Default.VolumeOff
                            is SpeakerWarning.VolumeLow -> Icons.Default.VolumeDown
                            is SpeakerWarning.SpeakerUnavailable -> Icons.Default.SpeakerPhone
                        },
                        contentDescription = null,
                        tint = OnErrorContainer,
                        modifier = Modifier.size(24.dp),
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = when (warning) {
                                is SpeakerWarning.VolumeMuted -> stringResource(R.string.speaker_muted)
                                is SpeakerWarning.VolumeLow -> stringResource(R.string.speaker_low)
                                is SpeakerWarning.SpeakerUnavailable -> stringResource(R.string.speaker_unavailable)
                            },
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = OnErrorContainer,
                        )
                        Text(
                            text = when (warning) {
                                is SpeakerWarning.VolumeMuted -> stringResource(R.string.speaker_muted_desc)
                                is SpeakerWarning.VolumeLow -> stringResource(R.string.speaker_low_desc)
                                is SpeakerWarning.SpeakerUnavailable -> stringResource(R.string.speaker_unavailable_desc)
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = OnErrorContainer.copy(alpha = 0.8f),
                        )
                    }
                    if (warning !is SpeakerWarning.SpeakerUnavailable) {
                        TextButton(onClick = onBoostVolume) {
                            Text(
                                text = stringResource(R.string.speaker_boost),
                                color = Error,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
        }
    }
}
