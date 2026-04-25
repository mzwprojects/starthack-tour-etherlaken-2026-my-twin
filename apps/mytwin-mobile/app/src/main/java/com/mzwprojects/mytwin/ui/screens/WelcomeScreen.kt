package com.mzwprojects.mytwin.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mzwprojects.mytwin.R
import com.mzwprojects.mytwin.ui.theme.VitalBright
import kotlinx.coroutines.launch

// ─── Model ───────────────────────────────────────────────────────────────────

private data class OnboardingPage(
    val eyebrowRes: Int,
    val headlineRes: Int,
    val bodyRes: Int,
    val illustration: @Composable (Float) -> Unit,
)

private val pages: List<OnboardingPage> = listOf(
    OnboardingPage(
        eyebrowRes = R.string.welcome_page1_eyebrow,
        headlineRes = R.string.welcome_page1_headline,
        bodyRes = R.string.welcome_page1_body,
        illustration = { pulse -> TwinPulseIllustration(pulse) },
    ),
    OnboardingPage(
        eyebrowRes = R.string.welcome_page2_eyebrow,
        headlineRes = R.string.welcome_page2_headline,
        bodyRes = R.string.welcome_page2_body,
        illustration = { _ -> TrajectoryIllustration() },
    ),
    OnboardingPage(
        eyebrowRes = R.string.welcome_page3_eyebrow,
        headlineRes = R.string.welcome_page3_headline,
        bodyRes = R.string.welcome_page3_body,
        illustration = { _ -> ForkIllustration() },
    ),
)

// ─── Screen ──────────────────────────────────────────────────────────────────

@Composable
fun WelcomeScreen(onFinished: () -> Unit) {
    val pagerState = rememberPagerState { pages.size }
    val isLastPage = pagerState.currentPage == pages.lastIndex
    val scope = rememberCoroutineScope()

    // Infinite pulse passed into illustrations
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulseFloat",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        // ── Pager ────────────────────────────────────────────────────────────
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
        ) { index ->
            PageContent(
                page = pages[index],
                pulse = pulse,
            )
        }

        // ── Skip ─────────────────────────────────────────────────────────────
        AnimatedVisibility(
            visible = !isLastPage,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(16.dp),
        ) {
            TextButton(onClick = onFinished) {
                Text(
                    text = stringResource(R.string.action_skip),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // ── Bottom bar ───────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 28.dp)
                .padding(bottom = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(28.dp),
        ) {
            PagerIndicator(
                pageCount = pages.size,
                currentPage = pagerState.currentPage,
            )

            AnimatedContent(
                targetState = isLastPage,
                transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(300)) },
                label = "ctaButton",
            ) { lastPage ->
                Button(
                    onClick = {
                        if (lastPage) {
                            onFinished()
                        } else {
                            scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = MaterialTheme.shapes.extraLarge,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (lastPage)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.primaryContainer,
                        contentColor = if (lastPage)
                            MaterialTheme.colorScheme.onPrimary
                        else
                            MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
                ) {
                    Text(
                        text = stringResource(
                            if (lastPage) R.string.welcome_cta_begin
                            else R.string.action_next,
                        ),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
        }
    }
}

// ─── Page content ────────────────────────────────────────────────────────────

@Composable
private fun PageContent(
    page: OnboardingPage,
    pulse: Float,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(0.12f))

        // Illustration
        Box(
            modifier = Modifier.size(260.dp),
            contentAlignment = Alignment.Center,
        ) {
            page.illustration(pulse)
        }

        Spacer(Modifier.weight(0.08f))

        // Eyebrow
        Text(
            text = stringResource(page.eyebrowRes),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(12.dp))

        // Headline
        Text(
            text = stringResource(page.headlineRes),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(16.dp))

        // Body
        Text(
            text = stringResource(page.bodyRes),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.weight(0.3f))
    }
}

// ─── Indicator ───────────────────────────────────────────────────────────────

@Composable
private fun PagerIndicator(
    pageCount: Int,
    currentPage: Int,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(pageCount) { index ->
            val isActive = index == currentPage
            val width: Dp by androidx.compose.animation.core.animateDpAsState(
                targetValue = if (isActive) 24.dp else 8.dp,
                animationSpec = tween(300, easing = FastOutSlowInEasing),
                label = "indicatorWidth",
            )
            val color by animateColorAsState(
                targetValue = if (isActive)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.surfaceVariant,
                animationSpec = tween(300),
                label = "indicatorColor",
            )
            Box(
                modifier = Modifier
                    .height(8.dp)
                    .width(width)
                    .clip(CircleShape)
                    .background(color),
            )
        }
    }
}

// ─── Illustrations ────────────────────────────────────────────────────────────

// Page 1 · Zwei pulsierende Kreise → User + Twin
@Composable
private fun TwinPulseIllustration(pulse: Float) {
    val primary = MaterialTheme.colorScheme.primary
    val container = MaterialTheme.colorScheme.primaryContainer
    val surface = MaterialTheme.colorScheme.surfaceContainerHigh

    Canvas(modifier = Modifier.fillMaxSize()) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val baseRadius = size.minDimension * 0.28f
        val offset = baseRadius * 0.45f
        val pulseExtra = pulse * baseRadius * 0.08f

        // Äusserer Puls-Ring
        drawCircle(
            color = container.copy(alpha = 0.25f + pulse * 0.15f),
            radius = baseRadius + pulseExtra + baseRadius * 0.22f,
            center = Offset(cx, cy),
        )
        drawCircle(
            color = container.copy(alpha = 0.12f + pulse * 0.08f),
            radius = baseRadius + pulseExtra + baseRadius * 0.44f,
            center = Offset(cx, cy),
        )

        // Linker Kreis (User · heute)
        drawCircle(color = surface, radius = baseRadius, center = Offset(cx - offset, cy))
        drawCircle(
            color = container,
            radius = baseRadius,
            center = Offset(cx - offset, cy),
            style = Stroke(2.dp.toPx())
        )

        // Rechter Kreis (Twin · Zukunft)
        drawCircle(
            color = primary.copy(alpha = 0.15f),
            radius = baseRadius,
            center = Offset(cx + offset, cy)
        )
        drawCircle(
            color = primary,
            radius = baseRadius,
            center = Offset(cx + offset, cy),
            style = Stroke(2.dp.toPx())
        )

        // Verbindungslinie
        drawLine(
            color = primary.copy(alpha = 0.35f + pulse * 0.25f),
            start = Offset(cx - offset + baseRadius * 0.6f, cy),
            end = Offset(cx + offset - baseRadius * 0.6f, cy),
            strokeWidth = 1.5.dp.toPx(),
            cap = StrokeCap.Round,
        )
    }
}

