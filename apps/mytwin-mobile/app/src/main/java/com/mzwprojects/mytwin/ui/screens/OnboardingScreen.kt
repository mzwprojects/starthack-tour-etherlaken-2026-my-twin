package com.mzwprojects.mytwin.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mzwprojects.mytwin.R
import com.mzwprojects.mytwin.data.datasource.SamsungHealthMetric
import com.mzwprojects.mytwin.data.model.BiologicalSex
import com.mzwprojects.mytwin.data.model.DietQuality
import com.mzwprojects.mytwin.data.model.SmokingStatus
import com.mzwprojects.mytwin.data.model.WearableSignal
import com.mzwprojects.mytwin.ui.viewmodels.OnboardingStep
import com.mzwprojects.mytwin.ui.viewmodels.OnboardingUiState
import com.mzwprojects.mytwin.ui.viewmodels.OnboardingViewModel
import kotlin.math.roundToInt

@Composable
fun OnboardingScreen(
    onCompleted: () -> Unit,
    vm: OnboardingViewModel = viewModel(),
) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    val primary = MaterialTheme.colorScheme.primary
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) vm.refreshPermissions()
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
                color = primary.copy(alpha = 0.05f),
                radius = size.width * 0.8f,
                center = Offset(size.width * 0.5f, -size.width * 0.2f),
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding(),
        ) {
            LinearProgressIndicator(
                progress = { (state.currentStep.ordinal + 1f) / OnboardingStep.entries.size },
                modifier = Modifier.fillMaxWidth(),
            )

            Text(
                text = stringResource(
                    R.string.onboarding_step_counter,
                    state.currentStep.ordinal + 1,
                    OnboardingStep.entries.size,
                ),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(horizontal = 24.dp, vertical = 8.dp),
            )

            AnimatedContent(
                targetState = state.currentStep,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                modifier = Modifier.weight(1f),
                label = "onboarding_step",
            ) { step ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    when (step) {
                        OnboardingStep.PROFILE -> ProfileStep(state, vm)
                        OnboardingStep.PERMISSIONS -> PermissionsStep(
                            state = state,
                            onGrant = {
                                context.findActivity()?.let(vm::requestPermissions)
                            },
                            onBack = vm::back,
                            onNext = vm::advance,
                        )
                        OnboardingStep.MANUAL_DATA -> ManualDataStep(state, vm)
                        OnboardingStep.HABITS -> HabitsStep(state, vm)
                        OnboardingStep.REVIEW -> ReviewStep(
                            state = state,
                            onBack = vm::back,
                            onSubmit = { vm.submit(onCompleted) },
                        )
                    }
                }
            }
        }
    }
}

