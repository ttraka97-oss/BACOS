package com.bacos.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.bacos.app.ui.Routes
import com.bacos.app.ui.components.GlassCard
import com.bacos.app.ui.theme.Bacos
import com.bacos.app.ui.theme.Body
import com.bacos.app.ui.theme.BodyMedium
import com.bacos.app.ui.theme.Caption
import com.bacos.app.ui.theme.H1
import com.bacos.app.ui.theme.Numbers
import com.bacos.app.ui.theme.Spacing

/** PRACTICE — hub: due reviews, quick adaptive quiz, mistakes book, simulator. */
@Composable
fun PracticeScreen(db: AppDb, nav: NavHostController) {
    var dueCount by remember { mutableStateOf(0) }
    var mistakesCount by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        dueCount = db.cardStateDao().dueCount(System.currentTimeMillis())
        mistakesCount = db.mistakeDao().open().size
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
        Text("تدرب", style = H1, color = Bacos.c.textPrimary)
        Text("الذكاء التكيفي يختار المستوى المناسب لك", style = Caption, color = Bacos.c.textTertiary)
        Spacer(Modifier.height(Spacing.xl))

        PracticeCard(
            title = "مراجعة البطاقات",
            subtitle = if (dueCount > 0) "$dueCount بطاقة مستحقة اليوم — spaced repetition" else "لا توجد بطاقات مستحقة حالياً",
            value = dueCount.toString(),
            enabled = dueCount > 0,
            onClick = { nav.navigate(Routes.REVIEW) },
        )
        PracticeCard(
            title = "اختبار سريع — رياضيات",
            subtitle = "أسئلة تكيفية حسب نقاط ضعفك",
            value = "10",
            onClick = { nav.navigate(Routes.quiz("math", 10)) },
        )
        PracticeCard(
            title = "اختبار سريع — فيزياء وكيمياء",
            subtitle = "RC، RLC، النووية والمعايرة",
            value = "10",
            onClick = { nav.navigate(Routes.quiz("phys", 10)) },
        )
        PracticeCard(
            title = "اختبار سريع — علوم الطبيعة",
            subtitle = "البروتين، التنسيق العصبي والمناعة",
            value = "10",
            onClick = { nav.navigate(Routes.quiz("svt", 10)) },
        )
        PracticeCard(
            title = "محاكاة الباك — نموذج مختصر",
            subtitle = "20 سؤالاً من كل المواد بتوقيت حقيقي",
            value = "20",
            onClick = { nav.navigate(Routes.quiz("all", 20)) },
        )
        PracticeCard(
            title = "دفتر الأخطاء",
            subtitle = if (mistakesCount > 0) "$mistakesCount خطأ يحتاج تصليح" else "نقي — لا أخطاء مفتوحة",
            value = mistakesCount.toString(),
            onClick = { nav.navigate(Routes.MISTAKES) },
        )

        Spacer(Modifier.height(Spacing.huge))
    }
}

@Composable
private fun PracticeCard(title: String, subtitle: String, value: String, enabled: Boolean = true, onClick: () -> Unit) {
    GlassCard(
        Modifier
            .fillMaxWidth()
            .padding(bottom = Spacing.md)
            .clickable { if (enabled) onClick() }
    ) {
        Row(
            Modifier.padding(Spacing.lg),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, style = BodyMedium, color = if (enabled) Bacos.c.textPrimary else Bacos.c.textTertiary)
                Spacer(Modifier.height(2.dp))
                Text(subtitle, style = Caption, color = Bacos.c.textTertiary)
            }
            Text(value, style = Numbers, color = if (enabled) Bacos.c.accent else Bacos.c.textTertiary)
        }
    }
}
