package com.ilaiyarasu.smartnote.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.ilaiyarasu.smartnote.util.PreferencesManager

@Composable
fun PinLockScreen(
    preferencesManager: PreferencesManager,
    forceSetup: Boolean = false,
    onUnlocked: () -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    val isSettingUpPin = forceSetup || !preferencesManager.isPinEnabled
    var awaitingConfirm by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = when {
                !isSettingUpPin -> "Enter PIN"
                awaitingConfirm -> "Confirm PIN"
                else -> "Set up a PIN"
            },
            style = MaterialTheme.typography.headlineSmall
        )
        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = if (awaitingConfirm) confirmPin else pin,
            onValueChange = {
                if (it.length <= 6) {
                    if (awaitingConfirm) confirmPin = it else pin = it
                    error = false
                }
            },
            label = { Text("PIN") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            isError = error,
            singleLine = true
        )

        if (error) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                when {
                    !isSettingUpPin -> {
                        if (preferencesManager.verifyPin(pin)) {
                            onUnlocked()
                        } else {
                            error = true
                            errorMessage = "Incorrect PIN, try again"
                            pin = ""
                        }
                    }
                    !awaitingConfirm -> {
                        if (pin.length >= 4) {
                            awaitingConfirm = true
                        } else {
                            error = true
                            errorMessage = "PIN must be at least 4 digits"
                        }
                    }
                    else -> {
                        if (confirmPin == pin) {
                            preferencesManager.setPin(pin)
                            preferencesManager.isPinEnabled = true
                            onUnlocked()
                        } else {
                            error = true
                            errorMessage = "PINs don't match, try again"
                            pin = ""
                            confirmPin = ""
                            awaitingConfirm = false
                        }
                    }
                }
            },
            enabled = (if (awaitingConfirm) confirmPin else pin).isNotBlank()
        ) {
            Text(
                when {
                    !isSettingUpPin -> "Unlock"
                    !awaitingConfirm -> "Next"
                    else -> "Confirm"
                }
            )
        }
    }
}