package com.ilaiyarasu.smartnote.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ilaiyarasu.smartnote.data.Note
import com.ilaiyarasu.smartnote.ui.components.TemplatePickerDialog
import com.ilaiyarasu.smartnote.viewmodel.NoteViewModel

private val categories = listOf("Study", "Work", "Personal")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteListScreen(
    viewModel: NoteViewModel,
    onNoteClick: (String) -> Unit,
    onAddClick: (String?) -> Unit,
    onSettingsClick: () -> Unit
) {
    val notes by viewModel.notes.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    var showTemplatePicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Smartnote") },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showTemplatePicker = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add note")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.onSearchQueryChange(it) },
                placeholder = { Text("Search notes") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                FilterChip(
                    selected = selectedCategory == null,
                    onClick = { viewModel.onCategorySelected(null) },
                    label = { Text("All") }
                )
                categories.forEach { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = {
                            viewModel.onCategorySelected(if (selectedCategory == cat) null else cat)
                        },
                        label = { Text(cat) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (notes.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (searchQuery.isNotBlank() || selectedCategory != null)
                            "No notes match your search"
                        else
                            "No notes yet. Tap + to add one.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 96.dp)
                ) {
                    items(notes, key = { it.id }) { note ->
                        NoteCard(
                            note = note,
                            onClick = { onNoteClick(note.id) },
                            onDelete = { viewModel.deleteNote(note) },
                            onTogglePin = { viewModel.togglePinned(note) }
                        )
                    }
                }
            }
        }
    }

    if (showTemplatePicker) {
        TemplatePickerDialog(
            onDismiss = { showTemplatePicker = false },
            onTemplateSelected = { templateId ->
                showTemplatePicker = false
                onAddClick(templateId)
            }
        )
    }
}

@Composable
private fun NoteCard(
    note: Note,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onTogglePin: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (note.isPinned) {
                        Icon(
                            Icons.Default.PushPin,
                            contentDescription = "Pinned",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text(
                        text = note.title.ifBlank { "Untitled" },
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = note.content,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                AssistChip(
                    onClick = {},
                    label = { Text(note.category, style = MaterialTheme.typography.labelSmall) }
                )
            }
            Column {
                IconButton(onClick = onTogglePin) {
                    Icon(
                        if (note.isPinned) Icons.Default.PushPin else Icons.Outlined.PushPin,
                        contentDescription = if (note.isPinned) "Unpin" else "Pin",
                        tint = if (note.isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete note")
                }
            }
        }
    }
}
