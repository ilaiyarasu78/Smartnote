package com.ilaiyarasu.smartnote.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ilaiyarasu.smartnote.data.Note
import com.ilaiyarasu.smartnote.data.NoteRepository
import com.ilaiyarasu.smartnote.ui.components.TemplatePickerDialog
import com.ilaiyarasu.smartnote.ui.theme.PinkAccent
import com.ilaiyarasu.smartnote.ui.theme.PurplePrimary
import com.ilaiyarasu.smartnote.viewmodel.NoteViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val categoryChips = listOf("All", "Study", "Work", "Personal")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteListScreen(
    viewModel: NoteViewModel,
    onNoteClick: (String) -> Unit,
    onAddClick: (String?) -> Unit
) {
    val notes by viewModel.notes.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val sortOrder by viewModel.sortOrder.collectAsState()

    var showTemplatePicker by remember { mutableStateOf(false) }
    var sortMenuExpanded by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    val pinnedNotes = notes.filter { it.isPinned }
    val recentNotes = notes.filter { !it.isPinned }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            item { Spacer(Modifier.height(8.dp)) }

            // Top bar
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Smart Note",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = PurplePrimary
                    )
                }
                Spacer(Modifier.height(16.dp))
            }

            // Search + Sort
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.onSearchQueryChange(it) },
                        placeholder = { Text("Search notes...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.weight(1f)
                    )
                    Box {
                        OutlinedIconLabelButton(
                            icon = when (sortOrder) {
                                NoteRepository.SortOrder.TITLE_ASC -> Icons.Default.SortByAlpha
                                else -> Icons.Default.SwapVert
                            },
                            label = "Sort"
                        ) {
                            sortMenuExpanded = true
                        }
                        DropdownMenu(
                            expanded = sortMenuExpanded,
                            onDismissRequest = { sortMenuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Newest first") },
                                onClick = {
                                    viewModel.onSortOrderChange(NoteRepository.SortOrder.DATE_DESC)
                                    sortMenuExpanded = false
                                },
                                leadingIcon = { Icon(Icons.Default.ArrowDownward, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Oldest first") },
                                onClick = {
                                    viewModel.onSortOrderChange(NoteRepository.SortOrder.DATE_ASC)
                                    sortMenuExpanded = false
                                },
                                leadingIcon = { Icon(Icons.Default.ArrowUpward, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Title A–Z") },
                                onClick = {
                                    viewModel.onSortOrderChange(NoteRepository.SortOrder.TITLE_ASC)
                                    sortMenuExpanded = false
                                },
                                leadingIcon = { Icon(Icons.Default.SortByAlpha, contentDescription = null) }
                            )
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            // Category chips
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categoryChips.forEach { chip ->
                        val isSelected = if (chip == "All") selectedCategory == null else selectedCategory == chip
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                viewModel.onCategorySelected(if (chip == "All") null else chip)
                            },
                            label = { Text(chip) },
                            leadingIcon = {
                                Icon(
                                    getCategoryIcon(chip),
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            shape = RoundedCornerShape(50),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PurplePrimary,
                                selectedLabelColor = Color.White,
                                selectedLeadingIconColor = Color.White
                            )
                        )
                    }
                }
                Spacer(Modifier.height(24.dp))
            }

            // Pinned Notes
            if (pinnedNotes.isNotEmpty()) {
                item {
                    Text("Pinned Notes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                }
                items(pinnedNotes, key = { it.id }) { note ->
                    NoteCard(
                        note = note,
                        onClick = { onNoteClick(note.id) },
                        onDelete = { coroutineScope.launch { viewModel.deleteNote(note) } },
                        onTogglePin = { coroutineScope.launch { viewModel.togglePinned(note) } }
                    )
                    Spacer(Modifier.height(6.dp))
                }
            }

            // Recent Notes
            item {
                Text("Recent Notes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
            }
            if (recentNotes.isEmpty() && pinnedNotes.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("No notes found", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                items(recentNotes, key = { it.id }) { note ->
                    NoteCard(
                        note = note,
                        onClick = { onNoteClick(note.id) },
                        onDelete = { coroutineScope.launch { viewModel.deleteNote(note) } },
                        onTogglePin = { coroutineScope.launch { viewModel.togglePinned(note) } }
                    )
                    Spacer(Modifier.height(6.dp))
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
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
    val currentLocale = LocalConfiguration.current.locales[0]
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(getCategoryColor(note.category).copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    getCategoryIcon(note.category),
                    contentDescription = null,
                    tint = getCategoryColor(note.category),
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    note.title.ifBlank { "Untitled" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    note.content,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color.Gray)
                    Spacer(Modifier.width(4.dp))
                    Text(
                        SimpleDateFormat("d MMM, h:mm a", currentLocale).format(Date(note.updatedAt)),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray,
                        fontSize = 10.sp
                    )
                }
            }
            Column {
                IconButton(
                    onClick = onTogglePin,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.PushPin,
                        contentDescription = "Pin",
                        tint = if (note.isPinned) PurplePrimary else Color.Gray,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = Color.Gray,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

private fun getCategoryIcon(category: String): ImageVector = when (category) {
    "Study" -> Icons.AutoMirrored.Filled.MenuBook
    "Work" -> Icons.Default.Work
    "Personal" -> Icons.Default.Person
    else -> Icons.AutoMirrored.Filled.Notes
}

private fun getCategoryColor(category: String): Color = when (category) {
    "Study" -> Color(0xFF2F6FED)
    "Work" -> Color(0xFF2FAE60)
    "Personal" -> PinkAccent
    else -> PurplePrimary
}

@Composable
private fun OutlinedIconLabelButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.height(56.dp),
        contentPadding = PaddingValues(horizontal = 12.dp)
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}
