package com.bacos.app.ui.screens

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.bacos.app.data.db.AppDb
import com.bacos.app.data.db.SessionEntity
import com.bacos.app.ui.components.GlassCard
import com.bacos.app.ui.components.ReadinessRing
import com.bacos.app.ui.components.SectionHeader
import com.bacos.app.ui.components.StatCard
import com.bacos.app.ui.theme.Bacos
import com.bacos.app.ui.theme.Body
import com.bacos.app.ui.theme.BodyMedium
import com.bacos.app.ui.theme.Caption
import com.bacos.app.ui.theme.H1
import com.bacos.app.ui.theme.Numbers
import com.bacos.app.ui.theme.Spacing
import java.time.LocalDate
import java.time.Instant
import java.time.ZoneId

/** PROFILE — analytics + achievements + settings summary. */
@Composable
fun ProfileScreen(db: AppDb, nav: NavHostController) {
    var profile by remember { mutableStateOf<com.bacos.app.data.db.ProfileEntity?>(null) }
    var sessions by remember { mutableStateOf<List<SessionEntity>>(emptyList()) }
    var readiness by remember { mutableStateOf(0) }
    var questionStats by remember { mutableStateOf(0 to 0) } // answered to correct
    var subjectStats by remember { mutableStateOf<List<Triple<String, Int, Color>>>(emptyList()) }

    LaunchedEffect(Unit) {
        profile = db.profileDao().get()
        sessions = db.sessionDao().all()
        val progresses = db.progressDao().all()
        val answered = progresses.sumOf { it.attempts }
        val correct = progresses.sumOf { it.correct }
        questionStats = answered to correct

        val subjects = db.subjectDao().all()
        val lessons = db.lessonDao().all()
        val chapters = subjects.flatMap { db.chapterDao().bySubject(it.id) }
        val lessonToSubject = lessons.associate { l ->
            l.id to (chapters.firstOrNull { c -> c.id == l.chapterId }?.subjectId ?: 0L)
        }
        val progressMap = progresses.associateBy { it.lessonId }
        subjectStats = subjects.map { s ->
            val ls = lessons.filter { lessonToSubject[it.id] == s.id }
            val avg = if (ls.isEmpty()) 0 else ls.map { progressMap[it.id]?.mastery ?: 0 }.average().toInt()
            Triple(s.nameAr, avg, Color(android.graphics.Color.parseColor(s.colorHex)))
        }
        readiness = com.bacos.app.domain.ReadinessEngine.overall(
            subjectStats.map { it.second.toDouble() }, profile?.streak ?: 0
        )
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Bacos.c.bg)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.xl)
    ) {
        Spacer(Modifier.height(Spacing.lg))
        Text("تقدمي", style = H1, color = Bacos.c.textPrimary)
        Text("MY PROGRESS — التحليل الكامل", style = Caption, color = Bacos.c.textTertiary)
        Spacer(Modifier.height(Spacing.xl))

        // ── Hero: readiness + identity ──
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
                    ReadinessRing(score = readiness, size = 140, subtitle = "READINESS")
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(profile?.name?.ifEmpty { "تلميذ BACOS" } ?: "تلميذ BACOS", style = BodyMedium, color = Bacos.c.textPrimary)
                        Text(branchName(profile?.branch), style = Caption, color = Bacos.c.textTertiary)
                        Spacer(Modifier.height(Spacing.md))
                        Text("🔥 ${profile?.streak ?: 0}", style = Numbers, color = Bacos.c.warning)
                        Text("يوم متواصل", style = Caption, color = Bacos.c.textTertiary)
                    }
                }
            }
        }

        Spacer(Modifier.height(Spacing.xl))

        // ── Study stats ──
        SectionHeader("إحصائيات الدراسة")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
            val totalMinutes = sessions.sumOf { it.minutes }
            StatCard("$totalMinutes", "مجموع الدقائق", modifier = Modifier.weight(1f), accent = Bacos.c.accent, animate = true)
            StatCard("${questionStats.first}", "سؤال محلول", modifier = Modifier.weight(1f), accent = Bacos.c.info)
        }
        Spacer(Modifier.height(Spacing.md))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
            val acc = if (questionStats.first == 0) 0 else questionStats.second * 100 / questionStats.first
            StatCard("$acc%", "نسبة الصحة", modifier = Modifier.weight(1f), accent = Bacos.c.warning)
            StatCard("${profile?.totalXp ?: 0}", "XP", modifier = Modifier.weight(1f), accent = Bacos.c.ai)
        }

        Spacer(Modifier.height(Spacing.xl))

        // ── Weekly activity chart ──
        SectionHeader("نشاط الأسبوع")
        GlassCard(Modifier.fillMaxWidth()) {
            WeeklyChart(sessions, Modifier.padding(Spacing.lg))
        }

        Spacer(Modifier.height(Spacing.xl))

        // ── Subject mastery ──
        SectionHeader("الإتقان حسب المادة")
        subjectStats.forEach { (name, score, color) ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = Spacing.sm),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(10.dp).clip(CircleShape).background(color))
                    Spacer(Modifier.padding(horizontal = 3.dp))
                    Text(name, style = Body, color = Bacos.c.textPrimary)
                }
                Text("$score%", style = Numbers, color = color)
            }
        }

        Spacer(Modifier.height(Spacing.xl))

        // ── Achievements ──
        SectionHeader("الإنجازات")
        val totalQ = questionStats.first
        val achievements = listOf(
            "🔥 أسبوع كامل" to ((profile?.streak ?: 0) >= 7),
            "⚡ شهر دراسي" to (sessions.size >= 30),
            "🎯 100 سؤال" to (totalQ >= 100),
            "🧠 500 سؤال" to (totalQ >= 500),
            "📈 جاهز للباك" to (readiness >= 85),
        )
        achievements.forEach { (name, unlocked) ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = Spacing.sm),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(name, style = Body, color = if (unlocked) Bacos.c.textPrimary else Bacos.c.textTertiary)
                Text(if (unlocked) "✓" else "—", style = Body, color = if (unlocked) Bacos.c.accent else Bacos.c.textTertiary)
            }
        }

        Spacer(Modifier.height(Spacing.xl))
        Text(
            "BACOS v1.0 — نظام الدراسة الذكي\nمحرك التكرار المتباعد SM-2 · تحليل الإتقان التكيفي",
            style = Caption,
            color = Bacos.c.textTertiary,
            modifier = Modifier.fillMaxWidth(),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        Spacer(Modifier.height(Spacing.huge))
    }
}

