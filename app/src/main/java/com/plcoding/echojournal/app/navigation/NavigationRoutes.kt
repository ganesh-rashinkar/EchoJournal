package com.plcoding.echojournal.app.navigation

import kotlinx.serialization.Serializable

sealed interface NavigationRoutes {

    @Serializable
    data object Echos: NavigationRoutes

    @Serializable
    data class CreateEcho(
        val recordingPath:String,
        val duration:Long,
        val amplitudes:String
    ): NavigationRoutes

    @Serializable
    data object Settings
}