private fun Context.findActivity(): Activity? {
    var current = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProfileStep(state: OnboardingUiState, vm: OnboardingViewModel) {
    StepHeader(
        title = stringResource(R.string.onboarding_profile_title),
        subtitle = stringResource(R.string.onboarding_profile_subtitle),
    )
    OutlinedTextField(
        value = state.displayName,
        onValueChange = vm::setDisplayName,
        label = { Text(stringResource(R.string.onboarding_name_label)) },
        placeholder = { Text(stringResource(R.string.onboarding_name_hint)) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
    )
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = state.ageInput,
            onValueChange = vm::setAge,
            label = { Text(stringResource(R.string.onboarding_age_label)) },
            suffix = { Text(stringResource(R.string.unit_years)) },
            modifier = Modifier.weight(1f),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            isError = state.ageInput.isNotBlank() && state.ageInput.toIntOrNull() == null,
        )
        OutlinedTextField(
            value = state.heightInput,
            onValueChange = vm::setHeight,
            label = { Text(stringResource(R.string.onboarding_height_label)) },
            suffix = { Text(stringResource(R.string.unit_cm)) },
            modifier = Modifier.weight(1f),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            isError = state.heightInput.isNotBlank() && state.heightInput.toIntOrNull() == null,
        )
    }
    OutlinedTextField(
        value = state.weightInput,
        onValueChange = vm::setWeight,
        label = { Text(stringResource(R.string.onboarding_weight_label)) },
        suffix = { Text(stringResource(R.string.unit_kg)) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        isError = state.weightInput.isNotBlank() && state.weightInput.toFloatOrNull() == null,
    )
    Text(
        text = stringResource(R.string.onboarding_sex_label),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        BiologicalSex.entries.forEach { sex ->
            FilterChip(
                selected = state.biologicalSex == sex,
                onClick = { vm.setBiologicalSex(sex) },
                label = { Text(sex.label()) },
            )
        }
    }
    Spacer(Modifier.height(8.dp))
    Button(
        onClick = vm::advance,
        enabled = state.isProfileValid,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Text(stringResource(R.string.action_next))
    }
}

@Composable
private fun PermissionsStep(
    state: OnboardingUiState,
    onGrant: () -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit,
) {
    StepHeader(
        title = stringResource(R.string.onboarding_permissions_title),
        subtitle = stringResource(R.string.onboarding_permissions_subtitle),
    )

    when {
        !state.isPermissionsChecked || state.isCheckingHealthState -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        }

        !state.isSamsungHealthAvailable -> {
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.onboarding_samsung_not_connected),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(20.dp),
                )
            }
            Button(
                onClick = onGrant,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = MaterialTheme.shapes.extraLarge,
            ) {
                Text(stringResource(R.string.onboarding_permissions_retry))
            }
            OutlinedButton(
                onClick = onNext,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = MaterialTheme.shapes.extraLarge,
            ) {
                Text(stringResource(R.string.onboarding_permissions_skip))
            }
        }

        state.samsungPolicyBlocked -> {
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = stringResource(R.string.onboarding_samsung_policy_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = stringResource(R.string.onboarding_samsung_policy_body),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            OutlinedButton(
                onClick = onNext,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = MaterialTheme.shapes.extraLarge,
            ) {
                Text(stringResource(R.string.onboarding_permissions_skip))
            }
        }

        else -> {
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = if (state.allPermissionsGranted) {
                            stringResource(R.string.onboarding_permissions_granted)
                        } else {
                            stringResource(R.string.onboarding_permissions_missing)
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    PermissionStatusRow(
                        label = stringResource(R.string.onboarding_metric_sleep),
                        signal = state.metricSignals[SamsungHealthMetric.SLEEP],
                        isGranted = SamsungHealthMetric.SLEEP in state.grantedMetrics,
                    )
                    PermissionStatusRow(
                        label = stringResource(R.string.onboarding_metric_steps),
                        signal = state.metricSignals[SamsungHealthMetric.STEPS],
                        isGranted = SamsungHealthMetric.STEPS in state.grantedMetrics,
                    )
                    PermissionStatusRow(
                        label = stringResource(R.string.onboarding_metric_heart_rate),
                        signal = state.metricSignals[SamsungHealthMetric.HEART_RATE],
                        isGranted = SamsungHealthMetric.HEART_RATE in state.grantedMetrics,
                    )
                    PermissionStatusRow(
                        label = stringResource(R.string.onboarding_metric_stress),
                        signal = state.metricSignals[SamsungHealthMetric.STRESS],
                        isGranted = SamsungHealthMetric.STRESS in state.grantedMetrics,
                    )
                }
            }

            if (!state.allPermissionsGranted) {
                Button(
                    onClick = onGrant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = MaterialTheme.shapes.extraLarge,
                ) {
                    Text(stringResource(R.string.onboarding_permissions_grant))
                }
            }

            OutlinedButton(
                onClick = onNext,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = MaterialTheme.shapes.extraLarge,
            ) {
                Text(
                    stringResource(
                        if (state.allPermissionsGranted) {
                            R.string.action_continue
                        } else {
                            R.string.onboarding_permissions_skip
                        },
                    ),
                )
            }
        }
    }

    OutlinedButton(
        onClick = onBack,
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Text(stringResource(R.string.action_back))
    }
}

@Composable
private fun PermissionStatusRow(
    label: String,
    signal: WearableSignal?,
    isGranted: Boolean,
) {
    ReviewRow(
        label = label,
        value = when {
            !isGranted -> stringResource(R.string.onboarding_metric_status_permission_needed)
            signal == WearableSignal.ACTIVE -> stringResource(R.string.onboarding_metric_status_active)
            signal == WearableSignal.DEVICE_PRESENT_NOT_WORN_RECENTLY ->
                stringResource(R.string.onboarding_metric_status_stale)
            signal == WearableSignal.NO_DEVICE_LIKELY ->
                stringResource(R.string.onboarding_metric_status_no_data)
            else -> stringResource(R.string.onboarding_metric_status_unknown)
        },
    )
}

