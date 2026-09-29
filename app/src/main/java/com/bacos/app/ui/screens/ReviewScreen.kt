package com.bacos.app.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.bacos.app.data.db.AppDb
import com.bacos.app.data.db.CardStateEntity
import com.bacos.app.data.db.FlashcardEntity
import com.bacos.app.data.db.SessionEntity
import com.bacos.app.domain.SRSEngine
import com.bacos.app.ui.components.EmptyState
import com.bacos.app.ui.components.GlassCard
import com.bacos.app.ui.components.PrimaryButton
import com.bacos.app.ui.components.GhostButton
import com.bacos.app.ui.theme.Bacos
import com.bacos.app.ui.theme.Body
import com.bacos.app.ui.theme.BodyMedium
import com.bacos.app.ui.theme.Caption
import com.bacos.app.ui.theme.H1
import com.bacos.app.ui.theme.H2
import com.bacos.app.ui.theme.Numbers
import com.bacos.app.ui.theme.NumbersLarge
import com.bacos.app.ui.theme.Radius
import com.bacos.app.ui.theme.Spacing
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * REVIEW — flashcard session with real SM-2 scheduling.
 * Swipe right = تذكرت · Swipe left = نسيت · tap to flip.
 */
@Composable
fun ReviewScreen(db: AppDb, nav: NavHostController) {
    val scope = rememberCoroutineScope()
    val view = LocalView.current

    var queue by remember { mutableStateOf<List<FlashcardEntity>>(emptyList()) }
    var states by remember { mutableStateOf<Map<Long, CardStateEntity>>(emptyMap()) }
    var index by remember { mutableIntStateOf(0) }
    var flipped by remember { mutableStateOf(false) }
    var done by remember { mutableStateOf(0) }
    var againCount by remember { mutableIntStateOf(0) }
    var goodCount by remember { mutableIntStateOf(0) }
    var finished by remember { mutableStateOf(false) }
    var offsetX by remember { mutableStateOf(0f) }
    var startTime by remember { mutableStateOf(0L) }

    LaunchedEffect(Unit) {
        val due = db.cardStateDao().due(System.currentTimeMillis(), 200)
        // load cards + states
        val cardIds = due.map { it.cardId }
        val loaded = mutableListOf<FlashcardEntity>()
        val stateMap = mutableMapOf<Long, CardStateEntity>()
        due.forEach { st ->
            val card = db.flashcardDao().byId(st.cardId)
            if (card != null) { loaded.add(card); stateMap[card.id] = st }
        }
        queue = loaded
        states = stateMap
        startTime = System.currentTimeMillis()
    }

    fun grade(quality: Int) {
        val card = queue.getOrNull(index) ?: return
        val state = states[card.id] ?: CardStateEntity(cardId = card.id)
        val result = SRSEngine.review(state, quality, System.currentTimeMillis())
        scope.launch {
            db.cardStateDao().upsert(
                CardStateEntity(
                    cardId = card.id,
                    ease = result.ease,
                    intervalDays = result.intervalDays,
                    repetitions = result.repetitions,
                    dueAt = result.dueAt,
                    lastReviewedAt = System.currentTimeMillis(),
                    lapses = result.lapses,
                )
            )
        }
        if (quality < 3) againCount += 1 else goodCount += 1
        done += 1
        view.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
        offsetX = 0f
        flipped = false
        if (index < queue.size - 1) index += 1 else {
            finished = true
            scope.launch {
                val minutes = ((System.currentTimeMillis() - startTime) / 60000L).coerceAtLeast(1L).toInt()
                db.sessionDao().insert(
                    SessionEntity(
                        day = LocalDate.now().toString(),
                        minutes = minutes,
                        type = "review",
                        xp = done * 5,
                        startedAt = startTime,
                    )
                )
                val profile = db.profileDao().get()
                if (profile != null) {
                    val today = LocalDate.now().toString()
                    val yesterday = LocalDate.now().minusDays(1).toString()
                    val streak = when {
                        profile.lastActiveDay == today -> profile.streak
                        profile.lastActiveDay == yesterday -> profile.streak + 1
                        else -> 1
                    }
                    db.profileDao().upsert(profile.copy(streak = streak, lastActiveDay = today, totalXp = profile.totalXp + done * 5))
                }
            }
        }
    }

    val animatedOffset by animateFloatAsState(targetValue = offsetX.toFloat(), animationSpec = tween(120), label = "drag")

    Box(
        Modifier
            .fillMaxSize()
            .background(Bacos.c.bg)
            .statusBarsPadding()
    ) {
        when {
            finished -> Column(
                Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = Spacing.xl),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("اكتملت المراجعة 🎉", style = H1, color = Bacos.c.textPrimary, textAlign = TextAlign.Center)
                Spacer(Modifier.height(Spacing.xl))
                Text("$done", style = NumbersLarge, color = Bacos.c.accent)
                Text("بطاقة راجعتها", style = Caption, color = Bacos.c.textTertiary)
                Spacer(Modifier.height(Spacing.xl))
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    GhostButton("نسيت: $againCount", onClick = {}, tint = Bacos.c.danger)
                    GhostButton("تذكرت: $goodCount", onClick = {}, tint = Bacos.c.accent)
                }
                Spacer(Modifier.height(Spacing.xl))
                PrimaryButton("تمام", onClick = { nav.popBackStack() }, modifier = Modifier.fillMaxWidth())
            }

            queue.isEmpty() -> Column(
                Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = Spacing.xl)
            ) {
                EmptyState(
                    icon = "✅",
                    title = "لا توجد بطاقات مستحقة",
                    subtitle = "راجع دروساً جديدة أو خذ اختباراً — ستظهر بطاقات جديدة في جدول المراجعة"
                )
                Spacer(Modifier.height(Spacing.xl))
                PrimaryButton("رجوع", onClick = { nav.popBackStack() }, modifier = Modifier.fillMaxWidth())
            }

            else -> Column(Modifier.fillMaxSize().padding(horizontal = Spacing.xl)) {
                Spacer(Modifier.height(Spacing.lg))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("${index + 1} / ${queue.size}", style = Numbers, color = Bacos.c.textSecondary)
                    Text("اسحب يمين = تذكرت · يسار = نسيت", style = Caption, color = Bacos.c.textTertiary)
                }
                Spacer(Modifier.height(Spacing.xl))

                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    val card = queue[index]
                    // ── The card: drag + flip ──
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(340.dp)
                            .graphicsLayer {
                                translationX = animatedOffset
                                rotationZ = animatedOffset * 0.04f
                            }
                            .pointerInput(card.id) {
                                detectHorizontalDragGestures(
                                    onDragEnd = {
                                        if (offsetX > 160f) grade(SRSEngine.GRADE_GOOD)
                                        else if (offsetX < -160f) grade(SRSEngine.GRADE_AGAIN)
                                        offsetX = 0f
                                    }
                                ) { change, dragAmount ->
                                    change.consume()
                                    offsetX += dragAmount
                                }
                            }
                    ) {
                        GlassCard(
                            Modifier
                                .fillMaxSize()
                                .clickable { flipped = !flipped },
                            cornerRadius = Radius.xl
                        ) {
                            Column(
                                Modifier
                                    .fillMaxSize()
                                    .padding(Spacing.xl),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    if (flipped) "الجواب" else "السؤال",
                                    style = Caption,
                                    color = Bacos.c.accent
                                )
                                Spacer(Modifier.height(Spacing.xl))
                                Text(
                                    if (flipped) card.back else card.front,
                                    style = H2,
                                    color = Bacos.c.textPrimary,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(Modifier.height(Spacing.xl))
                                Text(
                                    if (flipped) "اضغط للرجوع للسؤال" else "اضغط لقلب البطاقة",
                                    style = Caption,
                                    color = Bacos.c.textTertiary
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(Spacing.xl))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    GhostButton("نسيت", onClick = { grade(SRSEngine.GRADE_AGAIN) }, tint = Bacos.c.danger)
                    GhostButton("صعبة", onClick = { grade(SRSEngine.GRADE_HARD) }, tint = Bacos.c.warning)
                    GhostButton("سهلة", onClick = { grade(SRSEngine.GRADE_EASY) }, tint = Bacos.c.accent)
                }
                Spacer(Modifier.height(Spacing.xl))
            }
        }
    }
}
