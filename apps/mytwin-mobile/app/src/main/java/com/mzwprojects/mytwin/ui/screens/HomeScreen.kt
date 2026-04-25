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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mzwprojects.mytwin.R
import com.mzwprojects.mytwin.ui.viewmodels.GreetingTime
import com.mzwprojects.mytwin.ui.viewmodels.HomeUiState
import com.mzwprojects.mytwin.ui.viewmodels.HomeViewModel
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt

private enum class ManualMetricEditor {
    SLEEP,
    STEPS,
    HEART_RATE,
    STRESS,
}

@Composable
fun HomeScreen(
    onChatClicked: () -> Unit,
    onFoodScanClicked: () -> Unit,
    vm: HomeViewModel = viewModel(),
) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    val primary = MaterialTheme.colorScheme.primary
    val bpmUnit = stringResource(R.string.unit_bpm)
    var activeEditor by remember { mutableStateOf<ManualMetricEditor?>(null) }
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) vm.refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = primary.copy(alpha = 0.07f),
                radius = size.width,
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

            Text(
                text = if (state.isViewingToday) {
                    stringResource(R.string.home_today_eyebrow)
                } else {
                    stringResource(R.string.home_history_eyebrow)
                },
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

            Text(
                text = if (state.isViewingToday) {
                    stringResource(R.string.home_snapshot_title)
                } else {
                    stringResource(
                        R.string.home_snapshot_title_for_day,
                        state.selectedDate.toDisplayLabel(
                            todayLabel = stringResource(R.string.home_date_today),
                            yesterdayLabel = stringResource(R.string.home_date_yesterday),
                        ),
                    )
                },
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(12.dp))
            DateSelectorRow(
                selectedDate = state.selectedDate,
                dates = state.recentDates,
                onDateSelected = vm::selectDate,
                todayLabel = stringResource(R.string.home_date_today),
                yesterdayLabel = stringResource(R.string.home_date_yesterday),
            )
            Spacer(Modifier.height(12.dp))
            MetricGrid(state)

            if (state.showWearableNudge && state.isViewingToday) {
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

            if (state.isViewingToday) {
                Spacer(Modifier.height(24.dp))

                Text(
                    text = stringResource(R.string.home_manual_tracking_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.home_manual_tracking_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))

                ManualTrackingCard(
                    label = stringResource(R.string.home_sleep_label),
                    value = state.sleepHours?.let {
                        stringResource(R.string.home_sleep_value, it)
                    } ?: stringResource(R.string.home_value_unavailable),
                    description = if (state.sleepIsManualOverride) {
                        stringResource(R.string.home_manual_override_active)
                    } else {
                        stringResource(R.string.home_manual_sleep_description)
                    },
                    onEdit = { activeEditor = ManualMetricEditor.SLEEP },
                    onClear = if (state.hasSleepOverride) vm::clearManualSleepOverride else null,
                )
                Spacer(Modifier.height(12.dp))

                ManualTrackingCard(
                    label = stringResource(R.string.home_steps_label),
                    value = state.stepsToday?.let {
                        stringResource(R.string.home_steps_value, it)
                    } ?: stringResource(R.string.home_value_unavailable),
                    description = if (state.stepsIsManualOverride) {
                        stringResource(R.string.home_manual_override_active)
                    } else {
                        stringResource(R.string.home_manual_steps_description)
                    },
                    onEdit = { activeEditor = ManualMetricEditor.STEPS },
                    onClear = if (state.hasStepsOverride) vm::clearManualStepsOverride else null,
                )
                Spacer(Modifier.height(12.dp))

                ManualTrackingCard(
                    label = stringResource(R.string.home_heartrate_label),
                    value = state.restingHR?.let {
                        "$it ${stringResource(R.string.unit_bpm)}"
                    } ?: stringResource(R.string.home_value_unavailable),
                    description = if (state.heartRateIsManualOverride) {
                        stringResource(R.string.home_manual_override_active)
                    } else {
                        stringResource(R.string.home_manual_heartrate_description)
                    },
                    onEdit = { activeEditor = ManualMetricEditor.HEART_RATE },
                    onClear = if (state.hasHeartRateOverride) vm::clearManualHeartRateOverride else null,
                )
                Spacer(Modifier.height(12.dp))

                ManualTrackingCard(
                    label = stringResource(R.string.home_stress_label),
                    value = state.stressLevel?.let {
                        stringResource(R.string.home_stress_value, it)
                    } ?: stringResource(R.string.home_value_unavailable),
                    description = if (state.stressIsManualOverride) {
                        stringResource(R.string.home_manual_override_active)
                    } else {
                        stringResource(R.string.home_manual_stress_description)
                    },
                    onEdit = { activeEditor = ManualMetricEditor.STRESS },
                    onClear = if (state.hasStressOverride) vm::clearManualStressOverride else null,
                )
            } else {
                Spacer(Modifier.height(24.dp))
                OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(R.string.home_history_readonly_note),
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.height(32.dp))

            Button(
                onClick = onChatClicked,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
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

            Button(
                onClick = onFoodScanClicked,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                shape = MaterialTheme.shapes.extraLarge,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                ),
            ) {
                Text(
                    text = stringResource(R.string.home_food_scan_label),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(Modifier.width(16.dp))

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

            Spacer(Modifier.height(24.dp))
        }
    }

    when (activeEditor) {
        ManualMetricEditor.SLEEP -> ManualValueDialog(
            title = stringResource(R.string.home_manual_sleep_dialog_title),
            initialValue = state.sleepHours ?: 7f,
            range = 4f..12f,
            steps = 15,
            valueFormatter = { "%.1f h".format(it) },
            onDismiss = { activeEditor = null },
            onSave = {
                vm.updateManualSleep(it)
                activeEditor = null
            },
            onClear = if (state.hasSleepOverride) {
                {
                    vm.clearManualSleepOverride()
                    activeEditor = null
                }
            } else {
                null
            },
        )

        ManualMetricEditor.STEPS -> ManualValueDialog(
            title = stringResource(R.string.home_manual_steps_dialog_title),
            initialValue = (state.stepsToday ?: 8000).toFloat(),
            range = 1000f..25000f,
            steps = 47,
            valueFormatter = { "%,d steps".format(it.roundToInt()) },
            onDismiss = { activeEditor = null },
            onSave = {
                vm.updateManualSteps(it.roundToInt())
                activeEditor = null
            },
            onClear = if (state.hasStepsOverride) {
                {
                    vm.clearManualStepsOverride()
                    activeEditor = null
                }
            } else {
                null
            },
        )

        ManualMetricEditor.HEART_RATE -> ManualValueDialog(
            title = stringResource(R.string.home_manual_heartrate_dialog_title),
            initialValue = (state.restingHR ?: 80).toFloat(),
            range = 40f..200f,
            steps = 159,
            valueFormatter = { "${it.roundToInt()} $bpmUnit" },
            onDismiss = { activeEditor = null },
            onSave = {
                vm.updateManualHeartRate(it.roundToInt())
                activeEditor = null
            },
            onClear = if (state.hasHeartRateOverride) {
                {
                    vm.clearManualHeartRateOverride()
                    activeEditor = null
                }
            } else {
                null
            },
        )

        ManualMetricEditor.STRESS -> ManualValueDialog(
            title = stringResource(R.string.home_manual_stress_dialog_title),
            initialValue = (state.stressLevel ?: 5).toFloat(),
            range = 1f..10f,
            steps = 8,
            valueFormatter = { "${it.roundToInt()} / 10" },
            onDismiss = { activeEditor = null },
            onSave = {
                vm.updateManualStress(it.roundToInt())
                activeEditor = null
            },
            onClear = if (state.hasStressOverride) {
                {
                    vm.clearManualStressOverride()
                    activeEditor = null
                }
            } else {
                null
            },
        )

        null -> Unit
    }
}

@Composable
private fun DateSelectorRow(
    selectedDate: LocalDate,
    dates: List<LocalDate>,
    onDateSelected: (LocalDate) -> Unit,
    todayLabel: String,
    yesterdayLabel: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        dates.forEach { date ->
            val isSelected = date == selectedDate
            OutlinedButton(
                onClick = { onDateSelected(date) },
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (isSelected) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHigh
                    },
                ),
            ) {
                Text(
                    text = date.toDisplayLabel(todayLabel, yesterdayLabel),
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                )
            }
        }
    }
}

