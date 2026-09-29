package com.bacos.app.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextAlign
import com.bacos.app.data.db.AppDb
import com.bacos.app.data.db.ProfileEntity
import com.bacos.app.data.seed.DemoSeeder
import com.bacos.app.ui.components.GlassCard
import com.bacos.app.ui.components.PrimaryButton
import com.bacos.app.ui.components.ReadinessRing
import com.bacos.app.ui.components.GhostButton
import com.bacos.app.ui.theme.Bacos
import com.bacos.app.ui.theme.Body
import com.bacos.app.ui.theme.BodyMedium
import com.bacos.app.ui.theme.BodySmall
import com.bacos.app.ui.theme.Caption
import com.bacos.app.ui.theme.Display
import com.bacos.app.ui.theme.H2
import com.bacos.app.ui.theme.Spacing
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * First-run experience: welcome → branch → daily time → demo → plan generation.
 * Designed so the student reaches a living dashboard in under 60 seconds.
 */
@Composable
fun OnboardingScreen(db: AppDb, onDone: () -> Unit) {
    val scope = rememberCoroutineScope()
    var step by remember { mutableIntStateOf(0) }
    var branch by remember { mutableIntStateOf(-1) }
    var minutes by remember { mutableIntStateOf(60) }
    var demo by remember { mutableIntStateOf(-1) }
    var generating by remember { mutableIntStateOf(0) } // 0..100 progress

    val totalSteps = 4

    val branches = listOf(
        "علوم تجريبية" to "الشعبة الأكثر انتشاراً — رياضيات، فيزياء، SVT",
        "رياضيات" to "الشعبة الأقوى في التحليل والهندسة",
        "تقني رياضي" to "رياضيات متقدمة + تكنولوجيا",
        "تسيير واقتصاد" to "اقتصاد، محاسبة ورياضيات مالية",
        "آداب وفلسفة" to "فلسفة، لغات وتاريخ",
    )

    // ── Plan generation animation ──
    LaunchedEffect(step) {
        if (step == totalSteps) {
            while (generating < 100) {
                delay(40)
                generating = (generating + 4).coerceAtMost(100)
            }
            delay(700)
            // Persist profile + optional demo data
            scope.launch {
                val profile = ProfileEntity(
                    branch = when (branch) {
                        0 -> "sciences"; 1 -> "math"; 2 -> "techmath"; 3 -> "gestion"; else -> "letters"
                    },
                    dailyMinutes = minutes,
                    examDate = System.currentTimeMillis() + 220L * 24 * 60 * 60 * 1000, // ~BAC in 220 days
                    onboarded = true,
                    demoMode = demo == 1,
                    streak = if (demo == 1) 6 else 0,
                )
                db.profileDao().upsert(profile)
                if (demo == 1) DemoSeeder.seed(db)
            }.invokeOnCompletion {
                onDone()
            }
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(Bacos.c.bg, Bacos.c.surface1, Bacos.c.bg))
            )
            .systemBarsPadding()
            .padding(horizontal = Spacing.xl)
    ) {
        Column(Modifier.fillMaxSize()) {

            Spacer(Modifier.height(Spacing.huge))

            AnimatedContent(
                targetState = step,
                transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(200)) },
                label = "onboarding"
            ) { s ->
                when (s) {
                    0 -> Column {
                        Text("BACOS", style = Display, color = Bacos.c.textPrimary)
                        Spacer(Modifier.height(Spacing.md))
                        Text("نظام الباك الذكي", style = H2, color = Bacos.c.accent)
                        Spacer(Modifier.height(Spacing.lg))
                        Text(
                            "ماشي تطبيق دروس — مساعد شخصي يعرف مستواك، يتتبع تقدمك، ويقرر معك شنو تدير دابا.",
                            style = Body,
                            color = Bacos.c.textSecondary,
                        )
                        Spacer(Modifier.height(Spacing.xxl))
                    }
                    1 -> Column {
                        Text("اختر شعبتك", style = Display, color = Bacos.c.textPrimary)
                        Spacer(Modifier.height(Spacing.md))
                        Text("باش نظبط المحتوى والخطة على مقاسك", style = Body, color = Bacos.c.textSecondary)
                        Spacer(Modifier.height(Spacing.xl))
                        branches.forEachIndexed { i, (name, desc) ->
                            BranchOption(name, desc, selected = branch == i) { branch = i }
                            Spacer(Modifier.height(Spacing.md))
                        }
                    }
                    2 -> Column {
                        Text("قداش من وقت يومياً؟", style = Display, color = Bacos.c.textPrimary)
                        Spacer(Modifier.height(Spacing.md))
                        Text("باش نقسمو الجلسات على قدك", style = Body, color = Bacos.c.textSecondary)
                        Spacer(Modifier.height(Spacing.xxl))
                        listOf(30, 60, 90, 120).forEach { m ->
                            TimeOption(m, selected = minutes == m) { minutes = m }
                            Spacer(Modifier.height(Spacing.md))
                        }
                    }
                    3 -> Column {
                        Text("بلاصة البداية", style = Display, color = Bacos.c.textPrimary)
                        Spacer(Modifier.height(Spacing.md))
                        Text("تحب تبدا من الصفر ولا تشوف النظام مليان ببيانات تلميذ نموذجي؟", style = Body, color = Bacos.c.textSecondary)
                        Spacer(Modifier.height(Spacing.xl))
                        GlassCard(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(Spacing.lg)) {
                                Text("بدية نظيفة", style = BodyMedium, color = Bacos.c.textPrimary)
                                Spacer(Modifier.height(Spacing.xs))
                                Text("كلشي يبدا من الصفر — جداول فارغة وبطاقات جديدة", style = BodySmall, color = Bacos.c.textTertiary)
                                Spacer(Modifier.height(Spacing.lg))
                                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                    GhostButton("تبدي من الصفر", onClick = { demo = 0; step = 4 }, tint = Bacos.c.textPrimary)
                                }
                            }
                        }
                        Spacer(Modifier.height(Spacing.md))
                        GlassCard(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(Spacing.lg)) {
                                Text("تجربة سريعة 🚀", style = BodyMedium, color = Bacos.c.accent)
                                Spacer(Modifier.height(Spacing.xs))
                                Text("بيانات تلميذ حقيقي: مستويات، بطاقات جاهزة للمراجعة، أخطاء وجلسات", style = BodySmall, color = Bacos.c.textTertiary)
                                Spacer(Modifier.height(Spacing.lg))
                                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                    PrimaryButton("شوف النظام حيّ", onClick = { demo = 1; step = 4 }, modifier = Modifier.fillMaxWidth())
                                }
                            }
                        }
                    }
                    else -> Column(
                        Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(Modifier.height(Spacing.huge))
                        ReadinessRing(score = generating, subtitle = "بناء خطتك")
                        Spacer(Modifier.height(Spacing.xl))
                        Text(
                            when {
                                generating < 40 -> "تحليل المنهج…"
                                generating < 80 -> "ضبط جدول المراجعة…"
                                else -> "تجهيز البطاقات…"
                            },
                            style = Body, color = Bacos.c.textSecondary, textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            // ── Bottom controls ──
            if (step in 1..2) {
                PrimaryButton(
                    text = if (step == 1) "التالي" else "التالي",
                    onClick = { if (step == 1 && branch >= 0) step = 2 else if (step == 2) step = 3 },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = if (step == 1) branch >= 0 else true,
                )
                Spacer(Modifier.height(Spacing.md))
                Text(
                    "$step / ${totalSteps - 1}",
                    style = Caption, color = Bacos.c.textTertiary,
                    modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
                )
            } else if (step == 0) {
                PrimaryButton("يلا نبداو", onClick = { step = 1 }, modifier = Modifier.fillMaxWidth())
            }
            Spacer(Modifier.height(Spacing.xl))
        }
    }
}

@Composable
private fun BranchOption(name: String, desc: String, selected: Boolean, onClick: () -> Unit) {
    GlassCard(Modifier.fillMaxWidth().clickable { onClick() }) {
        Column(Modifier.padding(Spacing.lg)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(name, style = BodyMedium, color = if (selected) Bacos.c.accent else Bacos.c.textPrimary)
                if (selected) Text("✓", style = BodyMedium, color = Bacos.c.accent)
            }
            Spacer(Modifier.height(Spacing.xs))
            Text(desc, style = Caption, color = Bacos.c.textTertiary)
        }
    }
}

@Composable
private fun TimeOption(m: Int, selected: Boolean, onClick: () -> Unit) {
    GlassCard(Modifier.fillMaxWidth().clickable { onClick() }) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(Spacing.lg),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("${m} دقيقة", style = BodyMedium, color = if (selected) Bacos.c.accent else Bacos.c.textPrimary)
            if (selected) Text("✓", style = BodyMedium, color = Bacos.c.accent)
        }
    }
}