@Composable
private fun ManualDataStep(state: OnboardingUiState, vm: OnboardingViewModel) {
    StepHeader(
        title = stringResource(R.string.onboarding_manual_title),
        subtitle = stringResource(R.string.onboarding_manual_subtitle),
    )
    if (!state.needsSleepInput && !state.needsStepsInput) {
        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = stringResource(R.string.onboarding_manual_all_covered),
                    style = MaterialTheme.typography.bodyLarge,
                )
                state.representativeSleepHours?.let {
                    ReviewRow(
                        label = stringResource(R.string.onboarding_metric_sleep),
                        value = stringResource(
                            R.string.onboarding_sleep_value,
                            it,
                            stringResource(R.string.unit_hours_night),
                        ),
                    )
                }
                state.representativeDailySteps?.let {
                    ReviewRow(
                        label = stringResource(R.string.onboarding_metric_steps),
                        value = stringResource(
                            R.string.onboarding_steps_value,
                            it,
                            stringResource(R.string.unit_steps_day),
                        ),
                    )
                }
            }
        }
    } else {
        if (state.needsSleepInput) {
            SliderField(
                label = stringResource(R.string.onboarding_sleep_label),
                value = state.sleepHours,
                valueText = stringResource(
                    R.string.onboarding_sleep_value,
                    state.sleepHours,
                    stringResource(R.string.unit_hours_night),
                ),
                range = 4f..12f,
                steps = 15,
                onValueChange = vm::setSleepHours,
            )
        }
        if (state.needsStepsInput) {
            SliderField(
                label = stringResource(R.string.onboarding_steps_label),
                value = state.dailySteps.toFloat(),
                valueText = stringResource(
                    R.string.onboarding_steps_value,
                    state.dailySteps,
                    stringResource(R.string.unit_steps_day),
                ),
                range = 1000f..25000f,
                steps = 47,
                onValueChange = { vm.setDailySteps(it.roundToInt()) },
            )
        }
    }

    SliderField(
        label = stringResource(R.string.onboarding_stress_label),
        value = state.stressLevel.toFloat(),
        valueText = stringResource(R.string.onboarding_stress_value, state.stressLevel),
        range = 1f..10f,
        steps = 8,
        onValueChange = { vm.setStressLevel(it.roundToInt()) },
        startLabel = stringResource(R.string.onboarding_stress_low),
        endLabel = stringResource(R.string.onboarding_stress_high),
    )
    if (state.wearableStressLevel != null) {
        Text(
            text = stringResource(R.string.onboarding_stress_proxy_note, state.wearableStressLevel),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    Spacer(Modifier.height(8.dp))
    StepNavButtons(onBack = vm::back, onNext = vm::advance)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HabitsStep(state: OnboardingUiState, vm: OnboardingViewModel) {
    StepHeader(
        title = stringResource(R.string.onboarding_habits_title),
        subtitle = stringResource(R.string.onboarding_habits_subtitle),
    )
    LabelledGroup(stringResource(R.string.onboarding_smoking_label)) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SmokingStatus.entries.forEach { smokingStatus ->
                FilterChip(
                    selected = state.smokingStatus == smokingStatus,
                    onClick = { vm.setSmokingStatus(smokingStatus) },
                    label = { Text(smokingStatus.label()) },
                )
            }
        }
    }
    SliderField(
        label = stringResource(R.string.onboarding_alcohol_label),
        value = state.alcoholDrinksPerWeek.toFloat(),
        valueText = stringResource(
            R.string.onboarding_alcohol_value,
            state.alcoholDrinksPerWeek,
            stringResource(R.string.unit_drinks_week),
        ),
        range = 0f..21f,
        steps = 20,
        onValueChange = { vm.setAlcohol(it.roundToInt()) },
    )
    LabelledGroup(stringResource(R.string.onboarding_diet_label)) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DietQuality.entries.forEach { dietQuality ->
                FilterChip(
                    selected = state.dietQuality == dietQuality,
                    onClick = { vm.setDietQuality(dietQuality) },
                    label = { Text(dietQuality.label()) },
                )
            }
        }
    }
    Spacer(Modifier.height(8.dp))
    StepNavButtons(onBack = vm::back, onNext = vm::advance)
}

