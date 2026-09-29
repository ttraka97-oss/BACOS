package com.bacos.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.navigation.NavHostController
import com.bacos.app.data.db.AppDb
import com.bacos.app.data.db.LessonEntity
import com.bacos.app.data.db.MistakeEntity
import com.bacos.app.data.db.ProgressEntity
import com.bacos.app.domain.PlannerEngine
import com.bacos.app.domain.ReadinessEngine
import com.bacos.app.ui.Routes
import com.bacos.app.ui.components.GlassCard
import com.bacos.app.ui.components.PrimaryButton
import com.bacos.app.ui.components.ReadinessRing
import com.bacos.app.ui.components.SectionHeader
import com.bacos.app.ui.components.StatCard
import com.bacos.app.ui.theme.Bacos
import com.bacos.app.ui.theme.Body
import com.bacos.app.ui.theme.BodyMedium
import com.bacos.app.ui.theme.BodySmall
import com.bacos.app.ui.theme.Caption
import com.bacos.app.ui.theme.H2
import com.bacos.app.ui.theme.H3
import com.bacos.app.ui.theme.Numbers
import com.bacos.app.ui.theme.Spacing
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * HOME = TODAY. The screen answers one question:
 * "ماذا يجب أن أفعل الآن؟" — built from real BAC BRAIN signals.
 */
@Composable
fun HomeScreen(db: AppDb, nav: NavHostController) {
    var profile by remember { mutableStateOf<com.bacos.app.data.db.ProfileEntity?>(null) }
    var mission by remember { mutableStateOf<PlannerEngine.TodayMission?>(null) }
    var readiness by remember { mutableStateOf(0) }
    var streak by remember { mutableStateOf(0) }
    var daysLeft by remember { mutableStateOf(0) }
    var subjectStats by remember { mutableStateOf<List<Triple<String, Int, Color>>>(emptyList()) }
    var minutesToday by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        val p = db.profileDao().get() ?: return@LaunchedEffect
        profile = p
        streak = p.streak

        val now = System.currentTimeMillis()
        val due = db.cardStateDao().due(now, 200)
        val lessons = db.lessonDao().all()
        val progresses = db.progressDao().all().associateBy { it.lessonId }
        val mistakes = db.mistakeDao().open()

        // readiness per subject
        val subjects = db.subjectDao().all()
        val chapters = subjects.flatMap { db.chapterDao().bySubject(it.id) }
        val lessonToSubject = lessons.associate { l ->
            l.id to (chapters.firstOrNull { c -> c.id == l.chapterId }?.subjectId ?: 0L)
        }
        val recentSessions = db.sessionDao().all().take(30)
        val acc = if (recentSessions.isEmpty()) 0.5 else {
            val q = recentSessions.sumOf { it.questions }
            if (q == 0) 0.5 else recentSessions.sumOf { it.correct }.toDouble() / q
        }

        val inputs = ReadinessEngine.computeSubjectInputs(
            lessonsBySubject = subjects.associate { s -> s.id to lessons.filter { lessonToSubject[it.id] == s.id } },
            progressByLesson = progresses,
            recentAccuracy = acc,
        )
        readiness = ReadinessEngine.overall(inputs.values.map { ReadinessEngine.subjectScore(it) }, streak)

        subjectStats = subjects.map { s ->
            val input = inputs[s.id]
            val score = if (input != null) ReadinessEngine.subjectScore(input).toInt() else 0
            Triple(s.nameAr, score, Color(android.graphics.Color.parseColor(s.colorHex)))
        }

        val subjectMastery = subjectStats.associate { it.first to it.second.toDouble() }
        mission = PlannerEngine.build(
            dueCardIds = due.map { it.cardId },
            lessons = lessons,
            progressByLesson = progresses,
            openMistakes = mistakes,
            subjectMastery = subjectMastery,
            daysSinceActive = 0,
        )

        daysLeft = ChronoUnit.DAYS.between(LocalDate.now(), Instant.ofEpochMilli(p.examDate).atZone(ZoneId.systemDefault()).toLocalDate()).toInt()
        minutesToday = db.sessionDao().byDay(LocalDate.now().toString()).sumOf { it.minutes }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Bacos.c.bg)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.xl)
    ) {
        // ── Header: greeting + streak ──
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = Spacing.lg),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(mission?.greetingLine ?: "أهلاً بك 👋", style = H2, color = Bacos.c.textPrimary, maxLines = 1)
                Text("مهمة الباك اليوم", style = Caption, color = Bacos.c.textTertiary)
            }
            StreakBadge(streak)
        }

        Spacer(Modifier.height(Spacing.xl))

        // ── Hero: readiness + countdown ──
        GlassCard(Modifier.fillMaxWidth()) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(Spacing.xl),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ReadinessRing(score = readiness, size = 170, subtitle = "BAC READINESS™")
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("$daysLeft", style = Numbers, color = Bacos.c.textPrimary)
                        Text("يوم للباك", style = Caption, color = Bacos.c.textTertiary)
                        Spacer(Modifier.height(Spacing.md))
                        Text(
                            ReadinessEngine.label(readiness),
                            style = BodySmall, color = Bacos.c.accent
                        )
                    }
                }
                Spacer(Modifier.height(Spacing.md))
                val weak = subjectStats.filter { it.second < 50 }
                Text(
                    if (weak.isEmpty()) "كل المواد تحت السيطرة — حافظ على الوتيرة 🔥"
                    else "${weak.size} مواد تحتاج انتباه: ${weak.joinToString("، ") { it.first }}",
                    style = BodySmall,
                    color = if (weak.isEmpty()) Bacos.c.accent else Bacos.c.warning,
                )
            }
        }

        Spacer(Modifier.height(Spacing.xl))

        // ── Today's mission ──
        AnimatedVisibility(visible = mission != null, enter = fadeIn(tween(400)) + slideInVertically(tween(400)) { it / 4 }) {
            Column {
                SectionHeader("مهمة اليوم")
                mission?.let { m ->
                    MissionCard(
                        emoji = "🧠",
                        title = "مراجعة ذكية",
                        subtitle = "${m.dueReviews} بطاقة جاهزة — النظام اختارها ليك حسب نسيانك",
                        meta = "REVIEW",
                        color = Bacos.c.accent,
                        enabled = m.dueReviews > 0,
                    ) { nav.navigate(Routes.REVIEW) }

                    MissionCard(
                        emoji = "📘",
                        title = m.nextLesson?.titleAr ?: "كل الدروس تخلصت 🎉",
                        subtitle = m.nextLesson?.summary ?: "راجع بطاقاتك وكمل تمارين",
                        meta = "LEARN",
                        color = Bacos.c.info,
                        enabled = m.nextLesson != null,
                    ) {
                        m.nextLesson?.let { nav.navigate(Routes.lesson(it.id)) }
                    }

                    MissionCard(
                        emoji = "🎯",
                        title = "تدريب ${m.practiceTarget ?: "سريع"}",
                        subtitle = "أسئلة تكيفية — تبدأ سهلة وترتفع معك",
                        meta = "PRACTICE",
                        color = Bacos.c.warning,
                        enabled = true,
                    ) {
                        val code = when (m.practiceTarget) {
                            "الفيزياء والكيمياء" -> "phys"
                            "علوم الطبيعة والحياة" -> "svt"
                            else -> "math"
                        }
                        nav.navigate(Routes.quiz(code, 8))
                    }

                    if (m.fixConcepts.isNotEmpty()) {
                        MissionCard(
                            emoji = "⚠️",
                            title = "صحّح ${m.fixConcepts.size} خطأ قديم",
                            subtitle = "أخطاؤك المسجلة: ${m.fixConcepts.joinToString("، ") { it.concept }.take(50)}",
                            meta = "FIX",
                            color = Bacos.c.danger,
                            enabled = true,
                        ) { nav.navigate(Routes.MISTAKES) }
                    }
                }
            }
        }

        Spacer(Modifier.height(Spacing.xl))

        // ── Quick stats ──
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
            StatCard("$minutesToday", "دقيقة اليوم", modifier = Modifier.weight(1f), accent = Bacos.c.accent, animate = true)
            StatCard("$streak", "يوم متواصل", modifier = Modifier.weight(1f), accent = Bacos.c.warning)
            StatCard("$readiness%", "التحضير", modifier = Modifier.weight(1f), accent = Bacos.c.info)
        }

        Spacer(Modifier.height(Spacing.xl))

        // ── Subject mastery ──
        SectionHeader("مستوى المواد", action = "الكل", onAction = { nav.navigate(Routes.LEARN) })
        subjectStats.forEach { (name, score, color) ->
            SubjectRow(name, score, color)
            Spacer(Modifier.height(Spacing.md))
        }

        Spacer(Modifier.height(Spacing.lg))
        PrimaryButton(
            text = "ابدأ جلسة الآن",
            onClick = { nav.navigate(Routes.REVIEW) },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(Spacing.xxl))
        Spacer(Modifier.height(Spacing.huge))
    }
}