@Composable
private fun MetricGrid(state: HomeUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Rounded.Star,
                label = stringResource(R.string.home_sleep_label),
                value = state.sleepHours?.let { stringResource(R.string.home_sleep_value, it) }
                    ?: stringResource(R.string.home_value_unavailable),
                source = if (state.sleepHours != null) {
                    if (state.sleepIsManualOverride) {
                        stringResource(R.string.home_source_manual_override)
                    } else if (state.sleepIsFromWearable) {
                        stringResource(R.string.home_source_wearable)
                    } else {
                        stringResource(R.string.home_source_manual)
                    }
                } else {
                    null
                },
            )
            MetricCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Rounded.PlayArrow,
                label = stringResource(R.string.home_steps_label),
                value = state.stepsToday?.let { stringResource(R.string.home_steps_value, it) }
                    ?: stringResource(R.string.home_value_unavailable),
                source = if (state.stepsToday != null) {
                    if (state.stepsIsManualOverride) {
                        stringResource(R.string.home_source_manual_override)
                    } else if (state.stepsIsFromWearable) {
                        stringResource(R.string.home_source_wearable)
                    } else {
                        stringResource(R.string.home_source_manual)
                    }
                } else {
                    null
                },
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Rounded.FavoriteBorder,
                label = stringResource(R.string.home_heartrate_label),
                value = state.restingHR?.let { "$it ${stringResource(R.string.unit_bpm)}" }
                    ?: stringResource(R.string.home_value_unavailable),
                source = if (state.restingHR != null) {
                    if (state.heartRateIsManualOverride) {
                        stringResource(R.string.home_source_manual_override)
                    } else {
                        stringResource(R.string.home_source_wearable)
                    }
                } else {
                    null
                },
            )
            MetricCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Rounded.Warning,
                label = stringResource(R.string.home_stress_label),
                value = state.stressLevel?.let { stringResource(R.string.home_stress_value, it) }
                    ?: stringResource(R.string.home_value_unavailable),
                source = if (state.stressLevel != null) {
                    if (state.stressIsManualOverride) {
                        stringResource(R.string.home_source_manual_override)
                    } else if (state.stressIsFromWearable) {
                        stringResource(R.string.home_source_wearable)
                    } else {
                        stringResource(R.string.home_source_manual)
                    }
                } else {
                    null
                },
            )
        }
    }
}

