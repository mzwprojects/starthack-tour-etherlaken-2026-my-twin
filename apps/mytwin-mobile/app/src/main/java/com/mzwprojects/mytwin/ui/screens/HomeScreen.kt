package com.mzwprojects.mytwin.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mzwprojects.mytwin.R
import com.mzwprojects.mytwin.ui.viewmodels.GreetingTime
import com.mzwprojects.mytwin.ui.viewmodels.HomeUiState
import com.mzwprojects.mytwin.ui.viewmodels.HomeViewModel

@Composable
fun HomeScreen(
    onChatClicked: () -> Unit,
    vm: HomeViewModel = viewModel(),
) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    val primary = MaterialTheme.colorScheme.primary

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        // Ambient glow — mirrors WelcomeScreen's top-corner orb
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = primary.copy(alpha = 0.07f),
                radius = size.width * 1.0f,
                center = Offset(size.width * 0.85f, -size.width * 0.15f),
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
        ) {
            Spacer(Modifier.height(28.dp))

            // ── Greeting ─────────────────────────────────────────────────────
            Text(
                text = stringResource(R.string.home_today_eyebrow),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(8.dp))
            val greeting = when (state.greetingKey) {
                GreetingTime.MORNING -> stringResource(R.string.home_greeting_morning)
                GreetingTime.AFTERNOON -> stringResource(R.string.home_greeting_afternoon)
                GreetingTime.EVENING -> stringResource(R.string.home_greeting_evening)
            }
            val name = state.profile.displayName?.ifBlank { null }
            Text(
                text = if (name != null) "$greeting,\n$name" else greeting,
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )

            Spacer(Modifier.height(32.dp))

            // ── Snapshot section ─────────────────────────────────────────────
            Text(
                text = stringResource(R.string.home_snapshot_title),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(12.dp))
            MetricGrid(state)

            // ── Wearable nudge ────────────────────────────────────────────────
            if (state.showWearableNudge) {
                Spacer(Modifier.height(16.dp))
                OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.LocationOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                        )
                        Text(
                            text = stringResource(R.string.home_nudge_message),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            Spacer(Modifier.height(32.dp))

            // ── Chat CTA ─────────────────────────────────────────────────────
            Button(
                onClick = onChatClicked,
                modifier = Modifier.fillMaxWidth().height(60.dp),
                shape = MaterialTheme.shapes.extraLarge,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                ),
            ) {
                Text(
                    text = stringResource(R.string.home_chat_cta),
                    style = MaterialTheme.typography.titleMedium,
                )
            }

            Spacer(Modifier.height(16.dp))

            // ── Food scan placeholder ────────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                ),
                shape = MaterialTheme.shapes.large,
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.home_food_scan_label),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Box(
                        modifier = Modifier
                            .clip(MaterialTheme.shapes.small)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.home_food_coming_soon),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

// ─── Metric grid ──────────────────────────────────────────────────────────────

@Composable
private fun MetricGrid(state: HomeUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Rounded.Star,
                label = stringResource(R.string.home_sleep_label),
                value = state.sleepHours?.let { "%.1f h".format(it) }
                    ?: stringResource(R.string.home_value_unavailable),
                source = if (state.sleepHours != null) {
                    if (state.sleepIsFromWearable) stringResource(R.string.home_source_wearable)
                    else stringResource(R.string.home_source_manual)
                } else null,
            )
            MetricCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Rounded.PlayArrow,
                label = stringResource(R.string.home_steps_label),
                value = state.stepsToday?.let { "%,d".format(it) }
                    ?: stringResource(R.string.home_value_unavailable),
                source = if (state.stepsToday != null) {
                    if (state.stepsIsFromWearable) stringResource(R.string.home_source_wearable)
                    else stringResource(R.string.home_source_manual)
                } else null,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Rounded.FavoriteBorder,
                label = stringResource(R.string.home_heartrate_label),
                value = state.restingHR?.let { "$it bpm" }
                    ?: stringResource(R.string.home_value_unavailable),
                source = if (state.restingHR != null) stringResource(R.string.home_source_wearable) else null,
            )
            MetricCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Rounded.Warning,
                label = stringResource(R.string.home_stress_label),
                value = state.stressLevel?.let { "$it / 10" }
                    ?: stringResource(R.string.home_value_unavailable),
                source = if (state.stressLevel != null) stringResource(R.string.home_source_manual) else null,
            )
        }
    }
}

@Composable
private fun MetricCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    value: String,
    source: String?,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
        shape = MaterialTheme.shapes.large,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (source != null) {
                Text(
                    text = source,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary,
                    textAlign = TextAlign.Start,
                )
            }
        }
    }
}