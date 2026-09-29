package com.bacos.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import com.bacos.app.ui.theme.Bacos
import com.bacos.app.ui.theme.Caption
import com.bacos.app.ui.theme.NumbersLarge
import com.bacos.app.ui.theme.Spacing

/**
 * Animated BAC READINESS ring — the signature visual of BACOS.
 * Progress sweeps in with a spring when the composable enters composition.
 */
@Composable
fun ReadinessRing(
    score: Int,
    modifier: Modifier = Modifier,
    size: Int = 200,
    ringColor: Color = Bacos.c.accent,
    subtitle: String? = null,
) {
    var started by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { started = true }
    val sweep by animateFloatAsState(
        targetValue = if (started) score / 100f else 0f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 40f),
        label = "readiness"
    )

    Box(modifier = modifier.size(size.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = size * 0.055f
            val inset = stroke / 2 + size * 0.06f
            val arcSize = Size(this.size.width - inset * 2, this.size.height - inset * 2)
            val topLeft = Offset(inset, inset)

            // Track
            drawArc(
                color = Bacos.c.surface3,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
            // Progress sweep
            if (sweep > 0.01f) {
                drawArc(
                    brush = Brush.sweepGradient(
                        0f to ringColor.copy(alpha = 0.65f),
                        0.25f to ringColor,
                        0.5f to ringColor.copy(alpha = 0.85f),
                        0.75f to ringColor,
                        1f to ringColor.copy(alpha = 0.65f),
                    ),
                    startAngle = -90f,
                    sweepAngle = 360f * sweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("$score", style = NumbersLarge, color = Bacos.c.textPrimary, textAlign = TextAlign.Center)
            if (subtitle != null) {
                Spacer(Modifier.height(Spacing.xs))
                Text(subtitle, style = Caption, color = Bacos.c.textTertiary, textAlign = TextAlign.Center)
            }
        }
    }
}

/** Cinematic counting text used for scores after quizzes. */
@Composable
fun CountUpText(
    target: Int,
    modifier: Modifier = Modifier,
    style: androidx.compose.ui.text.TextStyle = NumbersLarge,
    color: Color = Bacos.c.textPrimary,
    suffix: String = "",
) {
    var started by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { started = true }
    val value by animateFloatAsState(
        targetValue = if (started) target.toFloat() else 0f,
        animationSpec = tween(durationMillis = 1100),
        label = "countup"
    )
    Text(
        "${value.toInt()}$suffix",
        style = style,
        color = color,
        modifier = modifier,
    )
}