private fun LocalDate.toDisplayLabel(todayLabel: String, yesterdayLabel: String): String {
    val today = LocalDate.now()
    return when (this) {
        today -> todayLabel
        today.minusDays(1) -> yesterdayLabel
        else -> dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
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

@Composable
private fun ManualTrackingCard(
    label: String,
    value: String,
    description: String,
    onEdit: () -> Unit,
    onClear: (() -> Unit)?,
) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(onClick = onEdit) {
                    Icon(
                        imageVector = Icons.Rounded.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.size(8.dp))
                    Text(text = stringResource(R.string.home_manual_update))
                }
                if (onClear != null) {
                    OutlinedButton(onClick = onClear) {
                        Text(text = stringResource(R.string.home_manual_clear))
                    }
                }
            }
        }
    }
}

@Composable
private fun ManualValueDialog(
    title: String,
    initialValue: Float,
    range: ClosedFloatingPointRange<Float>,
    steps: Int,
    valueFormatter: (Float) -> String,
    onDismiss: () -> Unit,
    onSave: (Float) -> Unit,
    onClear: (() -> Unit)? = null,
) {
    var value by remember(initialValue) { mutableFloatStateOf(initialValue) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = valueFormatter(value),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Slider(
                    value = value,
                    onValueChange = { value = it },
                    valueRange = range,
                    steps = steps,
                )
            }
        },
        confirmButton = {
            Button(onClick = { onSave(value) }) {
                Text(text = stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (onClear != null) {
                    OutlinedButton(onClick = onClear) {
                        Text(text = stringResource(R.string.home_manual_clear))
                    }
                }
                OutlinedButton(onClick = onDismiss) {
                    Text(text = stringResource(R.string.action_cancel))
                }
            }
        },
    )
}
