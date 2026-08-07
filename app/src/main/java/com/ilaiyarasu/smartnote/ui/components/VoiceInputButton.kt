package com.ilaiyarasu.smartnote.ui.components

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.ilaiyarasu.smartnote.util.SpeechRecognizerHelper
import com.ilaiyarasu.smartnote.util.supportedVoiceLanguages

@Composable
fun VoiceInputButton(
    onTextRecognized: (String) -> Unit
) {
    val context = LocalContext.current
    val helper = remember { SpeechRecognizerHelper(context) }

    var selectedLanguage by remember { mutableStateOf(supportedVoiceLanguages.first()) }
    var showLanguageMenu by remember { mutableStateOf(false) }

    val isListening by helper.isListening.collectAsState()
    val isTooQuiet by helper.isTooQuiet.collectAsState()
    val error by helper.error.collectAsState()

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) helper.startListening(selectedLanguage.code, onTextRecognized)
    }

    DisposableEffect(Unit) {
        onDispose { helper.destroy() }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = {
                    if (isListening) {
                        helper.stopListening()
                    } else {
                        val hasPermission = ContextCompat.checkSelfPermission(
                            context, Manifest.permission.RECORD_AUDIO
                        ) == PackageManager.PERMISSION_GRANTED
                        if (hasPermission) {
                            helper.startListening(selectedLanguage.code, onTextRecognized)
                        } else {
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    }
                }
            ) {
                Icon(
                    imageVector = if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
                    contentDescription = if (isListening) "Stop listening" else "Start voice input",
                    tint = if (isListening) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
            }

            Box {
                TextButton(onClick = { showLanguageMenu = true }) {
                    Text(selectedLanguage.label)
                }
                DropdownMenu(
                    expanded = showLanguageMenu,
                    onDismissRequest = { showLanguageMenu = false }
                ) {
                    supportedVoiceLanguages.forEach { lang ->
                        DropdownMenuItem(
                            text = { Text(lang.label) },
                            onClick = {
                                selectedLanguage = lang
                                showLanguageMenu = false
                            }
                        )
                    }
                }
            }
        }

        if (isListening && isTooQuiet) {
            Text(
                text = "Speak a little louder 🔊",
                color = MaterialTheme.colorScheme.tertiary,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        error?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}