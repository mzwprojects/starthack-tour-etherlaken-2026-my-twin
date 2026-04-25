package com.mzwprojects.mytwin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mzwprojects.mytwin.ui.navigation.AppNavGraph
import com.mzwprojects.mytwin.ui.navigation.Routes
import com.mzwprojects.mytwin.ui.theme.MyTwinTheme
import com.mzwprojects.mytwin.ui.viewmodels.RootViewModel

class MainActivity : ComponentActivity() {

    private val rootViewModel: RootViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        // Must be installed BEFORE super.onCreate() per Splash Screen API contract.
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)

        // Hold the splash until DataStore tells us whether onboarding is done.
        splash.setKeepOnScreenCondition {
            rootViewModel.onboardingCompleted.value == null
        }

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )

        setContent {
            MyTwinTheme {
                val onboardingCompleted by rootViewModel.onboardingCompleted
                    .collectAsStateWithLifecycle()

                // Once non-null, route to the correct start destination.
                onboardingCompleted?.let { completed ->
                    AppNavGraph(
                        startDestination = if (completed) Routes.HOME else Routes.WELCOME,
                    )
                }
            }
        }
    }
}