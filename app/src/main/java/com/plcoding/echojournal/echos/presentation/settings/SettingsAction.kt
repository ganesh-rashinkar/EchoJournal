package com.plcoding.echojournal.echos.presentation.settings

import com.plcoding.echojournal.echos.presentation.models.MoodUi

sealed interface SettingsAction {
    data class OnSearchTextChange(val text:String): SettingsAction
    data class OnSelectTopicClick(val topic: String): SettingsAction
    data class OnRemoveTopicClick(val topic:String): SettingsAction
    data object OnBackClick: SettingsAction
    data object OnDismissTopicDropdown: SettingsAction
    data object OnAddButtonClick: SettingsAction
    data class OnMoodClick(val mood: MoodUi): SettingsAction
}