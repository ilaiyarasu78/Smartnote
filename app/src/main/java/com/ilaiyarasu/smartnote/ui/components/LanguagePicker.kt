package com.ilaiyarasu.smartnote.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.ilaiyarasu.smartnote.util.AppLanguage
import com.ilaiyarasu.smartnote.util.supportedAppLanguages

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguagePickerDropdown(
    label: String,
    selected: AppLanguage,
    onSelect: (AppLanguage) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selected.label,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            supportedAppLanguages.forEach { lang ->
                DropdownMenuItem(
                    text = { Text(lang.label) },
                    onClick = {
                        onSelect(lang)
                        expanded = false
                    }
                )
            }
        }
    }
}
