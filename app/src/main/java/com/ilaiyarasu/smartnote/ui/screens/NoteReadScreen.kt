package com.ilaiyarasu.smartnote.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ilaiyarasu.smartnote.data.Note
import com.ilaiyarasu.smartnote.ui.components.LanguagePickerDropdown
import com.ilaiyarasu.smartnote.util.AppLanguage
import com.ilaiyarasu.smartnote.util.PdfExporter
import com.ilaiyarasu.smartnote.util.TextToSpeechHelper
import com.ilaiyarasu.smartnote.util.TranslationHelper
import com.ilaiyarasu.smartnote.util.supportedAppLanguages
import com.ilaiyarasu.smartnote.viewmodel.NoteViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteReadScreen(
    viewModel: NoteViewModel,
    noteId: String,
    onBack: () -> Unit,
    onEditClick: (String) -> Unit
) {
    var note by remember { mutableStateOf<Note?>(null) }

    var sourceLang by remember { mutableStateOf(supportedAppLanguages[0]) } // English
    var targetLang by remember { mutableStateOf(supportedAppLanguages[1]) } // Tamil
    var isTranslating by remember { mutableStateOf(false) }
    var translatedResult by remember { mutableStateOf<String?>(null) }
    var translateError by remember { mutableStateOf<String?>(null) }
    var showReadAloudMenu by remember { mutableStateOf(false) }
    var pdfExportError by remember { mutableStateOf<String?>(null) }

    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val ttsHelper = remember { TextToSpeechHelper(context) }
    val isSpeaking by ttsHelper.isSpeaking.collectAsState()
    val ttsError by ttsHelper.error.collectAsState()

    DisposableEffect(Unit) {
        onDispose { ttsHelper.destroy() }
    }

    LaunchedEffect(noteId) {
        note = viewModel.getNoteById(noteId)
    }

    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, noteId) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                coroutineScope.launch {
                    note = viewModel.getNoteById(noteId)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Note") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    note?.let { currentNote ->
                        IconButton(onClick = { onEditClick(currentNote.id) }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit")
                        }
                        IconButton(onClick = {
                            viewModel.deleteNote(currentNote)
                            onBack()
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete")
                        }
                    }
                }
            )
        }
    ) { padding ->
        note?.let { currentNote ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = currentNote.title.ifBlank { "Untitled" },
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    AssistChip(
                        onClick = {},
                        label = { Text(currentNote.category, style = MaterialTheme.typography.labelSmall) }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
                            .format(Date(currentNote.updatedAt)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = currentNote.content,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        lineHeight = 28.sp
                    )
                )

                Spacer(modifier = Modifier.height(20.dp))

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

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                                            currentNote.content,
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
                        enabled = !isTranslating
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
                                        ttsHelper.speak(currentNote.content, lang.ttsTag)
                                    }
                                )
                            }
                        }
                    }

                    TextButton(
                        onClick = {
                            pdfExportError = null
                            PdfExporter.exportNoteToPdf(context, currentNote).fold(
                                onSuccess = { uri -> PdfExporter.shareOrOpenPdf(context, uri) },
                                onFailure = { e -> pdfExportError = "Export failed: ${e.message}" }
                            )
                        }
                    ) {
                        Text("Export PDF")
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
                pdfExportError?.let {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
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
                    note?.let { currentNote ->
                        viewModel.updateNote(
                            currentNote,
                            currentNote.title,
                            result,
                            currentNote.category,
                            currentNote.reminderTime
                        )
                    }
                    translatedResult = null
                }) { Text("Replace note") }
            },
            dismissButton = {
                TextButton(onClick = { translatedResult = null }) { Text("Close") }
            }
        )
    }
}