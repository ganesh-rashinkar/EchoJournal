package com.plcoding.echojournal.echos.presentation.components

import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.plcoding.echojournal.core.presentation.designsystem.theme.EchoJournalTheme
import com.plcoding.echojournal.core.presentation.designsystem.theme.MoodPrimary25
import com.plcoding.echojournal.core.presentation.designsystem.theme.MoodPrimary35
import com.plcoding.echojournal.core.presentation.designsystem.theme.MoodPrimary80
import com.plcoding.echojournal.core.presentation.util.formatMMSS
import com.plcoding.echojournal.echos.presentation.echos.models.PlaybackState
import com.plcoding.echojournal.echos.presentation.echos.models.TrackSizeInfo
import com.plcoding.echojournal.echos.presentation.models.MoodUi
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@Composable
fun EchoMoodPlayer(
    moodUi: MoodUi?,
    playbackState: PlaybackState,
    playerProgress:()-> Float,
    durationPlayed:Duration,
    totalPlaybackDuration: Duration,
    powerRatios:List<Float>,
    onPlayClick:()-> Unit,
    onPauseClick:()->Unit,
    onTrackSizeAvailable:(TrackSizeInfo) -> Unit,
    modifier: Modifier = Modifier,
    amplitudeBarWidth: Dp = 5.dp,
    amplitudeBarSpacing: Dp = 4.dp,
) {
    val iconTint = when(moodUi){
        null -> MoodPrimary80
        else -> moodUi.colorSet.vivid
    }
    val trackFillColor = when(moodUi){
        null -> MoodPrimary80
        else -> moodUi.colorSet.vivid
    }
    val backgroundColor = when(moodUi){
        null -> MoodPrimary25
        else -> moodUi.colorSet.faded
    }
    val trackColor = when(moodUi){
        null -> MoodPrimary35
        else -> moodUi.colorSet.desaturated
    }

    val formattedDurationText = remember(durationPlayed,totalPlaybackDuration) {
        "${durationPlayed.formatMMSS()}/${totalPlaybackDuration.formatMMSS()}"
    }
    Surface(
        shape = CircleShape,
        color = backgroundColor,
        modifier = modifier
    ) {
        val density = LocalDensity.current
        Row(
            modifier = Modifier
                .height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically
        ) {
            EchoPlaybackButton(
                playbackState = playbackState,
                onPlayClick = onPlayClick,
                onPauseClick = onPauseClick,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = iconTint
                )
            )
            EchoPlayBar(
                amplitudeBarWidth = amplitudeBarWidth,
                amplitudeBarSpacing = amplitudeBarSpacing,
                powerRatios = powerRatios,
                trackColor = trackColor,
                trackFillColor = trackFillColor,
                playerProgress = playerProgress,
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 10.dp,
                        horizontal = 8.dp
                        )
                    .fillMaxHeight()
                    .onSizeChanged{
                        onTrackSizeAvailable(
                        TrackSizeInfo(
                            trackWidth = it.width.toFloat(),
                            barWidth = with(density){
                                amplitudeBarWidth.toPx()
                            },
                            spacing = with(density){
                                amplitudeBarSpacing.toPx()
                            }
                        )
                        )
                    }
            )
            Text(
                formattedDurationText,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(8.dp),
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFeatureSettings = "tnum"
                )
            )
        }
    }
}

@Preview
@Composable
private fun EchoMoodPlayerPreview() {
    EchoJournalTheme {
        val ratios=(1..30).map {
            Random.nextFloat()
        }
        EchoMoodPlayer(
            moodUi = MoodUi.NEUTRAL,
            playerProgress = {0.3f},
            playbackState = PlaybackState.PLAYING,
            durationPlayed = 1.seconds,
            totalPlaybackDuration = 5.seconds,
            powerRatios = ratios,
            onPlayClick = {},
            onPauseClick = {},
            onTrackSizeAvailable = {},
            modifier = Modifier.fillMaxWidth()
        )
    }
}