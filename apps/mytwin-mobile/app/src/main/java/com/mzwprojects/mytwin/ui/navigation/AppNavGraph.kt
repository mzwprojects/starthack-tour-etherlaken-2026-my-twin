package com.mzwprojects.mytwin.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.mzwprojects.mytwin.ui.screens.WelcomeScreen

// Routen als typsichere Konstanten
object Routes {
    const val WELCOME = "welcome"
    const val HOME    = "home"
    // Weitere Routen hier ergänzen
}

@Composable
fun AppNavGraph(
    startDestination: String,
    navController: NavHostController = rememberNavController(),
) {
    NavHost(
        navController    = navController,
        startDestination = startDestination,
    ) {
        composable(Routes.WELCOME) {
            WelcomeScreen(
                onFinished = {
                    navController.navigate(Routes.HOME) {
                        // Welcome aus dem Backstack entfernen —
                        // Back-Button soll die App schliessen, nicht zurück zum Onboarding
                        popUpTo(Routes.WELCOME) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.HOME) {
            // Placeholder — hier kommt dein HomeScreen rein
        }
    }
}