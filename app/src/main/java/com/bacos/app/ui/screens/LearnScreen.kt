package com.bacos.app.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.bacos.app.data.db.AppDb
import com.bacos.app.data.db.ChapterEntity
import com.bacos.app.data.db.LessonEntity
import com.bacos.app.data.db.SubjectEntity
import com.bacos.app.domain.ReadinessEngine
import com.bacos.app.ui.Routes
import com.bacos.app.ui.components.EmptyState
import com.bacos.app.ui.components.GlassCard
import com.bacos.app.ui.theme.Bacos
import com.bacos.app.ui.theme.Body
import com.bacos.app.ui.theme.BodyMedium
import com.bacos.app.ui.theme.Caption
import com.bacos.app.ui.theme.H1
import com.bacos.app.ui.theme.H2
import com.bacos.app.ui.theme.Numbers
import com.bacos.app.ui.theme.Radius
import com.bacos.app.ui.theme.Spacing

/** LEARN — subjects → chapters → lessons, mastery everywhere. */
@Composable
fun LearnScreen(db: AppDb, nav: NavHostController) {
    var subjects by remember { mutableStateOf<List<SubjectEntity>>(emptyList()) }
    var progresses by remember { mutableStateOf<Map<Long, com.bacos.app.data.db.ProgressEntity>>(emptyMap()) }
    var expandedSubject by remember { mutableStateOf<Long?>(null) }
    var chapters by remember { mutableStateOf<Map<Long, List<ChapterEntity>>>(emptyMap()) }
    var lessons by remember { mutableStateOf<Map<Long, List<LessonEntity>>>(emptyMap()) }

    LaunchedEffect(Unit) {
        val subs = db.subjectDao().all()
        subjects = subs
        progresses = db.progressDao().all().associateBy { it.lessonId }
        val chMap = mutableMapOf<Long, List<ChapterEntity>>()
        val lsMap = mutableMapOf<Long, List<LessonEntity>>()
        subs.forEach { s ->
            val chs = db.chapterDao().bySubject(s.id)
            chMap[s.id] = chs
            chs.forEach { ch -> lsMap[ch.id] = db.lessonDao().byChapter(ch.id) }
        }
        chapters = chMap
        lessons = lsMap
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Bacos.c.bg)
            .statusBarsPadding()
            .padding(horizontal = Spacing.xl)
    ) {
        Spacer(Modifier.height(Spacing.lg))
        Text("تعلم", style = H1, color = Bacos.c.textPrimary)
        Text("المنهج كامل مع مستوى إتقانك في كل درس", style = Caption, color = Bacos.c.textTertiary)
        Spacer(Modifier.height(Spacing.xl))

        LazyColumn {
            items(subjects) { subject ->
                val subjectLessons = chapters[subject.id]?.flatMap { lessons[it.id] ?: emptyList() } ?: emptyList()
                val avg = if (subjectLessons.isEmpty()) 0 else subjectLessons.map { progresses[it.id]?.mastery ?: 0 }.average().toInt()
                val color = Color(android.graphics.Color.parseColor(subject.colorHex))

                GlassCard(
                    Modifier
                        .fillMaxWidth()
                        .padding(bottom = Spacing.md)
                        .clickable { expandedSubject = if (expandedSubject == subject.id) null else subject.id }
                ) {
                    Column(Modifier.padding(Spacing.lg)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(color.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) { Text(subjectEmoji(subject.code), style = Body) }
                                Spacer(Modifier.width(Spacing.md))
                                Column {
                                    Text(subject.nameAr, style = BodyMedium, color = Bacos.c.textPrimary)
                                    Text(subject.nameFr, style = Caption, color = Bacos.c.textTertiary)
                                }
                            }
                            Text("$avg%", style = Numbers, color = color)
                        }

                        if (expandedSubject == subject.id) {
                            Spacer(Modifier.height(Spacing.lg))
                            chapters[subject.id]?.forEach { chapter ->
                                val chapterLessons = lessons[chapter.id] ?: emptyList()
                                Text(chapter.nameAr, style = Caption, color = Bacos.c.textSecondary)
                                Spacer(Modifier.height(Spacing.sm))
                                chapterLessons.forEach { lesson ->
                                    val mastery = progresses[lesson.id]?.mastery ?: 0
                                    LessonRow(lesson, mastery, color) { nav.navigate(Routes.lesson(lesson.id)) }
                                    Spacer(Modifier.height(Spacing.sm))
                                }
                                Spacer(Modifier.height(Spacing.md))
                            }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(Spacing.huge)) }
        }
    }
}

@Composable
private fun LessonRow(lesson: LessonEntity, mastery: Int, color: Color, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(Radius.md)
            .background(Bacos.c.surface2)
            .clickable { onClick() }
            .padding(horizontal = Spacing.md, vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(lesson.titleAr, style = Body, color = Bacos.c.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${lesson.minutes} دق · ${difficultyLabel(lesson.difficulty)}", style = Caption, color = Bacos.c.textTertiary)
        }
        Spacer(Modifier.width(Spacing.md))
        Box(
            Modifier
                .width(64.dp)
                .height(5.dp)
                .clip(CircleShape)
                .background(Bacos.c.surface3)
        ) {
            Box(
                Modifier
                    .fillMaxWidth(mastery / 100f)
                    .height(5.dp)
                    .clip(CircleShape)
                    .background(color)
            )
        }
    }
}

private fun difficultyLabel(d: Int) = when (d) { 1 -> "سهل"; 2 -> "متوسط"; else -> "صعب" }

private fun subjectEmoji(code: String) = when (code) {
    "math" -> "∑"; "phys" -> "⚡"; "svt" -> "🧬"; else -> "📘"
}
