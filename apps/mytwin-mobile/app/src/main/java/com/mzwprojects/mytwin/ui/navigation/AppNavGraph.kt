package com.mzwprojects.mytwin.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.mzwprojects.mytwin.ui.screens.ChatScreen
import com.mzwprojects.mytwin.ui.screens.FoodScanScreen
import com.mzwprojects.mytwin.ui.screens.HomeScreen
import com.mzwprojects.mytwin.ui.screens.OnboardingScreen
import com.mzwprojects.mytwin.ui.screens.SimulationScreen
import com.mzwprojects.mytwin.ui.screens.WelcomeScreen

object Routes {
    const val WELCOME = "welcome"
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val CHAT = "chat"
    const val FOOD_SCAN = "food_scan"
    const val SIMULATION = "simulation"
}

@Composable
fun AppNavGraph(
    startDestination: String,
    navController: NavHostController = rememberNavController(),
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
    ) {
        composable(Routes.WELCOME) {
            WelcomeScreen(
                onFinished = {
                    navController.navigate(Routes.ONBOARDING) {
                        popUpTo(Routes.WELCOME) { inclusive = true }
                    }
                },
            )
        }

        composable(Routes.ONBOARDING) {
            OnboardingScreen(
                onCompleted = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.ONBOARDING) { inclusive = true }
                    }
                },
            )
        }

        composable(Routes.HOME) {
            HomeScreen(
                onChatClicked = { navController.navigate(Routes.CHAT) },
                onFoodScanClicked = { navController.navigate(Routes.FOOD_SCAN) },
                onSimulationClicked = { navController.navigate(Routes.SIMULATION) },
            )
        }

        composable(Routes.CHAT) {
            ChatScreen(
                onBack = { navController.popBackStack() },
            )
        }

        composable(Routes.FOOD_SCAN) {
            FoodScanScreen(
                onBack = { navController.popBackStack() },
            )
        }

        composable(Routes.SIMULATION) {
            SimulationScreen(
                onBack = { navController.popBackStack() },
            )
        }
    }
}
