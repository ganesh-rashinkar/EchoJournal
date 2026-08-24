package com.plcoding.echojournal.echos.presentation.echos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.plcoding.echojournal.R
import com.plcoding.echojournal.core.presentation.designsystem.dropdowns.Selectable
import com.plcoding.echojournal.core.presentation.util.UiText
import com.plcoding.echojournal.echos.domain.audio.AudioPlayer
import com.plcoding.echojournal.echos.domain.recording.VoiceRecorder
import com.plcoding.echojournal.echos.presentation.echos.models.AudioCaptureMethod
import com.plcoding.echojournal.echos.presentation.echos.models.EchoFilterChip
import com.plcoding.echojournal.echos.presentation.echos.models.MoodChipContent
import com.plcoding.echojournal.echos.presentation.echos.models.RecordingState
import com.plcoding.echojournal.echos.presentation.models.MoodUi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class EchosViewModel(
    val voiceRecorder: VoiceRecorder,
    val audioPlayer: AudioPlayer) : ViewModel() {

    companion object{
        private val MIN_RECORD_DURATION = 1.5.seconds
    }
    private var hasLoadedInitialData = false
    private val playingEchoId = MutableStateFlow<Int?>(null)

    private val selectedMoodFilters = MutableStateFlow<List<MoodUi>>(emptyList())
    private val selectedTopicFilters = MutableStateFlow<List<String>>(emptyList())

    private val eventChannel = Channel<EchosEvent>()
    val events = eventChannel.receiveAsFlow()

    private val _state = MutableStateFlow(EchosState())
    val state = _state
        .onStart {
            if (!hasLoadedInitialData) {
                observeFilters()
                hasLoadedInitialData = true
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = EchosState()
        )

    fun onAction(action: EchosAction) {
        when (action) {
            EchosAction.OnRequestPermissionQuickRecording -> {
                requestAudioPermission()
                _state.update {
                    it.copy(
                        currentCaptureMethod = AudioCaptureMethod.QUICK
                    )
                }
            }
            EchosAction.OnRecordButtonLongClick ->{
                startRecording(captureMethod = AudioCaptureMethod.QUICK)
            }
            EchosAction.OnFabClick -> {
                requestAudioPermission()
                _state.update {
                    it.copy(
                        currentCaptureMethod = AudioCaptureMethod.STANDARD
                    )
                }
            }
            is EchosAction.OnRemoveFilters -> {
                when(action.filterType){
                    EchoFilterChip.MOODS -> selectedMoodFilters.update { emptyList() }
                    EchoFilterChip.TOPICS -> selectedTopicFilters.update { emptyList() }
                }
            }
            EchosAction.OnTopicChipClick -> {
                _state.update {
                    it.copy(
                        selectedEchoFilterChip = EchoFilterChip.TOPICS
                    )
                }
            }
            EchosAction.OnMoodChipClick -> {
                _state.update {
                    it.copy(
                        selectedEchoFilterChip = EchoFilterChip.MOODS
                    )
                }
            }
            EchosAction.OnSettingsClick -> {}
            EchosAction.OnDismissMoodDropdown -> {
                _state.update {
                    it.copy(selectedEchoFilterChip = null)
                }
            }
            EchosAction.OnDismissTopicDropdown -> {
                _state.update {
                    it.copy(selectedEchoFilterChip = null)
                }
            }
            is EchosAction.OnFilterByMoodClick -> {
                toggleMoodFilter(action.moodUi)
            }
            is EchosAction.OnFilterByTopicClick -> {
               toggleTopicFilter(action.topic)
            }

            EchosAction.OnPauseAudioClick -> {}
            is EchosAction.OnPlayEchoClick -> onPlayEchoClick(action.echoId)
            is EchosAction.OnTrackSizeAvailable -> {}
            is EchosAction.OnAudioPermissionGranted -> {
                startRecording(captureMethod = AudioCaptureMethod.STANDARD)
            }

            EchosAction.OnCancelRecording -> cancelRecording()
            EchosAction.OnPauseRecordingClick -> pauseRecording()
            EchosAction.OnCompleteRecordingClick -> stopRecording()
            EchosAction.OnResumeRecordingClick -> resumeRecording()
        }
    }

    private fun onPlayEchoClick(echoId: Int) {
        val selectedEcho = state.value.echos.values.flatten().first{ it.id == echoId}
        val activeTrack = audioPlayer.activeTrack.value
        val isNewEcho = playingEchoId.value != echoId
        val isSameEchoIsPlayingFromBeginning = echoId == playingEchoId.value && activeTrack != null
                && activeTrack.durationPlayed == Duration.ZERO

        when{
            isNewEcho || isSameEchoIsPlayingFromBeginning -> {
                playingEchoId.update { echoId }
                audioPlayer.stop()
                audioPlayer.play(
                    filePath = selectedEcho.audioFilePath,
                    onComplete = ::completePlayback
                )
            }
            else -> audioPlayer.resume()
        }
    }

    private fun completePlayback(){
        _state.update {
            it.copy(
                echos = it.echos.mapValues {
                    (_,echos) ->
                    echos.map {
                        echo->
                        echo.copy(
                            playbackTotalDuration = Duration.ZERO
                        )
                    }
                }
            )
        }
    }
    private fun requestAudioPermission() = viewModelScope.launch{
        eventChannel.send(EchosEvent.RequestAudioPermission)
    }

    private fun pauseRecording(){
        voiceRecorder.pause()
        _state.update {
            it.copy(
                recordingState = RecordingState.PAUSED
            )
        }
    }

    private fun resumeRecording(){
        voiceRecorder.resume()
        _state.update {
            it.copy(
                recordingState = RecordingState.NORMAL_CAPTURE
            )
        }
    }

    private fun cancelRecording(){
        _state.update {
            it.copy(
                recordingState = RecordingState.NOT_RECORDING,
                currentCaptureMethod = null
            )
        }
        voiceRecorder.cancel()
    }

    private fun stopRecording(){
        voiceRecorder.stop()

        _state.update {
            it.copy(
                recordingState = RecordingState.NOT_RECORDING
            )
        }
        val recordingDetails = voiceRecorder.recordingDetails.value
        viewModelScope.launch {
            if(recordingDetails.duration < MIN_RECORD_DURATION){
                eventChannel.send(EchosEvent.RecordingTooShort)
            }
            else{
                eventChannel.send(EchosEvent.OnDoneRecording(recordingDetails))
            }
        }

    }

    private fun startRecording(captureMethod: AudioCaptureMethod){
        _state.update {
            it.copy(
                recordingState = when(captureMethod){
                    AudioCaptureMethod.STANDARD -> RecordingState.NORMAL_CAPTURE
                    AudioCaptureMethod.QUICK -> RecordingState.QUICK_CAPTURE
                }
            )
        }
        voiceRecorder.start()

        if(captureMethod == AudioCaptureMethod.STANDARD){
            voiceRecorder
                .recordingDetails
                .distinctUntilChangedBy { it.duration }
                .map { it.duration }
                .onEach { duration->
                    _state.update {
                        it.copy(
                            recordingElapsedDuration = duration
                        )
                    }
                }
                .launchIn(viewModelScope)
        }
    }
    private fun toggleMoodFilter(moodUi: MoodUi){
        selectedMoodFilters.update {
            selectedMoods->
            if(moodUi in selectedMoods){
                selectedMoods - moodUi
            }
            else{
                selectedMoods + moodUi
            }
        }

    }

    private fun toggleTopicFilter(topic: String){
        selectedTopicFilters.update {
                selectedTopics->
            if(topic in selectedTopics){
                selectedTopics - topic
            }
            else{
                selectedTopics + topic
            }
        }

    }

    private fun observeFilters(){
        combine(
            selectedTopicFilters,
            selectedMoodFilters
        ){
            selectedTopics,selectedMods->
            _state.update {
                it.copy(
                    topics = it.topics.map {
                        selectableTopic->
                        Selectable(
                            item = selectableTopic.item,
                            selected = selectedTopics.contains(selectableTopic.item)
                        )
                    },
                    moods = MoodUi.values().map {
                        Selectable(
                            item = it,
                            selected = selectedMods.contains(it)
                        )
                    },
                    hasActiveMoodFilters = selectedMods.isNotEmpty(),
                    hasActiveTopicFilters = selectedTopics.isNotEmpty(),
                    topicChipTitle = selectedTopics.deriveTopicsToText(),
                    moodChipContent = selectedMods.deriveMoodChipContent()
                )
            }
        }.launchIn(viewModelScope)
    }
    private fun List<String>.deriveTopicsToText(): UiText{

        return when(size){
            0-> UiText.StringResource(R.string.all_topics)
            1-> UiText.Dynamic(this.first())
            2-> UiText.Dynamic("${this.first()}, ${this.last()}")
            else -> {
                val extraElementCount = size-2
                UiText.Dynamic("${this.first()}, ${this.last()} +$extraElementCount")
            }
        }
    }

    private fun List<MoodUi>.deriveMoodChipContent(): MoodChipContent{
        if(this.isEmpty()){
            return MoodChipContent()
        }
        val icons = this.map{
            it.iconSet.fill
        }
        val moodNames = this.map { it.title }
        return when(size){
            1 -> MoodChipContent(
                iconRes = icons,
                title = moodNames.first()
            )
            2 -> MoodChipContent(
                iconRes = icons,
                title = UiText.Combined(
                    format = "%s, %s ",
                    uiTexts = moodNames.toTypedArray()
                )
            )
            else -> {
                val extraElementsCount = size-2
                MoodChipContent(
                    iconRes = icons,
                    title = UiText.Combined(
                        format = "%s, %s +$extraElementsCount",
                        uiTexts = moodNames.take(2).toTypedArray()
                    )
                )
            }
        }
    }

}