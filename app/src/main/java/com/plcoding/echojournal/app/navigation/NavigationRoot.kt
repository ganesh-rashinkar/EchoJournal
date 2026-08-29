package com.plcoding.echojournal.app.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.plcoding.echojournal.echos.presentation.create_echo.CreateEchoRoot
import com.plcoding.echojournal.echos.presentation.echos.EchosRoot
import com.plcoding.echojournal.echos.presentation.echos.EchosScreen
import com.plcoding.echojournal.echos.presentation.settings.SettingsRoot
import com.plcoding.echojournal.echos.presentation.util.toCreateEchoRoute

@Composable
fun NavigationRoot(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = NavigationRoutes.Echos
    ) {
        composable<NavigationRoutes.Echos> {
            EchosRoot(
                onNavigateToCreateEcho = {
                    details->
                    navController.navigate(details.toCreateEchoRoute())
                },
                onNavigateToSettings = {
                    navController.navigate(NavigationRoutes.Settings)
                }
            )
        }
        composable<NavigationRoutes.CreateEcho>{
            CreateEchoRoot(
                onConfirmLeave = navController::navigateUp
            )
        }
        composable<NavigationRoutes.Settings> {
            SettingsRoot(
                onGoBack = navController::navigateUp
            )
        }
    }
}