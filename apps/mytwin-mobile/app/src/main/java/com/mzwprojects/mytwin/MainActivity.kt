package com.mzwprojects.mytwin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mzwprojects.mytwin.ui.navigation.AppNavGraph
import com.mzwprojects.mytwin.ui.navigation.Routes
import com.mzwprojects.mytwin.ui.theme.MyTwinTheme
import com.mzwprojects.mytwin.ui.viewmodels.WelcomeViewModel

class MainActivity : ComponentActivity() {

    private val welcomeViewModel: WelcomeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyTwinTheme {
                val onboardingCompleted by welcomeViewModel.onboardingCompleted
                    .collectAsStateWithLifecycle()

                when (onboardingCompleted) {
                    // DataStore lädt noch — leerer Background, kein Flicker
                    null  -> Box(
                        Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background)
                    )

                    // Entscheidung bekannt → NavGraph mit korrekter Startroute
                    else  -> AppNavGraph(
                        startDestination = if (onboardingCompleted == true)
                            Routes.HOME
                        else
                            Routes.WELCOME,
                    )
                }
            }
        }
    }
}