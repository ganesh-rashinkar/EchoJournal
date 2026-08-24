package com.plcoding.echojournal.echos.presentation.models

import com.plcoding.echojournal.echos.presentation.echos.models.PlaybackState
import com.plcoding.echojournal.echos.presentation.util.toReadableTime
import java.time.Instant
import kotlin.time.Duration

data class EchoUi (
    val id:Int,
    val title:String,
    val mood: MoodUi,
    val recordedAt: Instant,
    val note:String?,
    val topics:List<String>,
    val amplitudes:List<Float>,
    val playbackTotalDuration: Duration,
    val audioFilePath: String,
    val playbackCurrentDuration: Duration = Duration.ZERO,
    val playbackState: PlaybackState = PlaybackState.STOPPED
){
    val formattedRecordedAt  = recordedAt.toReadableTime()
    val playbackRatio = (playbackCurrentDuration/playbackTotalDuration).toFloat()
}