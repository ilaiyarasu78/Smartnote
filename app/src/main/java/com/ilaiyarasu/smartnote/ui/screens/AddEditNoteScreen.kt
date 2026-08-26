package com.ilaiyarasu.smartnote.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ilaiyarasu.smartnote.data.Note
import com.ilaiyarasu.smartnote.data.noteTemplates
import com.ilaiyarasu.smartnote.ui.components.LanguagePickerDropdown
import com.ilaiyarasu.smartnote.util.ReminderScheduler
import com.ilaiyarasu.smartnote.util.TextToSpeechHelper
import com.ilaiyarasu.smartnote.util.TranslationHelper
import com.ilaiyarasu.smartnote.util.supportedAppLanguages
import com.ilaiyarasu.smartnote.viewmodel.NoteViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date

private val categories = listOf("Study", "Work", "Personal")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditNoteScreen(
    viewModel: NoteViewModel,
    noteId: String?,
    templateId: String? = null,
    onBack: () -> Unit
) {
    var existingNote by remember { mutableStateOf<Note?>(null) }
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(categories.first()) }
    var categoryMenuExpanded by remember { mutableStateOf(false) }

    var reminderTime by remember { mutableStateOf<Long?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var pendingDateMillis by remember { mutableStateOf<Long?>(null) }

    var sourceLang by remember { mutableStateOf(supportedAppLanguages[0]) } // English
    var targetLang by remember { mutableStateOf(supportedAppLanguages[1]) } // Tamil
    var isTranslating by remember { mutableStateOf(false) }
    var translatedResult by remember { mutableStateOf<String?>(null) }
    var translateError by remember { mutableStateOf<String?>(null) }
    var showReadAloudMenu by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is NoteViewModel.UiEvent.NoteSaved -> {
                    android.widget.Toast.makeText(context, "Note saved", android.widget.Toast.LENGTH_SHORT).show()
                }
                is NoteViewModel.UiEvent.Error -> {
                    android.widget.Toast.makeText(context, event.message, android.widget.Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    val currentLocale = androidx.compose.ui.platform.LocalConfiguration.current.locales[0]
    val ttsHelper = remember { TextToSpeechHelper(context) }
    val isSpeaking by ttsHelper.isSpeaking.collectAsState()
    val ttsError by ttsHelper.error.collectAsState()

    DisposableEffect(Unit) {
        onDispose { ttsHelper.destroy() }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* Permission handled by system */ }

    LaunchedEffect(Unit) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = androidx.core.content.ContextCompat.checkSelfPermission(
                context, android.Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            if (!hasPermission) {
                notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    LaunchedEffect(noteId, templateId) {
        if (noteId != null) {
            val note = viewModel.getNoteById(noteId)
            note?.let {
                existingNote = it
                title = it.title
                content = it.content
                category = it.category
                reminderTime = it.reminderTime
            }
        } else if (templateId != null) {
            noteTemplates.find { it.id == templateId }?.let { template ->
                title = template.titleSuggestion
                content = template.contentTemplate
                category = template.suggestedCategory
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (noteId == null) "New Note" else "Edit Note") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (existingNote != null) {
                        IconButton(onClick = {
                            coroutineScope.launch {
                                viewModel.deleteNote(existingNote!!)
                                onBack()
                            }
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete")
                        }
                    }
                    IconButton(
                        onClick = {
                            if (title.isBlank() && content.isBlank()) {
                                onBack()
                                return@IconButton
                            }
                            
                            coroutineScope.launch {
                                val savedNoteId = if (existingNote != null) {
                                    viewModel.updateNote(existingNote!!, title, content, category, reminderTime)
                                    existingNote!!.id
                                } else {
                                    viewModel.addNote(title, content, category, reminderTime)
                                }

                                reminderTime?.let { time ->
                                    ReminderScheduler.scheduleReminder(context, savedNoteId, title, content, time)
                                } ?: run {
                                    existingNote?.let { ReminderScheduler.cancelReminder(context, it.id) }
                                }
                                
                                onBack()
                            }
                        }
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Save")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                textStyle = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold
                ),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            ExposedDropdownMenuBox(
                expanded = categoryMenuExpanded,
                onExpandedChange = { categoryMenuExpanded = it }
            ) {
                OutlinedTextField(
                    value = category,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Category") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryMenuExpanded)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled = true)
                )
                ExposedDropdownMenu(
                    expanded = categoryMenuExpanded,
                    onDismissRequest = { categoryMenuExpanded = false }
                ) {
                    categories.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                category = option
                                categoryMenuExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                label = { Text("Content") },
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    lineHeight = 26.sp
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .defaultMinSize(minHeight = 200.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LanguagePickerDropdown(
                    label = "From",
                    selected = sourceLang,
                    onSelect = { sourceLang = it },
                    modifier = Modifier.weight(1f)
                )
                LanguagePickerDropdown(
                    label = "To",
                    selected = targetLang,
                    onSelect = { targetLang = it },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
            ) {
                TextButton(
                    onClick = {
                        isTranslating = true
                        translateError = null
                        coroutineScope.launch {
                            TranslationHelper.ensureModelDownloaded(
                                sourceLang.mlKitCode,
                                targetLang.mlKitCode
                            ).fold(
                                onSuccess = {
                                    TranslationHelper.translate(
                                        content,
                                        sourceLang.mlKitCode,
                                        targetLang.mlKitCode
                                    ).fold(
                                        onSuccess = { translatedResult = it },
                                        onFailure = { translateError = "Translation failed: ${it.message}" }
                                    )
                                },
                                onFailure = { translateError = "Couldn't download language model. Check your connection." }
                            )
                            isTranslating = false
                        }
                    },
                    enabled = content.isNotBlank() && !isTranslating
                ) {
                    if (isTranslating) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text("Translate")
                }

                Box {
                    TextButton(onClick = {
                        if (isSpeaking) ttsHelper.stop() else showReadAloudMenu = true
                    }) {
                        Text(if (isSpeaking) "Stop" else "Speak")
                    }
                    DropdownMenu(
                        expanded = showReadAloudMenu,
                        onDismissRequest = { showReadAloudMenu = false }
                    ) {
                        supportedAppLanguages.forEach { lang ->
                            DropdownMenuItem(
                                text = { Text(lang.label) },
                                onClick = {
                                    showReadAloudMenu = false
                                    ttsHelper.speak(content, lang.ttsTag)
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(onClick = { showDatePicker = true }) {
                    Text(
                        if (reminderTime != null)
                            "Reminder: ${SimpleDateFormat("dd MMM, hh:mm a", currentLocale).format(Date(reminderTime!!))}"
                        else
                            "Set Reminder"
                    )
                }

                if (reminderTime != null) {
                    IconButton(onClick = {
                        reminderTime = null
                        existingNote?.let { ReminderScheduler.cancelReminder(context, it.id) }
                    }) {
                        Icon(Icons.Default.Close, contentDescription = "Remove reminder")
                    }
                }
            }

            translateError?.let {
                Spacer(modifier = Modifier.height(4.dp))
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            ttsError?.let {
                Spacer(modifier = Modifier.height(4.dp))
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        }
    }

    translatedResult?.let { result ->
        AlertDialog(
            onDismissRequest = { translatedResult = null },
            title = { Text("${targetLang.label} Translation") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(result)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    content = result
                    translatedResult = null
                }) { Text("Replace note") }
            },
            dismissButton = {
                TextButton(onClick = { translatedResult = null }) { Text("Close") }
            }
        )
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pendingDateMillis = datePickerState.selectedDateMillis
                    showDatePicker = false
                    showTimePicker = true
                }) { Text("Next") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        val timePickerState = rememberTimePickerState()
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Select time") },
            text = { TimePicker(state = timePickerState) },
            confirmButton = {
                TextButton(onClick = {
                    val calendar = java.util.Calendar.getInstance().apply {
                        timeInMillis = pendingDateMillis ?: System.currentTimeMillis()
                        set(java.util.Calendar.HOUR_OF_DAY, timePickerState.hour)
                        set(java.util.Calendar.MINUTE, timePickerState.minute)
                        set(java.util.Calendar.SECOND, 0)
                    }
                    val finalTime = calendar.timeInMillis

                    if (finalTime > System.currentTimeMillis()) {
                        reminderTime = finalTime
                    }
                    showTimePicker = false
                }) { Text("Set") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
            }
        )
    }
}
