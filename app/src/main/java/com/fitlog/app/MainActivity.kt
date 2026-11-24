package com.fitlog.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.fitlog.app.ui.screens.DetailScreen
import com.fitlog.app.ui.screens.EditScreen
import com.fitlog.app.ui.screens.HomeScreen
import com.fitlog.app.ui.screens.ListScreen
import com.fitlog.app.ui.screens.SettingsScreen
import com.fitlog.app.ui.theme.*
import com.fitlog.app.vm.AppViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FitLogTheme {
                AppNav()
            }
        }
    }
}

sealed class Route(val path: String) {
    data object Home : Route("home")
    data object List : Route("list")
    data object Settings : Route("settings")
    data object Detail : Route("detail/{id}")
    data object Edit : Route("edit/{id}")
}

@Composable
fun AppNav() {
    val nav = rememberNavController()
    val vm: AppViewModel = viewModel()
    val backStack by nav.currentBackStackEntryAsState()
    val current = backStack?.destination?.route
    val isTop = current == Route.Home.path || current == Route.List.path || current == Route.Settings.path

    Scaffold(
        containerColor = FitTheme.colors.bg,
        floatingActionButton = {
            AnimatedVisibility(
                visible = isTop,
                enter = scaleIn(animationSpec = Motion.springy()) + fadeIn(Motion.standard(Motion.SHORT4)),
                exit = scaleOut(animationSpec = Motion.standard(Motion.SHORT3)) + fadeOut(Motion.standard(Motion.SHORT3))
            ) {
                FloatingActionButton(
                    onClick = { nav.navigate("edit/-1") },
                    containerColor = Primary,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(Radius.hero),
                    modifier = Modifier.size(Touch.fab)
                ) { Icon(Icons.Default.Add, contentDescription = "添加衣物") }
            }
        },
        bottomBar = {
            AnimatedVisibility(
                visible = isTop,
                enter = fadeIn(Motion.standard(Motion.SHORT4)),
                exit = fadeOut(Motion.standard(Motion.SHORT3))
            ) {
                NavigationBar(
                    containerColor = FitTheme.colors.surface,
                    tonalElevation = if (FitTheme.colors.isDark) 3.dp else 0.dp
                ) {
                    NavItem(nav, "首页", Route.Home.path, Icons.Default.Home, current)
                    NavItem(nav, "衣物", Route.List.path, Icons.Default.List, current)
                    NavItem(nav, "设置", Route.Settings.path, Icons.Default.Settings, current)
                }
            }
        }
    ) { pad ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(pad)
                .background(FitTheme.colors.bg)
        ) {
            NavHost(navController = nav, startDestination = Route.Home.path) {
                composable(Route.Home.path) {
                    HomeScreen(vm, onAdd = { nav.navigate("edit/-1") }, onOpenGarment = { nav.navigate("detail/$it") })
                }
                composable(Route.List.path) {
                    ListScreen(vm, onOpen = { nav.navigate("detail/$it") })
                }
                composable(Route.Settings.path) { SettingsScreen(vm) }
                composable(
                    Route.Detail.path,
                    arguments = listOf(navArgument("id") { type = NavType.LongType })
                ) { entry ->
                    val id = entry.arguments?.getLong("id") ?: 0L
                    DetailScreen(vm, id, onBack = { nav.popBackStack() }, onEdit = { nav.navigate("edit/$it") })
                }
                composable(
                    Route.Edit.path,
                    arguments = listOf(navArgument("id") { type = NavType.LongType })
                ) { entry ->
                    val id = entry.arguments?.getLong("id") ?: -1L
                    EditScreen(vm, id, onBack = { nav.popBackStack() })
                }
            }
        }
    }
}

@Composable
private fun RowScope.NavItem(
    nav: androidx.navigation.NavHostController,
    label: String,
    route: String,
    icon: ImageVector,
    current: String?
) {
    val selected = current == route
    NavigationBarItem(
        selected = selected,
        onClick = {
            nav.navigate(route) {
                popUpTo(Route.Home.path) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        },
        icon = { Icon(icon, contentDescription = label, modifier = Modifier.size(22.dp)) },
        label = { Text(label, fontSize = 10.sp) },
        colors = NavigationBarItemDefaults.colors(
            selectedIconColor = Primary,
            selectedTextColor = Primary,
            indicatorColor = if (FitTheme.colors.isDark) FitTheme.colors.surfaceVariant else PrimaryLight,
            unselectedIconColor = FitTheme.colors.textSub,
            unselectedTextColor = FitTheme.colors.textSub
        )
    )
}