// Page 2 · Gesundheitskurve mit Gefahren-Plateau und Divergenz
@Composable
private fun TrajectoryIllustration() {
    val primary = MaterialTheme.colorScheme.primary
    val error = MaterialTheme.colorScheme.error
    val outline = MaterialTheme.colorScheme.outlineVariant

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val pad = 24.dp.toPx()

        // Grid-Linien
        repeat(4) { i ->
            val y = pad + (h - 2 * pad) * (i / 3f)
            drawLine(outline.copy(alpha = 0.3f), Offset(pad, y), Offset(w - pad, y), 0.5.dp.toPx())
        }

        // Absteigende "so weiter"-Kurve (rot)
        val badPath = Path().apply {
            moveTo(pad, h * 0.35f)
            cubicTo(
                w * 0.3f, h * 0.38f,
                w * 0.55f, h * 0.52f,
                w - pad, h * 0.74f,
            )
        }
        drawPath(
            badPath,
            error.copy(alpha = 0.6f),
            style = Stroke(2.dp.toPx(), cap = StrokeCap.Round)
        )

        // Aufsteigende "verändert"-Kurve (teal)
        val goodPath = Path().apply {
            moveTo(pad, h * 0.35f)
            cubicTo(
                w * 0.3f, h * 0.33f,
                w * 0.55f, h * 0.22f,
                w - pad, h * 0.18f,
            )
        }
        drawPath(goodPath, primary, style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round))

        // Startpunkt
        drawCircle(primary, 5.dp.toPx(), Offset(pad, h * 0.35f))
    }
}

// Page 3 · Verzweigung → zwei Zukunftspfade
@Composable
private fun ForkIllustration() {
    val primary = MaterialTheme.colorScheme.primary
    val vital = VitalBright
    val surface = MaterialTheme.colorScheme.surfaceContainerHigh
    val onSurface = MaterialTheme.colorScheme.onSurfaceVariant

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val mid = Offset(w / 2f, h * 0.52f)
        val src = Offset(w / 2f, h * 0.22f)

        // Stamm
        drawLine(primary, src, mid, 2.5.dp.toPx(), StrokeCap.Round)

        // Linker Ast (dunkler)
        drawLine(
            onSurface.copy(alpha = 0.35f),
            mid,
            Offset(w * 0.22f, h * 0.80f),
            2.dp.toPx(),
            StrokeCap.Round,
        )
        // Rechter Ast (teal · beste Zukunft)
        drawLine(
            vital,
            mid,
            Offset(w * 0.78f, h * 0.80f),
            2.5.dp.toPx(),
            StrokeCap.Round,
        )

        // Endpunkte
        drawCircle(onSurface.copy(alpha = 0.3f), 8.dp.toPx(), Offset(w * 0.22f, h * 0.80f))
        drawCircle(vital, 10.dp.toPx(), Offset(w * 0.78f, h * 0.80f))

        // Startpunkt
        drawCircle(primary, 6.dp.toPx(), src)

        // Verzweigungspunkt
        drawCircle(surface, 10.dp.toPx(), mid)
        drawCircle(primary, 10.dp.toPx(), mid, style = Stroke(2.dp.toPx()))
    }
}