@Composable
private fun StreakBadge(streak: Int) {
    Row(
        Modifier
            .clip(CircleShape)
            .background(Bacos.c.warningDim)
            .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("🔥", style = BodySmall)
        Spacer(Modifier.width(Spacing.xs))
        Text("$streak يوم", style = Numbers, color = Bacos.c.warning)
    }
}

@Composable
private fun MissionCard(
    emoji: String,
    title: String,
    subtitle: String,
    meta: String,
    color: Color,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    GlassCard(
        Modifier
            .fillMaxWidth()
            .then(if (enabled) Modifier.clickable { onClick() } else Modifier)
            .padding(vertical = Spacing.xs)
    ) {
        Row(
            Modifier.padding(Spacing.lg),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) { Text(emoji, style = Body) }
            Spacer(Modifier.width(Spacing.md))
            Column(Modifier.weight(1f)) {
                Text(title, style = BodyMedium, color = Bacos.c.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(2.dp))
                Text(subtitle, style = Caption, color = Bacos.c.textTertiary, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Text(meta, style = Caption, color = color)
        }
    }
}

@Composable
private fun SubjectRow(name: String, score: Int, color: Color) {
    GlassCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(Spacing.lg)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(name, style = BodyMedium, color = Bacos.c.textPrimary)
                Text("$score%", style = Numbers, color = color)
            }
            Spacer(Modifier.height(Spacing.sm))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(Bacos.c.surface3)
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(score / 100f)
                        .height(6.dp)
                        .clip(CircleShape)
                        .background(color)
                )
            }
        }
    }
}

private fun Int.dp = androidx.compose.ui.unit.Dp(this.toFloat())
