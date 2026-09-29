package com.bacos.app.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bacos.app.ui.theme.Bacos
import com.bacos.app.ui.theme.ButtonText
import com.bacos.app.ui.theme.BodySmall
import com.bacos.app.ui.theme.Caption
import com.bacos.app.ui.theme.H3
import com.bacos.app.ui.theme.Numbers
import com.bacos.app.ui.theme.Radius
import com.bacos.app.ui.theme.Spacing

// ═══════════ Buttons ═══════════

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .clip(Radius.md)
            .background(
                if (enabled) Brush.horizontalGradient(listOf(Bacos.c.accent, Bacos.c.accentDeep))
                else Brush.horizontalGradient(listOf(Bacos.c.surface3, Bacos.c.surface3))
            )
            .clickable(interactionSource = interaction, indication = rememberRipple(), enabled = enabled, onClick = onClick)
            .padding(vertical = Spacing.lg, horizontal = Spacing.xl),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            style = ButtonText,
            color = if (enabled) Bacos.c.bg else Bacos.c.textTertiary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun GhostButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = Bacos.c.textSecondary,
) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .clip(Radius.md)
            .border(1.dp, Bacos.c.border, Radius.md)
            .clickable(interactionSource = interaction, indication = rememberRipple(), onClick = onClick)
            .padding(vertical = Spacing.md, horizontal = Spacing.lg),
        contentAlignment = Alignment.Center
    ) {
        Text(text, style = ButtonText, color = tint)
    }
}

// ═══════════ Cards ═══════════

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: RoundedCornerShape = Radius.lg,
    border: Boolean = true,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .clip(cornerRadius)
            .background(Bacos.c.surface1)
            .then(if (border) Modifier.border(1.dp, Bacos.c.borderSubtle, cornerRadius) else Modifier)
            .animateContentSize(spring())
    ) {
        content()
    }
}

@Composable
fun AccentCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .clip(Radius.lg)
            .background(
                Brush.linearGradient(
                    listOf(
                        Bacos.c.accent.copy(alpha = 0.16f),
                        Bacos.c.surface1,
                    )
                )
            )
            .border(1.dp, Bacos.c.accent.copy(alpha = 0.25f), Radius.lg)
    ) { content() }
}

// ═══════════ Data display ═══════════

@Composable
fun StatCard(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    accent: Color = Bacos.c.textPrimary,
    animate: Boolean = false,
) {
    val intValue = value.toIntOrNull()
    val animated by animateIntAsState(
        targetValue = intValue ?: 0,
        animationSpec = if (animate) tween(900) else tween(0),
        label = "stat"
    )
    GlassCard(modifier = modifier) {
        Column(Modifier.padding(Spacing.lg)) {
            Text(
                if (animate && intValue != null) "$animated" else value,
                style = Numbers,
                color = accent,
            )
            Spacer(Modifier.height(Spacing.xs))
            Text(label, style = Caption, color = Bacos.c.textTertiary)
        }
    }
}

@Composable
fun MasteryBar(progress: Float, modifier: Modifier = Modifier, color: Color = Bacos.c.accent) {
    Box(
        modifier = modifier
            .clip(Radius.pill)
            .height(6.dp)
            .background(Bacos.c.surface3)
    ) {
        Box(
            Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .height(6.dp)
                .clip(Radius.pill)
                .background(color)
        )
    }
}

@Composable
fun Chip(
    text: String,
    selected: Boolean = false,
    onClick: () -> Unit = {},
    tint: Color = Bacos.c.textSecondary,
) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .clip(Radius.pill)
            .background(if (selected) Bacos.c.accentDim else Bacos.c.surface2)
            .border(
                1.dp,
                if (selected) Bacos.c.accent.copy(alpha = 0.4f) else Bacos.c.borderSubtle,
                Radius.pill
            )
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = Spacing.lg, vertical = Spacing.sm)
    ) {
        Text(text, style = BodySmall, color = if (selected) Bacos.c.accent else tint)
    }
}

@Composable
fun SectionHeader(title: String, action: String? = null, onAction: (() -> Unit)? = null) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.sm),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = H3, color = Bacos.c.textPrimary)
        if (action != null && onAction != null) {
            Text(
                action,
                style = BodySmall,
                color = Bacos.c.accent,
                modifier = Modifier.clickable(onClick = onAction)
            )
        }
    }
}

@Composable
fun EmptyState(icon: String, title: String, subtitle: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(Spacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(icon, style = Numbers, color = Bacos.c.textTertiary)
        Spacer(Modifier.height(Spacing.md))
        Text(title, style = H3, color = Bacos.c.textSecondary)
        Spacer(Modifier.height(Spacing.xs))
        Text(subtitle, style = BodySmall, color = Bacos.c.textTertiary)
    }
}

@Composable
fun StreakFlame(streak: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("🔥", style = BodySmall)
        Spacer(Modifier.width(Spacing.xs))
        Text("$streak", style = Numbers, color = Bacos.c.warning)
    }
}