@Composable
private fun WeeklyChart(sessions: List<SessionEntity>, modifier: Modifier = Modifier) {
    val days = (6 downTo 0).map { LocalDate.now().minusDays(it.toLong()) }
    val maxMinutes = maxOf(sessions.maxOfOrNull { it.minutes } ?: 1, 1)
    val labels = listOf("س", "ح", "ن", "ث", "ر", "خ", "ج") // أيام الأسبوع من السبت

    val activeColor = Bacos.c.accent
    val dimColor = Bacos.c.accent.copy(alpha = 0.35f)
    Column(modifier) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(120.dp)
        ) {
            val barWidth = size.width / 7f * 0.5f
            val gap = size.width / 7f
            days.forEachIndexed { i, day ->
                val minutes = sessions.filter { it.day == day.toString() }.sumOf { it.minutes }
                val h = (minutes.toFloat() / maxMinutes) * (size.height - 20f)
                drawRoundRect(
                    color = if (i == 6) activeColor else dimColor,
                    topLeft = androidx.compose.ui.geometry.Offset(
                        gap * i + (gap - barWidth) / 2f,
                        size.height - h
                    ),
                    size = androidx.compose.ui.geometry.Size(barWidth, h),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(12f, 12f),
                )
            }
        }
        Spacer(Modifier.height(Spacing.sm))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
            days.forEachIndexed { i, _ ->
                Text(labels[i], style = Caption, color = Bacos.c.textTertiary)
            }
        }
    }
}

private fun branchName(code: String?) = when (code) {
    "sciences" -> "علوم تجريبية"
    "math" -> "رياضيات"
    "techmath" -> "تقني رياضي"
    "gestion" -> "تسيير واقتصاد"
    "letters" -> "آداب وفلسفة"
    else -> "—"
}
