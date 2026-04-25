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
import com.mzwprojects.mytwin.data.model.BiologicalSex
import com.mzwprojects.mytwin.data.model.DietQuality
import com.mzwprojects.mytwin.data.model.SmokingStatus
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

    // When the app returns to foreground (e.g. after Samsung Health permission dialog),
    // re-check whether permissions were granted.
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
            // ── Progress bar ─────────────────────────────────────────────────
            LinearProgressIndicator(
                progress = { (state.currentStep.ordinal + 1f) / OnboardingStep.entries.size },
                modifier = Modifier.fillMaxWidth(),
            )

            // ── Step counter ─────────────────────────────────────────────────
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

            // ── Animated step content ────────────────────────────────────────
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
                                context.findActivity()?.let { vm.requestPermissions(it) }
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
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

// ─── Step: Profile ───────────────────────────────────────────────────────────

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
        modifier = Modifier.fillMaxWidth().height(56.dp),
        shape = MaterialTheme.shapes.extraLarge,
    ) { Text(stringResource(R.string.action_next)) }
}

// ─── Step: Permissions ───────────────────────────────────────────────────────

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
        !state.isPermissionsChecked -> {
            Box(
                modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }
        }
        !state.isSamsungHealthConnected -> {
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.onboarding_samsung_not_connected),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(20.dp),
                )
            }
            OutlinedButton(
                onClick = onNext,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = MaterialTheme.shapes.extraLarge,
            ) { Text(stringResource(R.string.onboarding_permissions_skip)) }
        }
        state.allPermissionsGranted -> {
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.onboarding_permissions_granted),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(20.dp),
                )
            }
            Button(
                onClick = onNext,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = MaterialTheme.shapes.extraLarge,
            ) { Text(stringResource(R.string.action_continue)) }
        }
        else -> {
            Button(
                onClick = onGrant,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = MaterialTheme.shapes.extraLarge,
            ) { Text(stringResource(R.string.onboarding_permissions_grant)) }
            OutlinedButton(
                onClick = onNext,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = MaterialTheme.shapes.extraLarge,
            ) { Text(stringResource(R.string.onboarding_permissions_skip)) }
            Text(
                text = stringResource(R.string.onboarding_hc_samsung_hint),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }
    }

    Spacer(Modifier.height(4.dp))
    OutlinedButton(
        onClick = onBack,
        modifier = Modifier.fillMaxWidth().height(48.dp),
        shape = MaterialTheme.shapes.extraLarge,
    ) { Text(stringResource(R.string.action_back)) }
}

// ─── Step: Manual Data ───────────────────────────────────────────────────────

@Composable
private fun ManualDataStep(state: OnboardingUiState, vm: OnboardingViewModel) {
    StepHeader(
        title = stringResource(R.string.onboarding_manual_title),
        subtitle = stringResource(R.string.onboarding_manual_subtitle),
    )
    if (state.allWearableCovered) {
        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.onboarding_manual_all_covered),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(20.dp),
            )
        }
    } else {
        if (state.needsSleepInput) {
            SliderField(
                label = stringResource(R.string.onboarding_sleep_label),
                value = state.sleepHours,
                valueText = "%.1f %s".format(state.sleepHours, stringResource(R.string.unit_hours_night)),
                range = 4f..12f,
                steps = 15,
                onValueChange = vm::setSleepHours,
            )
        }
        if (state.needsStepsInput) {
            SliderField(
                label = stringResource(R.string.onboarding_steps_label),
                value = state.dailySteps.toFloat(),
                valueText = "%,d %s".format(state.dailySteps, stringResource(R.string.unit_steps_day)),
                range = 1000f..25000f,
                steps = 47,
                onValueChange = { vm.setDailySteps(it.roundToInt()) },
            )
        }
    }
    SliderField(
        label = stringResource(R.string.onboarding_stress_label),
        value = state.stressLevel.toFloat(),
        valueText = "${state.stressLevel} ${stringResource(R.string.unit_out_of_ten)}",
        range = 1f..10f,
        steps = 8,
        onValueChange = { vm.setStressLevel(it.roundToInt()) },
        startLabel = stringResource(R.string.onboarding_stress_low),
        endLabel = stringResource(R.string.onboarding_stress_high),
    )
    Spacer(Modifier.height(8.dp))
    StepNavButtons(onBack = vm::back, onNext = vm::advance)
}

