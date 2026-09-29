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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.bacos.app.ai.AiProvider
import com.bacos.app.ai.AIService
import com.bacos.app.data.db.AppDb
import com.bacos.app.ui.Routes
import com.bacos.app.ui.components.GlassCard
import com.bacos.app.ui.theme.Bacos
import com.bacos.app.ui.theme.Body
import com.bacos.app.ui.theme.BodySmall
import com.bacos.app.ui.theme.Caption
import com.bacos.app.ui.theme.H1
import com.bacos.app.ui.theme.Radius
import com.bacos.app.ui.theme.Spacing
import kotlinx.coroutines.launch

private data class ChatMsg(
    val fromUser: Boolean,
    val text: String,
    val actions: List<AIService.AiAction> = emptyList(),
)

/**
 * BAC AI — the tutor. Not a chatbot shell: replies come from the AI
 * abstraction layer (AiProvider) and carry real actions.
 */
@Composable
fun AiScreen(db: AppDb, nav: NavHostController) {
    val scope = rememberCoroutineScope()
    val ai = remember { AiProvider.get() }

    var input by remember { mutableStateOf("") }
    var thinking by remember { mutableStateOf(false) }
    var dueCount by remember { mutableStateOf(0) }
    var mistakesCount by remember { mutableStateOf(0) }
    var readiness by remember { mutableStateOf(0) }
    val messages = remember { mutableStateListOf<ChatMsg>() }
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) {
        dueCount = db.cardStateDao().dueCount(System.currentTimeMillis())
        mistakesCount = db.mistakeDao().open().size
        val progresses = db.progressDao().all()
        readiness = if (progresses.isEmpty()) 0 else progresses.map { it.mastery }.average().toInt()
        if (messages.isEmpty()) {
            messages.add(
                ChatMsg(
                    false,
                    "أهلاً بك 👋 أنا BAC AI — أعرف مستواك الحقيقي من متابعة نظام BACOS لنشاطك.\nاسألني أي شيء، أو اختر من الاقتراحات.",
                )
            )
        }
    }

    fun send(text: String) {
        val msg = text.trim()
        if (msg.isEmpty() || thinking) return
        messages.add(ChatMsg(true, msg))
        input = ""
        thinking = true
        scope.launch {
            val reply = ai.respond(
                userMessage = msg,
                mode = AIService.Mode.EXPLAIN,
                context = AIService.Context(
                    activeLessonTitle = null,
                    dueReviews = dueCount,
                    openMistakes = mistakesCount,
                    weakestSubject = "الفيزياء والكيمياء",
                    readinessScore = readiness,
                ),
            )
            messages.add(ChatMsg(false, reply.text, reply.actions))
            thinking = false
        }
    }

    fun runAction(action: AIService.AiAction) {
        when {
            action.key == "mistakes" -> nav.navigate(Routes.MISTAKES)
            action.key == "review" || action.key == "session" -> nav.navigate(Routes.REVIEW)
            action.key.startsWith("quiz:") -> {
                val topic = action.key.removePrefix("quiz:")
                val code = when {
                    topic.contains("فيز") -> "phys"
                    topic.contains("علوم") -> "svt"
                    else -> "math"
                }
                nav.navigate(Routes.quiz(code, 8))
            }
            action.key.startsWith("lesson:") -> nav.navigate(Routes.LEARN)
            action.key == "progress" -> nav.navigate(Routes.PROFILE)
            else -> nav.navigate(Routes.PRACTICE)
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Bacos.c.bg)
            .statusBarsPadding()
            .imePadding()
    ) {
        Spacer(Modifier.height(Spacing.lg))
        Row(
            Modifier.padding(horizontal = Spacing.xl),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Bacos.c.aiDim),
                contentAlignment = Alignment.Center
            ) { Text("🤖", style = Body) }
            Spacer(Modifier.width(Spacing.md))
            Column {
                Text("BAC AI", style = H1, color = Bacos.c.ai)
                Text(ai.name, style = Caption, color = Bacos.c.textTertiary)
            }
        }
        Spacer(Modifier.height(Spacing.lg))

        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = Spacing.xl),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            items(messages) { m -> MessageBubble(m) { runAction(it) } }
            if (thinking) {
                item {
                    GlassCard(Modifier.fillMaxWidth(0.5f)) {
                        Text("يفكر…", style = BodySmall, color = Bacos.c.textTertiary, modifier = Modifier.padding(Spacing.md))
                    }
                }
            }
        }

        LaunchedEffect(messages.size, thinking) {
            if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
        }

        // ── Suggestion chips (while conversation is fresh) ──
        if (messages.size <= 1) {
            LazyRow(
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = Spacing.xl),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                modifier = Modifier.padding(vertical = Spacing.sm)
            ) {
                val suggestions = listOf(
                    "اختبرني في الدوال", "راجع أخطائي", "ماذا أراجع اليوم؟", "عندي ساعة فقط", "وين نقاط قوتي؟",
                )
                items(suggestions) { sug ->
                    Box(
                        Modifier
                            .clip(Radius.pill)
                            .background(Bacos.c.surface2)
                            .clickable { send(sug) }
                            .padding(horizontal = Spacing.lg, vertical = Spacing.md)
                    ) { Text(sug, style = BodySmall, color = Bacos.c.ai) }
                }
            }
        }

        // ── Input bar ──
        Row(
            Modifier
                .fillMaxWidth()
                .background(Bacos.c.surface1)
                .padding(horizontal = Spacing.lg, vertical = Spacing.md)
        ) {
            Box(
                Modifier
                    .weight(1f)
                    .clip(Radius.pill)
                    .background(Bacos.c.surface2)
                    .padding(horizontal = Spacing.lg, vertical = Spacing.md)
            ) {
                if (input.isEmpty()) {
                    Text("اكتب سؤالك…", style = Body, color = Bacos.c.textTertiary)
                }
                BasicTextField(
                    value = input,
                    onValueChange = { input = it },
                    textStyle = Body.copy(color = Bacos.c.textPrimary),
                    cursorBrush = SolidColor(Bacos.c.accent),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Spacer(Modifier.width(Spacing.md))
            Box(
                Modifier
                    .clip(CircleShape)
                    .background(Bacos.c.accent)
                    .clickable { send(input) }
                    .padding(Spacing.md),
                contentAlignment = Alignment.Center
            ) { Text("↑", style = H1, color = Bacos.c.bg) }
        }
    }
}

@Composable
private fun MessageBubble(msg: ChatMsg, onAction: (AIService.AiAction) -> Unit) {
    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = if (msg.fromUser) Alignment.End else Alignment.Start
    ) {
        Box(
            Modifier
                .widthIn(max = 320.dp)
                .clip(Radius.lg)
                .background(if (msg.fromUser) Bacos.c.accentDim else Bacos.c.surface1)
        ) {
            Column(Modifier.padding(Spacing.lg)) {
                Text(msg.text, style = Body, color = Bacos.c.textPrimary)
                if (msg.actions.isNotEmpty()) {
                    Spacer(Modifier.height(Spacing.md))
                    msg.actions.forEach { a ->
                        Text(
                            a.label,
                            style = BodySmall,
                            color = Bacos.c.accent,
                            modifier = Modifier
                                .clip(Radius.md)
                                .clickable { onAction(a) }
                                .padding(vertical = Spacing.sm)
                        )
                    }
                }
            }
        }
    }
}
