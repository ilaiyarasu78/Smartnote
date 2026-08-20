package com.ilaiyarasu.smartnote.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.StickyNote2
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ilaiyarasu.smartnote.ui.screens.AddEditNoteScreen
import com.ilaiyarasu.smartnote.ui.screens.NoteListScreen
import com.ilaiyarasu.smartnote.ui.screens.PinLockScreen
import com.ilaiyarasu.smartnote.ui.screens.SettingsScreen
import com.ilaiyarasu.smartnote.ui.theme.PinkAccent
import com.ilaiyarasu.smartnote.ui.theme.PurplePrimary
import com.ilaiyarasu.smartnote.util.PreferencesManager
import com.ilaiyarasu.smartnote.viewmodel.NoteViewModel

sealed class Screen(val route: String, val label: String = "", val icon: ImageVector? = null) {
    object PinLock : Screen("pin_lock")
    object PinSetup : Screen("pin_setup")
    object NoteList : Screen("note_list", "Notes", Icons.AutoMirrored.Filled.StickyNote2)
    object AddEditNote : Screen("add_edit_note?noteId={noteId}&templateId={templateId}", "Add", Icons.Default.Add) {
        fun createRoute(noteId: String? = null, templateId: String? = null): String {
            val params = mutableListOf<String>()
            if (noteId != null) params.add("noteId=$noteId")
            if (templateId != null) params.add("templateId=$templateId")
            return if (params.isEmpty()) "add_edit_note" else "add_edit_note?${params.joinToString("&")}"
        }
    }
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
    object NoteRead : Screen("note_read/{noteId}") {
        fun createRoute(noteId: String) = "note_read/$noteId"
    }
}

@Composable
fun SmartnoteNavGraph(
    navController: NavHostController = rememberNavController(),
    viewModel: NoteViewModel,
    preferencesManager: PreferencesManager
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // Show bottom bar for main screens
    val showBottomBar = currentDestination?.route in listOf(Screen.NoteList.route, Screen.Settings.route)

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                Box {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 8.dp
                    ) {
                        // Notes Item
                        NavigationBarItem(
                            icon = { Icon(Screen.NoteList.icon!!, contentDescription = Screen.NoteList.label) },
                            label = { Text(Screen.NoteList.label) },
                            selected = currentDestination?.hierarchy?.any { it.route == Screen.NoteList.route } == true,
                            onClick = {
                                navController.navigate(Screen.NoteList.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = PurplePrimary,
                                selectedTextColor = PurplePrimary,
                                indicatorColor = PurplePrimary.copy(alpha = 0.15f)
                            )
                        )

                        // Empty middle slot for FAB
                        NavigationBarItem(
                            icon = {},
                            label = {},
                            selected = false,
                            onClick = {},
                            enabled = false,
                            colors = NavigationBarItemDefaults.colors(
                                unselectedIconColor = Color.Transparent,
                                disabledIconColor = Color.Transparent
                            )
                        )

                        // Settings Item
                        NavigationBarItem(
                            icon = { Icon(Screen.Settings.icon!!, contentDescription = Screen.Settings.label) },
                            label = { Text(Screen.Settings.label) },
                            selected = currentDestination?.hierarchy?.any { it.route == Screen.Settings.route } == true,
                            onClick = {
                                navController.navigate(Screen.Settings.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = PurplePrimary,
                                selectedTextColor = PurplePrimary,
                                indicatorColor = PurplePrimary.copy(alpha = 0.15f)
                            )
                        )
                    }

                    // Floating gradient "+" button
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .offset(y = (-20).dp)
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(PinkAccent, PurplePrimary)))
                            .clickable {
                                navController.navigate(Screen.AddEditNote.createRoute())
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Add note",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        val startDestination = if (preferencesManager.isPinEnabled) {
            Screen.PinLock.route
        } else {
            Screen.NoteList.route
        }

        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.PinLock.route) {
                PinLockScreen(
                    preferencesManager = preferencesManager,
                    onUnlocked = {
                        navController.navigate(Screen.NoteList.route) {
                            popUpTo(Screen.PinLock.route) { inclusive = true }
                        }
                    }
                )
            }
            composable(Screen.PinSetup.route) {
                PinLockScreen(
                    preferencesManager = preferencesManager,
                    forceSetup = true,
                    onUnlocked = { navController.popBackStack() }
                )
            }
            composable(Screen.NoteList.route) {
                NoteListScreen(
                    viewModel = viewModel,
                    onNoteClick = { noteId ->
                        navController.navigate(Screen.NoteRead.createRoute(noteId))
                    },
                    onAddClick = { templateId ->
                        navController.navigate(Screen.AddEditNote.createRoute(templateId = templateId))
                    }
                )
            }
            composable(
                route = Screen.AddEditNote.route,
                arguments = listOf(
                    navArgument("noteId") { type = NavType.StringType; nullable = true },
                    navArgument("templateId") { type = NavType.StringType; nullable = true }
                )
            ) { backStackEntry ->
                val noteId = backStackEntry.arguments?.getString("noteId")
                val templateId = backStackEntry.arguments?.getString("templateId")
                AddEditNoteScreen(
                    viewModel = viewModel,
                    noteId = noteId,
                    templateId = templateId,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.Settings.route) {
                SettingsScreen(
                    preferencesManager = preferencesManager,
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onChangePinClick = { navController.navigate(Screen.PinSetup.route) }
                )
            }
            composable(
                route = Screen.NoteRead.route,
                arguments = listOf(navArgument("noteId") { type = NavType.StringType })
            ) { backStackEntry ->
                val noteId = backStackEntry.arguments?.getString("noteId") ?: return@composable
                com.ilaiyarasu.smartnote.ui.screens.NoteReadScreen(
                    viewModel = viewModel,
                    noteId = noteId,
                    onBack = { navController.popBackStack() },
                    onEditClick = { id ->
                        navController.navigate(Screen.AddEditNote.createRoute(noteId = id))
                    }
                )
            }
        }
    }
}
