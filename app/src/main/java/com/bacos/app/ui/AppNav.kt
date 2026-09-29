package com.bacos.app.ui

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.bacos.app.data.db.AppDb
import com.bacos.app.data.seed.SeedData
import com.bacos.app.ui.screens.AiScreen
import com.bacos.app.ui.screens.HomeScreen
import com.bacos.app.ui.screens.LearnScreen
import com.bacos.app.ui.screens.LessonScreen
import com.bacos.app.ui.screens.MistakesScreen
import com.bacos.app.ui.screens.OnboardingScreen
import com.bacos.app.ui.screens.PracticeScreen
import com.bacos.app.ui.screens.ProfileScreen
import com.bacos.app.ui.screens.QuizScreen
import com.bacos.app.ui.screens.ReviewScreen
import com.bacos.app.ui.theme.Bacos
import com.bacos.app.ui.theme.Caption
import kotlinx.coroutines.launch

object Routes {
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val LEARN = "learn"
    const val PRACTICE = "practice"
    const val AI = "ai"
    const val PROFILE = "profile"
    const val LESSON = "lesson/{lessonId}"
    const val QUIZ = "quiz/{subject}/{count}"
    const val REVIEW = "review"
    const val MISTAKES = "mistakes"

    fun lesson(id: Long) = "lesson/$id"
    fun quiz(subject: String, count: Int) = "quiz/$subject/$count"
}

private class Tab(
    val key: String,
    val route: String,
    val label: String,
    val icon: ImageVector,
    val iconActive: ImageVector,
)

@Composable
fun AppRoot() {
    val context = LocalContext.current
    val db = remember { AppDb.get(context) }
    val nav = rememberNavController()

    var onboarded by remember { mutableStateOf<Boolean?>(null) }

    LaunchedEffect(Unit) {
        SeedData.seed(db)
        onboarded = db.profileDao().get()?.onboarded == true
    }

    when (onboarded) {
        null -> Box(Modifier.fillMaxSize().background(Bacos.c.bg))
        false -> OnboardingScreen(db = db, onDone = { onboarded = true })
        true -> MainShell(db = db, nav = nav)
    }
}

@Composable
private fun MainShell(db: AppDb, nav: NavHostController) {
    val tabs = remember {
        listOf(
            Tab("home", Routes.HOME, "الرئيسية", Icons.Outlined.Home, Icons.Filled.Home),
            Tab("learn", Routes.LEARN, "تعلم", Icons.Outlined.School, Icons.Filled.School),
            Tab("practice", Routes.PRACTICE, "تدرب", Icons.Outlined.PlayArrow, Icons.Filled.PlayArrow),
            Tab("ai", Routes.AI, "BAC AI", Icons.Outlined.SmartToy, Icons.Filled.SmartToy),
            Tab("profile", Routes.PROFILE, "حسابي", Icons.Outlined.Person, Icons.Filled.Person),
        )
    }

    val backstack by nav.currentBackStackEntryAsState()
    val currentRoute = backstack?.destination?.route

    val showBar = currentRoute in tabs.map { it.route }

    Scaffold(
        containerColor = Bacos.c.bg,
        bottomBar = {
            if (showBar) {
                NavigationBar(containerColor = Bacos.c.surface1, tonalElevation = 0.dp) {
                    tabs.forEach { tab ->
                        val selected = currentRoute == tab.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                nav.navigate(tab.route) {
                                    popUpTo(Routes.HOME) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(if (selected) tab.iconActive else tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label, style = Caption, fontSize = 10.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Bacos.c.accent,
                                selectedTextColor = Bacos.c.accent,
                                unselectedIconColor = Bacos.c.textTertiary,
                                unselectedTextColor = Bacos.c.textTertiary,
                                indicatorColor = Bacos.c.accentDim,
                            ),
                        )
                    }
                }
            }
        },
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .background(Bacos.c.bg)
                .padding(padding)
        ) {
            NavHost(
                navController = nav,
                startDestination = Routes.HOME,
                enterTransition = { fadeIn(tween(220)) },
                exitTransition = { fadeOut(tween(180)) },
            ) {
                composable(Routes.HOME) { HomeScreen(db, nav) }
                composable(Routes.LEARN) { LearnScreen(db, nav) }
                composable(Routes.PRACTICE) { PracticeScreen(db, nav) }
                composable(Routes.AI) { AiScreen(db, nav) }
                composable(Routes.PROFILE) { ProfileScreen(db, nav) }
                composable(Routes.LESSON) { entry ->
                    val id = entry.arguments?.getString("lessonId")?.toLongOrNull() ?: 0L
                    LessonScreen(db, nav, id)
                }
                composable(Routes.QUIZ) { entry ->
                    val subject = entry.arguments?.getString("subject") ?: ""
                    val count = entry.arguments?.getString("count")?.toIntOrNull() ?: 10
                    QuizScreen(db, nav, subject, count)
                }
                composable(Routes.REVIEW) { ReviewScreen(db, nav) }
                composable(Routes.MISTAKES) { MistakesScreen(db, nav) }
            }
        }
    }
}
