package com.mzwprojects.mytwin.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mzwprojects.mytwin.R

@Composable
fun ChatScreen(onBack: () -> Unit) {
    val primary = MaterialTheme.colorScheme.primary

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = primary.copy(alpha = 0.06f),
                radius = size.width * 0.9f,
                center = Offset(size.width * 0.15f, -size.width * 0.1f),
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            IconButton(
                onClick = onBack,

                modifier = Modifier.padding(8.dp),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = stringResource(R.string.chat_back),
                    tint = MaterialTheme.colorScheme.onBackground,
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                // Dormant orb — mirrors WelcomeScreen canvas language
                Canvas(modifier = Modifier.size(140.dp)) {
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    drawCircle(
                        color = primary.copy(alpha = 0.06f),
                        radius = size.minDimension * 0.46f,
                        center = Offset(cx, cy),
                    )
                    drawCircle(
                        color = primary.copy(alpha = 0.14f),
                        radius = size.minDimension * 0.30f,
                        center = Offset(cx, cy),
                    )
                    drawCircle(
                        color = primary.copy(alpha = 0.60f),
                        radius = size.minDimension * 0.18f,
                        center = Offset(cx, cy),
                        style = Stroke(2.dp.toPx()),
                    )
                }

                Spacer(Modifier.height(32.dp))

                Text(
                    text = stringResource(R.string.chat_coming_soon_eyebrow),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.chat_coming_soon_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.chat_coming_soon_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )

                Spacer(Modifier.fillMaxWidth().height(80.dp))
            }
        }
    }
}
