package com.plcoding.echojournal.echos.presentation.create_echo

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.plcoding.echojournal.app.navigation.NavigationRoutes
import com.plcoding.echojournal.core.presentation.designsystem.dropdowns.Selectable.Companion.asUnselectedItems
import com.plcoding.echojournal.echos.domain.audio.AudioPlayer
import com.plcoding.echojournal.echos.domain.echo.Echo
import com.plcoding.echojournal.echos.domain.echo.EchoDataSource
import com.plcoding.echojournal.echos.domain.echo.Mood
import com.plcoding.echojournal.echos.domain.recording.RecordingStorage
import com.plcoding.echojournal.echos.domain.settings.SettingsPreferences
import com.plcoding.echojournal.echos.presentation.echos.models.PlaybackState
import com.plcoding.echojournal.echos.presentation.echos.models.TrackSizeInfo
import com.plcoding.echojournal.echos.presentation.models.MoodUi
import com.plcoding.echojournal.echos.presentation.util.AmplitudeNormalizer
import com.plcoding.echojournal.echos.presentation.util.toRecordingDetails
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

class CreateEchoViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val recordingStorage: RecordingStorage,
    private val audioPlayer: AudioPlayer,
    private val echoDataSource: EchoDataSource,
    private val settingsPreferences: SettingsPreferences
) : ViewModel() {

    private var hasLoadedInitialData = false
    private val route = savedStateHandle.toRoute<NavigationRoutes.CreateEcho>()
    private val recordingDetails = route.toRecordingDetails()

    private val eventChannel = Channel<CreateEchoEvent>()
    val events = eventChannel.receiveAsFlow()

    private val restoredTopics = savedStateHandle.get<String>("topics")?.split(",")
    private val _state = MutableStateFlow(CreateEchoState(
        playbackTotalDuration = recordingDetails.duration,
        titleText = savedStateHandle["titleText"]?:"",
        noteText = savedStateHandle["noteText"]?:"",
        topics = restoredTopics?:emptyList(),
        mood = savedStateHandle.get<String>("mood")?.let {
            MoodUi.valueOf(it)
        },
        showMoodSelector = savedStateHandle.get<String>("mood") == null,
        canSaveEcho = savedStateHandle.get<Boolean>("canSaveEcho") == true
    ))
    val state = _state
        .onStart {
            if (!hasLoadedInitialData) {
                observeAddTopicText()
                fetchDefaultSettings()
                hasLoadedInitialData = true
            }
        }
        .onEach {
            state ->
            savedStateHandle["titleText"] = state.titleText
            savedStateHandle["noteText"] = state.noteText
            savedStateHandle["topics"] = state.topics.joinToString(",")
            savedStateHandle["mood"] = state.mood?.name
            savedStateHandle["canSaveEcho"] = state.canSaveEcho
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = CreateEchoState()
        )


    private var durationJob: Job? = null

    fun onAction(action: CreateEchoAction) {
        when (action) {
            is CreateEchoAction.OnAddTopicTextChange -> onAddTopicTextChange(action.text)
            CreateEchoAction.OnConfirmMood -> onConfirmMood()
            CreateEchoAction.OnDismissMoodSelector -> onDismissMoodSelector()
            CreateEchoAction.OnDismissTopicSuggestions -> onDismissTopicSuggestions()
            is CreateEchoAction.OnMoodClick ->  onMoodClick(action.moodUi)
            is CreateEchoAction.OnNoteTextChange -> onNoteTextChange(action.text)
            CreateEchoAction.OnPauseAudioClick -> audioPlayer.pause()
            CreateEchoAction.OnPlayAudioClick -> onPlayAudioClick()
            is CreateEchoAction.OnRemoveTopicClick -> onRemoveTopicClick(action.topic)
            CreateEchoAction.OnSaveClick -> onSaveClick()
            is CreateEchoAction.OnTitleTextChange -> onTitleTextChange(action.text)
            is CreateEchoAction.OnTopicClick -> onTopicClick(action.topic)
            is CreateEchoAction.OnTrackSizeAvailable -> onTrackSizeAvailable(action.trackSizeInfo)
            CreateEchoAction.OnSelectMoodClick -> onSelectMoodClick()
            CreateEchoAction.OnDismissConfirmLeaveDialog -> onDismissConfirmLeaveMessage()
            CreateEchoAction.OnCancelClick,
            CreateEchoAction.OnNavigateBackClick,
            CreateEchoAction.OnGoBack -> onConfirmLeaveDialog()
        }
    }

    private fun fetchDefaultSettings() {
        settingsPreferences.observeDefaultMood()
            .take(1)
            .onEach {
                defaultMood->
                _state.update {
                    val moodUi=MoodUi.valueOf(defaultMood.name)
                    it.copy(
                        selectedMood = moodUi,
                        mood = moodUi,
                        showMoodSelector = false
                    )
                }
            }
            .launchIn(viewModelScope)

        settingsPreferences.observeDefaultTopics()
            .take(1)
            .onEach {
                    defaultTopics->
                _state.update {
                    it.copy(
                        topics = defaultTopics
                    )
                }
            }
            .launchIn(viewModelScope)

    }
    private fun onNoteTextChange(text: String) {
        _state.update {
            it.copy(
                noteText = text
            )
        }
    }

    private fun onPlayAudioClick() {
        if(state.value.playbackState == PlaybackState.PAUSED){
            audioPlayer.resume()
        }
        else{
            audioPlayer.play(
                filePath = recordingDetails.filePath ?: throw IllegalArgumentException(
                    "File path can't be null"
                ),
                onComplete = {
                    _state.update {
                        it.copy(
                            playbackState = PlaybackState.STOPPED,
                            durationPlayed = Duration.ZERO
                        )
                    }
                    durationJob?.cancel()
                    durationJob = null
                }
            )
            durationJob = audioPlayer
                .activeTrack
                .filterNotNull()
                .onEach { track ->
                    _state.update {
                        it.copy(
                            playbackState = if(track.isPlaying) PlaybackState.PLAYING else PlaybackState.PAUSED,
                            durationPlayed = track.durationPlayed
                        )
                    }

                }
                .launchIn(viewModelScope)

        }
    }

    private fun onTrackSizeAvailable(trackSizeInfo: TrackSizeInfo) {
        viewModelScope.launch {
            val finalAmplitudes = AmplitudeNormalizer.normalize(
                sourceAmplitudes = recordingDetails.amplitudes,
                trackWidth = trackSizeInfo.trackWidth,
                barWidth = trackSizeInfo.barWidth,
                spacing = trackSizeInfo.spacing
            )
            _state.update {
                it.copy(
                    playbackAmplitudes = finalAmplitudes
                )
            }
        }
    }

    private fun onTitleTextChange(text: String) {
        _state.update {
            it.copy(
                titleText = text,
                canSaveEcho = text.isNotBlank() && it.mood != null
            )
        }
    }

    private fun onSaveClick() {
        if(recordingDetails.filePath == null && !state.value.canSaveEcho){
            return
        }
        viewModelScope.launch {
            val savedFilePath = recordingStorage.savePersistently(
                tempFilePath = recordingDetails.filePath?:""
            )
            if(savedFilePath == null){
                eventChannel.send(CreateEchoEvent.FailedToSaveFile)
                return@launch
            }
            val currentState =state.value
            val echo = Echo(
                mood = currentState.mood?.let {
                    Mood.valueOf(it.name)
                } ?: throw IllegalStateException("Mood must set before saving echo"),
                title = currentState.titleText.trim(),
                note = currentState.noteText.ifBlank { null },
                topics = currentState.topics,
                audioFilePath = savedFilePath,
                audioPlaybackLength = currentState.playbackTotalDuration,
                audioAmplitudes = recordingDetails.amplitudes,
                recordedAt = Instant.now()
            )
            echoDataSource.insertEcho(echo)
            eventChannel.send(CreateEchoEvent.EchoSuccessfullySaved)
        }
    }

    private fun onConfirmLeaveDialog() {
        _state.update {
            it.copy(
                showConfirmLeaveDialog = true
            )
        }
    }

    private fun onDismissConfirmLeaveMessage() {
        _state.update {
            it.copy(
                showConfirmLeaveDialog = false
            )
        }
    }

    @OptIn(FlowPreview::class)
    private fun observeAddTopicText() {
        state
            .map{
                it.addTopicText
            }
            .distinctUntilChanged()
            .debounce (300.milliseconds)
            .onEach { query ->
                _state.update {
                    it.copy(
                        showTopicSuggestions = query.isNotBlank() && query.trim() !in it.topics,
                        searchResults = listOf(
                            "Hello",
                            "Helloworld"
                        ).asUnselectedItems()
                    )
                }
            }
            .launchIn(viewModelScope)
    }
    private fun onDismissTopicSuggestions() {
        _state.update {
            it.copy(
                showTopicSuggestions = false
            )
        }
    }

    private fun onRemoveTopicClick(topic: String) {
        _state.update {
            it.copy(
                topics= it.topics - topic
            )
        }
    }

    private fun onTopicClick(topic:String){
        _state.update {
            it.copy(
                addTopicText = "",
                topics = (it.topics + topic).distinct()
            )
        }
    }
    private fun onAddTopicTextChange(text: String) {
        _state.update { state->
            state.copy(
                addTopicText = text
                    .filter { it.isLetterOrDigit() }
            )
        }
    }

    private fun onConfirmMood(){
        _state.update {
            it.copy(
                mood = it.selectedMood,
                canSaveEcho = it.titleText.isNotBlank(),
                showMoodSelector = false
            )
        }
    }
    private fun onDismissMoodSelector() {
        _state.update {
            it.copy(showMoodSelector = false)
        }
    }

    fun onSelectMoodClick(){
        _state.update {
            it.copy(
                showMoodSelector = true
            )
        }
    }
    fun onMoodClick(mood: MoodUi){
        _state.update {
            it.copy(selectedMood = mood)
        }
    }
}