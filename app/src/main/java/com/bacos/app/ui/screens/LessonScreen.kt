package com.bacos.app.ui.screens

import androidx.compose.animation.animateContentSize
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.bacos.app.data.db.AppDb
import com.bacos.app.data.db.ProgressEntity
import com.bacos.app.ui.Routes
import com.bacos.app.ui.components.GlassCard
import com.bacos.app.ui.components.PrimaryButton
import com.bacos.app.ui.components.StatCard
import com.bacos.app.ui.theme.Bacos
import com.bacos.app.ui.theme.Body
import com.bacos.app.ui.theme.BodyMedium
import com.bacos.app.ui.theme.Caption
import com.bacos.app.ui.theme.H2
import com.bacos.app.ui.theme.H3
import com.bacos.app.ui.theme.Numbers
import com.bacos.app.ui.theme.Radius
import com.bacos.app.ui.theme.Spacing
import kotlinx.coroutines.launch
import org.json.JSONArray

/** LESSON — progressive disclosure: concept → rule → example → mistake → practice. */
@Composable
fun LessonScreen(db: AppDb, nav: NavHostController, lessonId: Long) {
    val scope = rememberCoroutineScope()
    var lesson by remember { mutableStateOf<com.bacos.app.data.db.LessonEntity?>(null) }
    var progress by remember { mutableStateOf<ProgressEntity?>(null) }
    var sections by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    var questionCount by remember { mutableStateOf(0) }
    var cardCount by remember { mutableStateOf(0) }
    var expanded by remember { mutableStateOf(-1) }

    LaunchedEffect(lessonId) {
        lesson = db.lessonDao().byId(lessonId)
        progress = db.progressDao().byLesson(lessonId)
        lesson?.let { l ->
            try {
                val arr = JSONArray(l.sectionsJson)
                sections = (0 until arr.length()).map { i ->
                    val o = arr.getJSONObject(i)
                    o.getString("t") to o.getString("b")
                }
            } catch (_: Exception) { }
            questionCount = db.questionDao().byLessons(listOf(lessonId)).size
            cardCount = db.flashcardDao().byLessons(listOf(lessonId)).size
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Bacos.c.bg)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.xl)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { nav.popBackStack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع", tint = Bacos.c.textPrimary)
            }
        }

        lesson?.let { l ->
            Text(l.titleAr, style = H2, color = Bacos.c.textPrimary)
            Spacer(Modifier.height(Spacing.xs))
            Text(l.summary, style = Caption, color = Bacos.c.textTertiary)
            Spacer(Modifier.height(Spacing.lg))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                StatCard("${progress?.mastery ?: 0}%", "الإتقان", modifier = Modifier.weight(1f), accent = Bacos.c.accent)
                StatCard("$questionCount", "أسئلة", modifier = Modifier.weight(1f), accent = Bacos.c.info)
                StatCard("$cardCount", "بطاقات", modifier = Modifier.weight(1f), accent = Bacos.c.ai)
            }

            Spacer(Modifier.height(Spacing.xl))

            sections.forEachIndexed { i, (title, body) ->
                SectionCard(index = i, title = title, body = body, expanded = expanded == i) {
                    expanded = if (expanded == i) -1 else i
                }
                Spacer(Modifier.height(Spacing.md))
            }

            Spacer(Modifier.height(Spacing.xl))

            PrimaryButton(
                text = "تدرب على هذا الدرس ($questionCount سؤال)",
                onClick = {
                    scope.launch {
                        // mark as started
                        db.progressDao().upsert(
                            ProgressEntity(
                                lessonId = lessonId,
                                mastery = progress?.mastery ?: 0,
                                status = if (progress?.status == "done") "done" else "learning",
                                lastStudiedAt = System.currentTimeMillis(),
                                attempts = (progress?.attempts ?: 0),
                                correct = progress?.correct ?: 0,
                            )
                        )
                    }
                    nav.navigate("quiz/lesson:$lessonId/10")
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = questionCount > 0,
            )
            if (questionCount == 0) {
                Spacer(Modifier.height(Spacing.xs))
                Text("لا توجد أسئلة لهذا الدرس بعد", style = Caption, color = Bacos.c.textTertiary, modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }

            Spacer(Modifier.height(Spacing.xl))
            PrimaryButton(
                text = "راجع بطاقات الدرس ($cardCount)",
                onClick = { nav.navigate(Routes.REVIEW) },
                modifier = Modifier.fillMaxWidth(),
                enabled = cardCount > 0,
            )
            Spacer(Modifier.height(Spacing.huge))
        }
    }
}

@Composable
private fun SectionCard(index: Int, title: String, body: String, expanded: Boolean, onClick: () -> Unit) {
    GlassCard(
        Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .animateContentSize()
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
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Bacos.c.accentDim),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("${index + 1}", style = Numbers, color = Bacos.c.accent)
                    }
                    Spacer(Modifier.width(Spacing.md))
                    Text(title, style = H3, color = Bacos.c.textPrimary)
                }
                Text(if (expanded) "−" else "+", style = H2, color = Bacos.c.textTertiary)
            }
            if (expanded) {
                Spacer(Modifier.height(Spacing.md))
                Text(body, style = Body, color = Bacos.c.textSecondary)
            }
        }
    }
}