@Composable
private fun ReviewStep(
    state: OnboardingUiState,
    onBack: () -> Unit,
    onSubmit: () -> Unit,
) {
    StepHeader(
        title = stringResource(R.string.onboarding_review_title),
        subtitle = stringResource(R.string.onboarding_review_subtitle),
    )
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            ReviewRow(
                label = stringResource(R.string.review_name),
                value = state.displayName.ifBlank { stringResource(R.string.review_not_set) },
            )
            ReviewRow(
                label = stringResource(R.string.review_age),
                value = state.ageInput.takeIf { it.isNotBlank() }
                    ?.let { "$it ${stringResource(R.string.unit_years)}" }
                    ?: stringResource(R.string.review_dash),
            )
            ReviewRow(
                label = stringResource(R.string.review_sex),
                value = state.biologicalSex?.label() ?: stringResource(R.string.review_dash),
            )
            ReviewRow(
                label = stringResource(R.string.review_height),
                value = state.heightInput.takeIf { it.isNotBlank() }
                    ?.let { "$it ${stringResource(R.string.unit_cm)}" }
                    ?: stringResource(R.string.review_dash),
            )
            ReviewRow(
                label = stringResource(R.string.review_weight),
                value = state.weightInput.takeIf { it.isNotBlank() }
                    ?.let { "$it ${stringResource(R.string.unit_kg)}" }
                    ?: stringResource(R.string.review_dash),
            )
            if (state.needsSleepInput) {
                ReviewRow(
                    label = stringResource(R.string.review_sleep),
                    value = stringResource(
                        R.string.onboarding_sleep_value,
                        state.sleepHours,
                        stringResource(R.string.unit_hours_night),
                    ),
                )
            }
            if (state.needsStepsInput) {
                ReviewRow(
                    label = stringResource(R.string.review_steps),
                    value = stringResource(
                        R.string.onboarding_steps_value,
                        state.dailySteps,
                        stringResource(R.string.unit_steps_day),
                    ),
                )
            }
            ReviewRow(
                label = stringResource(R.string.review_stress),
                value = stringResource(R.string.onboarding_stress_value, state.stressLevel),
            )
            ReviewRow(
                label = stringResource(R.string.review_smoking),
                value = state.smokingStatus.label(),
            )
            ReviewRow(
                label = stringResource(R.string.review_alcohol),
                value = stringResource(
                    R.string.onboarding_alcohol_value,
                    state.alcoholDrinksPerWeek,
                    stringResource(R.string.unit_drinks_week),
                ),
            )
            ReviewRow(
                label = stringResource(R.string.review_diet),
                value = state.dietQuality.label(),
            )
        }
    }
    Spacer(Modifier.height(8.dp))
    Button(
        onClick = onSubmit,
        enabled = !state.isSubmitting,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        if (state.isSubmitting) {
            CircularProgressIndicator(
                modifier = Modifier
                    .height(20.dp)
                    .width(20.dp),
                strokeWidth = 2.dp,
            )
        } else {
            Text(stringResource(R.string.onboarding_review_cta))
        }
    }
    OutlinedButton(
        onClick = onBack,
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Text(stringResource(R.string.action_back))
    }
}

@Composable
private fun StepHeader(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun LabelledGroup(label: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        content()
    }
}

@Composable
private fun SliderField(
    label: String,
    value: Float,
    valueText: String,
    range: ClosedFloatingPointRange<Float>,
    steps: Int,
    onValueChange: (Float) -> Unit,
    startLabel: String? = null,
    endLabel: String? = null,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = valueText,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            steps = steps,
        )
        if (startLabel != null && endLabel != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = startLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = endLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ReviewRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.End,
        )
    }
}

@Composable
private fun StepNavButtons(onBack: () -> Unit, onNext: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        OutlinedButton(
            onClick = onBack,
            modifier = Modifier
                .weight(1f)
                .height(56.dp),
            shape = MaterialTheme.shapes.extraLarge,
        ) {
            Text(stringResource(R.string.action_back))
        }
        Button(
            onClick = onNext,
            modifier = Modifier
                .weight(1f)
                .height(56.dp),
            shape = MaterialTheme.shapes.extraLarge,
        ) {
            Text(stringResource(R.string.action_next))
        }
    }
}

@Composable
private fun BiologicalSex.label(): String = stringResource(
    when (this) {
        BiologicalSex.MALE -> R.string.sex_male
        BiologicalSex.FEMALE -> R.string.sex_female
        BiologicalSex.OTHER -> R.string.sex_other
        BiologicalSex.PREFER_NOT_TO_SAY -> R.string.sex_prefer_not
    },
)

@Composable
private fun SmokingStatus.label(): String = stringResource(
    when (this) {
        SmokingStatus.NEVER -> R.string.smoking_never
        SmokingStatus.FORMER -> R.string.smoking_former
        SmokingStatus.OCCASIONAL -> R.string.smoking_occasional
        SmokingStatus.REGULAR -> R.string.smoking_regular
    },
)

@Composable
private fun DietQuality.label(): String = stringResource(
    when (this) {
        DietQuality.POOR -> R.string.diet_poor
        DietQuality.AVERAGE -> R.string.diet_average
        DietQuality.GOOD -> R.string.diet_good
        DietQuality.EXCELLENT -> R.string.diet_excellent
    },
)
