package com.njjpro.brainbox

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.njjpro.brainbox.data.Category
import com.njjpro.brainbox.notify.Notifications
import com.njjpro.brainbox.ui.BrainboxTheme
import com.njjpro.brainbox.ui.BrainboxViewModel
import com.njjpro.brainbox.ui.ExpenseScreen
import com.njjpro.brainbox.ui.HomeScreen
import com.njjpro.brainbox.ui.NoteEditorScreen
import com.njjpro.brainbox.ui.NoteListScreen
import com.njjpro.brainbox.ui.ReminderScreen
import com.njjpro.brainbox.ui.SettingsScreen

private object Routes {
    const val HOME = "home"
    const val REMINDERS = "reminders"
    const val EXPENSES = "expenses"
    const val SETTINGS = "settings"
    const val LIST = "list/{categoryId}"
    const val EDITOR = "editor/{categoryId}/{noteId}"

    fun list(category: Category) = "list/${category.id}"
    fun newNote(category: Category) = "editor/${category.id}/0"
    fun editNote(category: Category, noteId: Long) = "editor/${category.id}/$noteId"
}

class MainActivity : ComponentActivity() {

    private val vm: BrainboxViewModel by viewModels()
    private var launchNoteId by mutableStateOf<Long?>(null)

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        launchNoteId = noteIdFrom(intent)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !Notifications.hasPermission(this)) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            BrainboxTheme {
                val navController = rememberNavController()
                val notes by vm.notes.collectAsState()

                LaunchedEffect(launchNoteId, notes) {
                    val id = launchNoteId ?: return@LaunchedEffect
                    val note = notes.firstOrNull { it.id == id } ?: return@LaunchedEffect
                    navController.navigate(Routes.editNote(note.categoryEnum, note.id))
                    launchNoteId = null
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    NavHost(navController = navController, startDestination = Routes.HOME) {
                        composable(Routes.HOME) {
                            HomeScreen(
                                vm = vm,
                                onOpenCategory = { navController.navigate(Routes.list(it)) },
                                onOpenReminders = { navController.navigate(Routes.REMINDERS) },
                                onOpenExpenses = { navController.navigate(Routes.EXPENSES) },
                                onOpenNote = { id ->
                                    val note = notes.firstOrNull { it.id == id } ?: return@HomeScreen
                                    navController.navigate(Routes.editNote(note.categoryEnum, note.id))
                                },
                                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                            )
                        }

                        composable(
                            route = Routes.LIST,
                            arguments = listOf(navArgument("categoryId") { type = NavType.StringType }),
                        ) { entry ->
                            val category = Category.of(entry.arguments?.getString("categoryId").orEmpty())
                            NoteListScreen(
                                vm = vm,
                                category = category,
                                onBack = { navController.popBackStack() },
                                onOpenNote = { id -> navController.navigate(Routes.editNote(category, id)) },
                                onNewNote = { navController.navigate(Routes.newNote(category)) },
                            )
                        }

                        composable(
                            route = Routes.EDITOR,
                            arguments = listOf(
                                navArgument("categoryId") { type = NavType.StringType },
                                navArgument("noteId") { type = NavType.LongType },
                            ),
                        ) { entry ->
                            val category = Category.of(entry.arguments?.getString("categoryId").orEmpty())
                            val noteId = entry.arguments?.getLong("noteId") ?: 0L
                            NoteEditorScreen(
                                vm = vm,
                                noteId = noteId,
                                category = category,
                                onBack = { navController.popBackStack() },
                            )
                        }

                        composable(Routes.REMINDERS) {
                            ReminderScreen(
                                vm = vm,
                                onBack = { navController.popBackStack() },
                                onOpenNote = { id ->
                                    val note = notes.firstOrNull { it.id == id }
                                    val category = note?.categoryEnum ?: Category.REMINDER
                                    navController.navigate(Routes.editNote(category, id))
                                },
                                onNewReminder = { navController.navigate(Routes.newNote(Category.REMINDER)) },
                            )
                        }

                        composable(Routes.EXPENSES) {
                            ExpenseScreen(vm = vm, onBack = { navController.popBackStack() })
                        }

                        composable(Routes.SETTINGS) {
                            SettingsScreen(vm = vm, onBack = { navController.popBackStack() })
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        noteIdFrom(intent)?.let { launchNoteId = it }
    }

    private fun noteIdFrom(intent: Intent?): Long? {
        val id = intent?.getLongExtra(EXTRA_NOTE_ID, -1L) ?: -1L
        return if (id > 0) id else null
    }

    companion object {
        const val EXTRA_NOTE_ID = "noteId"
    }
}
