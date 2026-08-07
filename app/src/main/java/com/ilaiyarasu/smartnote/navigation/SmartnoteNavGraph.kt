package com.ilaiyarasu.smartnote.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ilaiyarasu.smartnote.ui.screens.AddEditNoteScreen
import com.ilaiyarasu.smartnote.ui.screens.NoteListScreen
import com.ilaiyarasu.smartnote.ui.screens.PinLockScreen
import com.ilaiyarasu.smartnote.ui.screens.SettingsScreen
import com.ilaiyarasu.smartnote.util.PreferencesManager
import com.ilaiyarasu.smartnote.viewmodel.NoteViewModel

sealed class Screen(val route: String) {
    object PinLock : Screen("pin_lock")
    object PinSetup : Screen("pin_setup")
    object NoteList : Screen("note_list")
    object AddEditNote : Screen("add_edit_note?noteId={noteId}&templateId={templateId}") {
        fun createRoute(noteId: String? = null, templateId: String? = null): String {
            val params = mutableListOf<String>()
            if (noteId != null) params.add("noteId=$noteId")
            if (templateId != null) params.add("templateId=$templateId")
            return if (params.isEmpty()) "add_edit_note" else "add_edit_note?${params.joinToString("&")}"
        }
    }
    object Settings : Screen("settings")
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
    val startDestination = if (preferencesManager.isPinEnabled) {
        Screen.PinLock.route
    } else {
        Screen.NoteList.route
    }

    NavHost(navController = navController, startDestination = startDestination) {
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
                },
                onSettingsClick = {
                    navController.navigate(Screen.Settings.route)
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
