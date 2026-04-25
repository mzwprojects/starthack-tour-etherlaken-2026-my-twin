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
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mzwprojects.mytwin.R
import com.mzwprojects.mytwin.simulation.SimulationConfidence
import com.mzwprojects.mytwin.simulation.SimulationScenarioComparison
import com.mzwprojects.mytwin.ui.viewmodels.SimulationViewModel

@Composable
fun SimulationScreen(
    onBack: () -> Unit,
    vm: SimulationViewModel = viewModel(),
) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    val primary = MaterialTheme.colorScheme.primary

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = primary.copy(alpha = 0.06f),
                radius = size.width * 0.95f,
                center = Offset(size.width * 0.82f, -size.width * 0.12f),
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState()),
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.padding(8.dp),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = stringResource(R.string.simulation_back),
                    tint = MaterialTheme.colorScheme.onBackground,
                )
            }

            Column(
                modifier = Modifier.padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = stringResource(R.string.simulation_eyebrow),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = stringResource(R.string.simulation_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = stringResource(R.string.simulation_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                when {
                    state.isLoading -> {
                        Text(
                            text = stringResource(R.string.simulation_loading),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    state.errorMessage != null -> {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                            ),
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = stringResource(R.string.simulation_error_title),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = state.errorMessage ?: "",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                )
                                Spacer(Modifier.height(12.dp))
                                OutlinedButton(onClick = vm::refresh) {
                                    Text(text = stringResource(R.string.simulation_retry))
                                }
                            }
                        }
                    }

                    state.bundle != null -> {
                        val bundle = state.bundle ?: return@Column
                        BaselineSection(
                            headline = bundle.baseline.narrative.headline,
                            confidence = bundle.baseline.confidence,
                            recovery = bundle.baseline.scores.recovery,
                            energy = bundle.baseline.scores.energy,
                            focus = bundle.baseline.scores.focus,
                            strain = bundle.baseline.scores.strain,
                            longTermRisk = bundle.baseline.scores.longTermRisk,
                        )
                        NarrativeSection(
                            title = stringResource(R.string.simulation_short_term_title),
                            lines = bundle.baseline.narrative.shortTermInsights,
                        )
                        NarrativeSection(
                            title = stringResource(R.string.simulation_recommendations_title),
                            lines = bundle.baseline.narrative.recommendations,
                        )
                        NarrativeSection(
                            title = stringResource(R.string.simulation_long_term_title),
                            lines = bundle.baseline.narrative.longTermSignals,
                        )
                        Text(
                            text = stringResource(R.string.simulation_scenarios_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                        bundle.scenarioComparisons.forEach { comparison ->
                            ScenarioCard(comparison = comparison)
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun BaselineSection(
    headline: String,
    confidence: SimulationConfidence,
    recovery: Int,
    energy: Int,
    focus: Int,
    strain: Int,
    longTermRisk: Int,
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = headline,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = when (confidence) {
                    SimulationConfidence.HIGH -> "Confidence: high"
                    SimulationConfidence.MEDIUM -> "Confidence: medium"
                    SimulationConfidence.LOW -> "Confidence: low"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
            )
            ScoreRow(
                leftLabel = stringResource(R.string.simulation_score_recovery),
                leftValue = recovery,
                rightLabel = stringResource(R.string.simulation_score_energy),
                rightValue = energy,
            )
            ScoreRow(
                leftLabel = stringResource(R.string.simulation_score_focus),
                leftValue = focus,
                rightLabel = stringResource(R.string.simulation_score_strain),
                rightValue = strain,
            )
            ScoreRow(
                leftLabel = stringResource(R.string.simulation_score_long_term_risk),
                leftValue = longTermRisk,
                rightLabel = "",
                rightValue = null,
            )
        }
    }
}

@Composable
private fun ScoreRow(
    leftLabel: String,
    leftValue: Int,
    rightLabel: String,
    rightValue: Int?,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ScorePill(
            modifier = Modifier.weight(1f),
            label = leftLabel,
            value = leftValue,
        )
        if (rightValue != null) {
            ScorePill(
                modifier = Modifier.weight(1f),
                label = rightLabel,
                value = rightValue,
            )
        } else {
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun ScorePill(
    modifier: Modifier,
    label: String,
    value: Int,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value.toString(),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun NarrativeSection(
    title: String,
    lines: List<String>,
) {
    if (lines.isEmpty()) return

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            lines.forEach { line ->
                Text(
                    text = "• $line",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ScenarioCard(comparison: SimulationScenarioComparison) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = comparison.scenario.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = comparison.scenario.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = comparison.summary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = comparison.projected.narrative.headline,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Start,
            )
        }
    }
}
