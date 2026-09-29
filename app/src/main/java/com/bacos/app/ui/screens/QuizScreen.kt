package com.bacos.app.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.bacos.app.data.db.AppDb
import com.bacos.app.data.db.MistakeEntity
import com.bacos.app.data.db.QuestionEntity
import com.bacos.app.data.db.SessionEntity
import com.bacos.app.domain.QuizEngine
import com.bacos.app.ui.Routes
import com.bacos.app.ui.components.CountUpText
import com.bacos.app.ui.components.GlassCard
import com.bacos.app.ui.components.PrimaryButton
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
import org.json.JSONArray
import java.time.LocalDate

/**
 * QUIZ — adaptive questions → answer + explanation → mastery & mistake updates.
 * `subject` is a subject code ("math"|"phys"|"svt"|"all") or "lesson:<id>".
 */
@Composable
fun QuizScreen(db: AppDb, nav: NavHostController, subject: String, count: Int) {
    val scope = rememberCoroutineScope()
    val view = LocalView.current

    var questions by remember { mutableStateOf<List<QuestionEntity>>(emptyList()) }
    var index by remember { mutableIntStateOf(0) }
    var chosen by remember { mutableIntStateOf(-1) }
    var revealed by remember { mutableStateOf(false) }
    var correctCount by remember { mutableIntStateOf(0) }
    var finished by remember { mutableStateOf(false) }
    var lessonMasteryUpdates by remember { mutableStateOf<Map<Long, Pair<Int, Int>>>(emptyMap()) }
    val results = remember { mutableStateListOf<Boolean>() }

    LaunchedEffect(subject, count) {
        val lessons = if (subject.startsWith("lesson:")) {
            val id = subject.removePrefix("lesson:").toLongOrNull() ?: 0L
            listOfNotNull(db.lessonDao().byId(id))
        } else {
            val all = db.lessonDao().all()
            if (subject == "all") all
            else {
                val subjects = db.subjectDao().all()
                val target = subjects.firstOrNull { it.code == subject }
                if (target == null) all
                else {
                    val chapters = db.chapterDao().bySubject(target.id)
                    chapters.flatMap { db.lessonDao().byChapter(it.id) }
                }
            }
        }
        val progresses = db.progressDao().all().associateBy { it.lessonId }
        val recent = db.sessionDao().all().take(20)
        val acc = if (recent.sumOf { it.questions } == 0) null
        else recent.sumOf { it.correct }.toDouble() / recent.sumOf { it.questions }

        val pool = db.questionDao().byLessons(lessons.map { it.id })
        questions = QuizEngine.selectQuestions(pool, progresses, count, acc)
    }

    fun parseChoices(q: QuestionEntity): List<String> = try {
        val arr = JSONArray(q.choicesJson)
        (0 until arr.length()).map { arr.getString(it) }
    } catch (_: Exception) { emptyList() }

    fun submit(choice: Int) {
        if (revealed || questions.isEmpty()) return
        val q = questions[index]
        chosen = choice
        revealed = true
        val isCorrect = choice == q.correctIndex
        results.add(isCorrect)
        if (isCorrect) {
            correctCount += 1
            view.performHapticFeedback(HapticFeedbackType.LongPress)
        } else {
            scope.launch {
                db.mistakeDao().insert(
                    MistakeEntity(
                        questionId = q.id,
                        lessonId = q.lessonId,
                        concept = q.concept,
                        chosenIndex = choice,
                        createdAt = System.currentTimeMillis(),
                    )
                )
            }
        }
        // accumulate per-lesson stats
        val prev = lessonMasteryUpdates[q.lessonId] ?: (0 to 0)
        lessonMasteryUpdates = lessonMasteryUpdates + (
                q.lessonId to Pair(prev.first + if (isCorrect) 1 else 0, prev.second + 1)
                )
    }

    fun next() {
        if (index < questions.size - 1) {
            index += 1
            chosen = -1
            revealed = false
        } else {
            finished = true
            // ── Adaptive loop: commit mastery + session + streak
            scope.launch {
                lessonMasteryUpdates.forEach { (lessonId, stats) ->
                    val p = db.progressDao().byLesson(lessonId)
                    val newMastery = QuizEngine.updateMastery(p?.mastery ?: 0, stats.first, stats.second)
                    db.progressDao().upsert(
                        com.bacos.app.data.db.ProgressEntity(
                            lessonId = lessonId,
                            mastery = newMastery,
                            status = if (newMastery >= 85) "done" else "learning",
                            lastStudiedAt = System.currentTimeMillis(),
                            attempts = (p?.attempts ?: 0) + stats.second,
                            correct = (p?.correct ?: 0) + stats.first,
                        )
                    )
                }
                db.sessionDao().insert(
                    SessionEntity(
                        day = LocalDate.now().toString(),
                        minutes = (questions.size * 2).coerceAtLeast(5),
                        type = "quiz",
                        xp = correctCount * 10,
                        questions = questions.size,
                        correct = correctCount,
                        startedAt = System.currentTimeMillis(),
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
                    db.profileDao().upsert(
                        profile.copy(streak = streak, lastActiveDay = today, totalXp = profile.totalXp + correctCount * 10)
                    )
                }
            }
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Bacos.c.bg)
            .statusBarsPadding()
            .padding(horizontal = Spacing.xl)
    ) {
        when {
            questions.isEmpty() -> Column(
                Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("⏳", style = H1, color = Bacos.c.textTertiary)
                Spacer(Modifier.height(Spacing.md))
                Text("جاري تحضير الأسئلة…", style = Body, color = Bacos.c.textSecondary)
            }

            finished -> QuizResult(
                correct = correctCount,
                total = questions.size,
                onRetry = {
                    index = 0; chosen = -1; revealed = false; correctCount = 0
                    results.clear(); finished = false
                },
                onDone = { nav.popBackStack() },
                onMistakes = { nav.navigate(Routes.MISTAKES) },
            )

            else -> Column(Modifier.fillMaxSize()) {
                Spacer(Modifier.height(Spacing.lg))

                // ── Progress dots + counter ──
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("${index + 1} / ${questions.size}", style = Numbers, color = Bacos.c.textSecondary)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        questions.indices.forEach { i ->
                            Box(
                                Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            i < results.size && results[i] -> Bacos.c.accent
                                            i < results.size -> Bacos.c.danger
                                            i == index -> Bacos.c.info
                                            else -> Bacos.c.surface3
                                        }
                                    )
                            )
                        }
                    }
                }

                Spacer(Modifier.height(Spacing.xl))

                AnimatedContent(
                    targetState = index,
                    transitionSpec = { fadeIn(tween(250)) togetherWith fadeOut(tween(150)) },
                    label = "question"
                ) { i ->
                    val q = questions[i]
                    val choices = parseChoices(q)
                    Column {
                        Text(q.text, style = H2, color = Bacos.c.textPrimary)
                        Spacer(Modifier.height(Spacing.xxl))
                        choices.forEachIndexed { ci, choice ->
                            AnswerOption(
                                text = choice,
                                state = when {
                                    !revealed -> if (chosen == ci) AnswerState.CHOSEN else AnswerState.IDLE
                                    ci == q.correctIndex -> AnswerState.CORRECT
                                    chosen == ci -> AnswerState.WRONG
                                    else -> AnswerState.DIM
                                },
                            ) { submit(ci) }
                            Spacer(Modifier.height(Spacing.md))
                        }
                        if (revealed) {
                            Spacer(Modifier.height(Spacing.lg))
                            GlassCard(Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(Spacing.lg)) {
                                    Text(
                                        if (chosen == q.correctIndex) "إجابة صحيحة ✓" else "إجابة خاطئة ✗",
                                        style = BodyMedium,
                                        color = if (chosen == q.correctIndex) Bacos.c.accent else Bacos.c.danger
                                    )
                                    Spacer(Modifier.height(Spacing.sm))
                                    Text(q.explanation, style = Body, color = Bacos.c.textSecondary)
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.weight(1f))

                if (revealed) {
                    PrimaryButton(
                        text = if (index < questions.size - 1) "السؤال التالي" else "إظهار النتيجة",
                        onClick = { next() },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Spacer(Modifier.height(Spacing.xl))
            }
        }
    }
}

private enum class AnswerState { IDLE, CHOSEN, CORRECT, WRONG, DIM }

@Composable
private fun AnswerOption(text: String, state: AnswerState, onClick: () -> Unit) {
    val borderColor = when (state) {
        AnswerState.CHOSEN -> Bacos.c.info
        AnswerState.CORRECT -> Bacos.c.accent
        AnswerState.WRONG -> Bacos.c.danger
        else -> Bacos.c.borderSubtle
    }
    val bg = when (state) {
        AnswerState.CORRECT -> Bacos.c.accentDim
        AnswerState.WRONG -> Bacos.c.dangerDim
        AnswerState.CHOSEN -> Bacos.c.surface3
        else -> Bacos.c.surface1
    }
    val textColor = when (state) {
        AnswerState.DIM -> Bacos.c.textTertiary
        else -> Bacos.c.textPrimary
    }
    Row(
        Modifier
            .fillMaxWidth()
            .clip(Radius.md)
            .background(bg)
            .then(if (state != AnswerState.IDLE) Modifier.border(1.dp, borderColor, Radius.md) else Modifier)
            .clickable(enabled = state == AnswerState.IDLE || state == AnswerState.CHOSEN) { onClick() }
            .padding(Spacing.lg),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text, style = Body, color = textColor)
    }
}

@Composable
private fun QuizResult(correct: Int, total: Int, onRetry: () -> Unit, onDone: () -> Unit, onMistakes: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(top = Spacing.huge),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("النتيجة", style = H1, color = Bacos.c.textPrimary)
        Spacer(Modifier.height(Spacing.xl))
        CountUpText(target = correct, suffix = " / $total", style = NumbersLarge, color = Bacos.c.accent)
        Spacer(Modifier.height(Spacing.md))
        val pct = if (total == 0) 0 else correct * 100 / total
        Text(
            when {
                pct >= 80 -> "ممتاز — المستوى يرتفع 🚀"
                pct >= 50 -> "جيد — واصل التدريب 💪"
                else -> "يحتاج مراجعة — راجع دفتر الأخطاء"
            },
            style = Body, color = Bacos.c.textSecondary, textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(Spacing.xl))
        GlassCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(Spacing.lg)) {
                Text("خطواتك القادمة", style = BodyMedium, color = Bacos.c.textPrimary)
                Spacer(Modifier.height(Spacing.md))
                NextStep("١", "راجع البطاقات المستحقة اليوم") { onDone() }
                NextStep("٢", "صحّح أخطاءك في دفتر الأخطاء") { onMistakes() }
                NextStep("٣", "أعد الاختبار غداً لتثبيت المعلومة") { onRetry() }
            }
        }
        Spacer(Modifier.weight(1f))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
            PrimaryButton("تم", onClick = onDone, modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(Spacing.xxl))
    }
}

@Composable
private fun NextStep(num: String, text: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(Radius.md)
            .clickable { onClick() }
            .padding(vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(Bacos.c.surface3),
            contentAlignment = Alignment.Center
        ) { Text(num, style = Caption, color = Bacos.c.accent) }
        Spacer(Modifier.width(Spacing.md))
        Text(text, style = Body, color = Bacos.c.textSecondary)
    }
}
