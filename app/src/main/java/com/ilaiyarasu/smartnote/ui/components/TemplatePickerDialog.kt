package com.ilaiyarasu.smartnote.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ilaiyarasu.smartnote.data.noteTemplates

@Composable
fun TemplatePickerDialog(
    onDismiss: () -> Unit,
    onTemplateSelected: (String?) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Start a new note") },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.heightIn(max = 400.dp)
            ) {
                item {
                    ListItem(
                        headlineContent = { Text("Blank Note") },
                        leadingContent = {
                            Icon(Icons.Default.NoteAdd, contentDescription = null)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onTemplateSelected(null) }
                    )
                    HorizontalDivider()
                }
                items(noteTemplates) { template ->
                    ListItem(
                        headlineContent = { Text(template.label) },
                        supportingContent = { Text(template.suggestedCategory) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onTemplateSelected(template.id) }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