// ─── Step: Habits ────────────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HabitsStep(state: OnboardingUiState, vm: OnboardingViewModel) {
    StepHeader(
        title = stringResource(R.string.onboarding_habits_title),
        subtitle = stringResource(R.string.onboarding_habits_subtitle),
    )
    LabelledGroup(stringResource(R.string.onboarding_smoking_label)) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SmokingStatus.entries.forEach { s ->
                FilterChip(
                    selected = state.smokingStatus == s,
                    onClick = { vm.setSmokingStatus(s) },
                    label = { Text(s.label()) },
                )
            }
        }
    }
    SliderField(
        label = stringResource(R.string.onboarding_alcohol_label),
        value = state.alcoholDrinksPerWeek.toFloat(),
        valueText = "${state.alcoholDrinksPerWeek} ${stringResource(R.string.unit_drinks_week)}",
        range = 0f..21f,
        steps = 20,
        onValueChange = { vm.setAlcohol(it.roundToInt()) },
    )
    LabelledGroup(stringResource(R.string.onboarding_diet_label)) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DietQuality.entries.forEach { d ->
                FilterChip(
                    selected = state.dietQuality == d,
                    onClick = { vm.setDietQuality(d) },
                    label = { Text(d.label()) },
                )
            }
        }
    }
    Spacer(Modifier.height(8.dp))
    StepNavButtons(onBack = vm::back, onNext = vm::advance)
}

// ─── Step: Review ────────────────────────────────────────────────────────────

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
            ReviewRow(stringResource(R.string.review_name),
                state.displayName.ifBlank { stringResource(R.string.review_not_set) })
            ReviewRow(stringResource(R.string.review_age),
                state.ageInput.let { if (it.isBlank()) "—" else "$it ${stringResource(R.string.unit_years)}" })
            ReviewRow(stringResource(R.string.review_sex),
                state.biologicalSex?.label() ?: "—")
            ReviewRow(stringResource(R.string.review_height),
                state.heightInput.let { if (it.isBlank()) "—" else "$it ${stringResource(R.string.unit_cm)}" })
            ReviewRow(stringResource(R.string.review_weight),
                state.weightInput.let { if (it.isBlank()) "—" else "$it ${stringResource(R.string.unit_kg)}" })
            if (state.needsSleepInput) {
                ReviewRow(stringResource(R.string.review_sleep),
                    "%.1f ${stringResource(R.string.unit_hours_night)}".format(state.sleepHours))
            }
            if (state.needsStepsInput) {
                ReviewRow(stringResource(R.string.review_steps),
                    "%,d ${stringResource(R.string.unit_steps_day)}".format(state.dailySteps))
            }
            ReviewRow(stringResource(R.string.review_stress), "${state.stressLevel}/10")
            ReviewRow(stringResource(R.string.review_smoking), state.smokingStatus.label())
            ReviewRow(stringResource(R.string.review_alcohol),
                "${state.alcoholDrinksPerWeek} ${stringResource(R.string.unit_drinks_week)}")
            ReviewRow(stringResource(R.string.review_diet), state.dietQuality.label())
        }
    }
    Spacer(Modifier.height(8.dp))
    Button(
        onClick = onSubmit,
        enabled = !state.isSubmitting,
        modifier = Modifier.fillMaxWidth().height(56.dp),
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        if (state.isSubmitting) {
            CircularProgressIndicator(modifier = Modifier.height(20.dp).width(20.dp), strokeWidth = 2.dp)
        } else {
            Text(stringResource(R.string.onboarding_review_cta))
        }
    }
    OutlinedButton(
        onClick = onBack,
        modifier = Modifier.fillMaxWidth().height(48.dp),
        shape = MaterialTheme.shapes.extraLarge,
    ) { Text(stringResource(R.string.action_back)) }
}

// ─── Shared composables ───────────────────────────────────────────────────────

@Composable
private fun StepHeader(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = title, style = MaterialTheme.typography.headlineSmall)
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
        Text(label, style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
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
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(valueText, style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary)
        }
        Slider(value = value, onValueChange = onValueChange, valueRange = range, steps = steps)
        if (startLabel != null && endLabel != null) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(startLabel, style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(endLabel, style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun ReviewRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.End)
    }
}

@Composable
private fun StepNavButtons(onBack: () -> Unit, onNext: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f).height(56.dp),
            shape = MaterialTheme.shapes.extraLarge) { Text(stringResource(R.string.action_back)) }
        Button(onClick = onNext, modifier = Modifier.weight(1f).height(56.dp),
            shape = MaterialTheme.shapes.extraLarge) { Text(stringResource(R.string.action_next)) }
    }
}

// ─── Enum label helpers ───────────────────────────────────────────────────────

@Composable
private fun BiologicalSex.label(): String = stringResource(when (this) {
    BiologicalSex.MALE -> R.string.sex_male
    BiologicalSex.FEMALE -> R.string.sex_female
    BiologicalSex.OTHER -> R.string.sex_other
    BiologicalSex.PREFER_NOT_TO_SAY -> R.string.sex_prefer_not
})

@Composable
private fun SmokingStatus.label(): String = stringResource(when (this) {
    SmokingStatus.NEVER -> R.string.smoking_never
    SmokingStatus.FORMER -> R.string.smoking_former
    SmokingStatus.OCCASIONAL -> R.string.smoking_occasional
    SmokingStatus.REGULAR -> R.string.smoking_regular
})

@Composable
private fun DietQuality.label(): String = stringResource(when (this) {
    DietQuality.POOR -> R.string.diet_poor
    DietQuality.AVERAGE -> R.string.diet_average
    DietQuality.GOOD -> R.string.diet_good
    DietQuality.EXCELLENT -> R.string.diet_excellent
})
