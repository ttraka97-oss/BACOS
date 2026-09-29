package com.bacos.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.bacos.app.data.db.AppDb
import com.bacos.app.data.db.MistakeEntity
import com.bacos.app.data.db.QuestionEntity
import com.bacos.app.ui.components.EmptyState
import com.bacos.app.ui.components.GlassCard
import com.bacos.app.ui.components.PrimaryButton
import com.bacos.app.ui.theme.Bacos
import com.bacos.app.ui.theme.Body
import com.bacos.app.ui.theme.BodyMedium
import com.bacos.app.ui.theme.Caption
import com.bacos.app.ui.theme.H1
import com.bacos.app.ui.theme.Spacing
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** MISTAKES — the error book: concept, what happened, and practice this mistake. */
@Composable
fun MistakesScreen(db: AppDb, nav: NavHostController) {
    var mistakes by remember { mutableStateOf<List<MistakeEntity>>(emptyList()) }
    var questions by remember { mutableStateOf<Map<Long, QuestionEntity>>(emptyMap()) }

    LaunchedEffect(Unit) {
        val open = db.mistakeDao().open()
        mistakes = open
        val qs = db.questionDao().byLessons(open.map { it.lessonId }.distinct())
        questions = qs.associateBy { it.id }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Bacos.c.bg)
            .statusBarsPadding()
            .padding(horizontal = Spacing.xl)
    ) {
        Spacer(Modifier.height(Spacing.lg))
        Text("دفتر الأخطاء", style = H1, color = Bacos.c.textPrimary)
        Text("النظام يسجل تلقائياً كل خطأ — من هنا تصلحه", style = Caption, color = Bacos.c.textTertiary)
        Spacer(Modifier.height(Spacing.xl))

        if (mistakes.isEmpty()) {
            EmptyState(
                icon = "🎯",
                title = "لا أخطاء مفتوحة",
                subtitle = "خذ اختباراً — كل خطأ يُسجل هنا مع المفهوم والحل الصحيح"
            )
        } else {
            LazyColumn {
                items(mistakes) { m ->
                    val q = questions[m.questionId]
                    GlassCard(
                        Modifier
                            .fillMaxWidth()
                            .padding(bottom = Spacing.md)
                    ) {
                        Column(Modifier.padding(Spacing.lg)) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("❌ ${m.concept}", style = BodyMedium, color = Bacos.c.danger)
                                Text(
                                    formatDate(m.createdAt),
                                    style = Caption,
                                    color = Bacos.c.textTertiary
                                )
                            }
                            if (q != null) {
                                Spacer(Modifier.height(Spacing.sm))
                                Text(q.text, style = Body, color = Bacos.c.textSecondary, maxLines = 2)
                                Spacer(Modifier.height(Spacing.md))
                                Text("المفهوم الصحيح:", style = Caption, color = Bacos.c.textTertiary)
                                Text(q.explanation, style = Body, color = Bacos.c.textPrimary, maxLines = 3)
                                Spacer(Modifier.height(Spacing.lg))
                                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                                    PrimaryButton(
                                        text = "تدرب على هذا الخطأ",
                                        onClick = { nav.navigate("quiz/lesson:${m.lessonId}/6") },
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(Spacing.huge)) }
            }
        }
    }
}

private fun formatDate(millis: Long): String = try {
    val d = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
    d.format(DateTimeFormatter.ofPattern("dd/MM"))
} catch (_: Exception) { "" }
