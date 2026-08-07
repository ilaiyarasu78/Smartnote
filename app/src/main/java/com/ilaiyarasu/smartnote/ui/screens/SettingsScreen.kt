package com.ilaiyarasu.smartnote.ui.screens

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.common.api.ApiException
import com.ilaiyarasu.smartnote.BuildConfig
import com.ilaiyarasu.smartnote.util.AutoSyncScheduler
import com.ilaiyarasu.smartnote.util.DriveBackupManager
import com.ilaiyarasu.smartnote.util.PermissionHelper
import com.ilaiyarasu.smartnote.util.PreferencesManager
import com.ilaiyarasu.smartnote.viewmodel.NoteViewModel
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    preferencesManager: PreferencesManager,
    viewModel: NoteViewModel,
    onBack: () -> Unit,
    onChangePinClick: () -> Unit = {}
) {
    var darkMode by remember { mutableStateOf(preferencesManager.isDarkMode) }
    var pinEnabled by remember { mutableStateOf(preferencesManager.isPinEnabled) }
    var showDisablePinConfirm by remember { mutableStateOf(false) }
    var showSignOutConfirm by remember { mutableStateOf(false) }
    var autoSyncEnabled by remember { mutableStateOf(preferencesManager.isAutoSyncEnabled) }
    var lastSync by remember { mutableStateOf(preferencesManager.lastSyncTimestamp) }
    var permissionsExpanded by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val notes by viewModel.notes.collectAsState()

    var notificationGranted by remember { mutableStateOf(PermissionHelper.hasNotificationPermission(context)) }
    var exactAlarmGranted by remember { mutableStateOf(PermissionHelper.hasExactAlarmPermission(context)) }
    var batteryExempt by remember { mutableStateOf(PermissionHelper.isIgnoringBatteryOptimizations(context)) }
    val allPermissionsGranted = notificationGranted && exactAlarmGranted && batteryExempt

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                notificationGranted = PermissionHelper.hasNotificationPermission(context)
                exactAlarmGranted = PermissionHelper.hasExactAlarmPermission(context)
                batteryExempt = PermissionHelper.isIgnoringBatteryOptimizations(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val driveBackupManager = remember { DriveBackupManager(context) }
    var account by remember { mutableStateOf(driveBackupManager.getLastSignedInAccount()) }

    LaunchedEffect(Unit) {
        if (account == null) {
            val signedIn = driveBackupManager.silentSignIn()
            if (signedIn != null) {
                account = signedIn
            }
        }
        if (account != null && autoSyncEnabled) {
            AutoSyncScheduler.schedulePeriodicSync(context)
        }
    }

    var isBackingUp by remember { mutableStateOf(false) }
    var isRestoring by remember { mutableStateOf(false) }
    var driveStatus by remember { mutableStateOf<String?>(null) }
    var driveError by remember { mutableStateOf<String?>(null) }
    var showRestoreConfirm by remember { mutableStateOf(false) }

    val signInLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            val signedInAccount = task.getResult(ApiException::class.java)
            account = signedInAccount
            driveError = null
            if (autoSyncEnabled) AutoSyncScheduler.schedulePeriodicSync(context)
        } catch (e: ApiException) {
            driveError = "Sign-in failed: ${e.statusCode}"
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> notificationGranted = granted }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            // Profile Section
            if (account != null) {
                ProfileCard(account!!) {
                    signInLauncher.launch(driveBackupManager.getSignInClient().signInIntent)
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Appearance & Security Section
            SettingsSection(title = "General") {
                SettingsRow(
                    icon = Icons.Default.DarkMode,
                    label = "Dark Mode",
                    description = "Switch between light and dark themes"
                ) {
                    Switch(
                        checked = darkMode,
                        onCheckedChange = {
                            darkMode = it
                            preferencesManager.isDarkMode = it
                        }
                    )
                }

                SettingsRow(
                    icon = Icons.Default.Lock,
                    label = "PIN Lock",
                    description = "Secure your notes with a PIN"
                ) {
                    Switch(
                        checked = pinEnabled,
                        onCheckedChange = { turningOn ->
                            if (turningOn) {
                                pinEnabled = true
                                onChangePinClick()
                            } else {
                                showDisablePinConfirm = true
                            }
                        }
                    )
                }
                if (pinEnabled) {
                    TextButton(
                        onClick = onChangePinClick,
                        modifier = Modifier.padding(start = 48.dp)
                    ) {
                        Text("Change PIN")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Cloud & Sync Section
            SettingsSection(title = "Cloud & Sync") {
                if (account == null) {
                    Text(
                        "Backup your data securely to your Google Drive. Sign in to get started.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(8.dp)
                    )
                    Button(
                        onClick = {
                            signInLauncher.launch(driveBackupManager.getSignInClient().signInIntent)
                        },
                        modifier = Modifier.fillMaxWidth().padding(8.dp)
                    ) {
                        Icon(Icons.Default.CloudUpload, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Sign in with Google")
                    }
                } else {
                    SyncStatusCard(
                        noteCount = notes.size,
                        lastSync = lastSync,
                        isSyncing = isBackingUp || isRestoring,
                        status = driveStatus,
                        error = driveError
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    SettingsRow(
                        icon = Icons.Default.Sync,
                        label = "Auto Sync",
                        description = "Automatically back up your notes"
                    ) {
                        Switch(
                            checked = autoSyncEnabled,
                            onCheckedChange = { enabled ->
                                autoSyncEnabled = enabled
                                preferencesManager.isAutoSyncEnabled = enabled
                                if (enabled) {
                                    AutoSyncScheduler.schedulePeriodicSync(context)
                                } else {
                                    AutoSyncScheduler.cancelAll(context)
                                }
                            }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val currentAccount = account ?: return@Button
                                isBackingUp = true
                                driveStatus = "Backing up..."
                                driveError = null
                                coroutineScope.launch {
                                    driveBackupManager.backupNotes(currentAccount, notes).fold(
                                        onSuccess = {
                                            val now = System.currentTimeMillis()
                                            preferencesManager.lastSyncTimestamp = now
                                            lastSync = now
                                            driveStatus = "Synced Successfully"
                                        },
                                        onFailure = { 
                                            driveError = "Backup failed"
                                            driveStatus = null
                                        }
                                    )
                                    isBackingUp = false
                                }
                            },
                            enabled = !isBackingUp && !isRestoring,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Backup Now")
                        }

                        OutlinedButton(
                            onClick = { showRestoreConfirm = true },
                            enabled = !isBackingUp && !isRestoring,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Restore Data")
                        }
                    }

                    TextButton(
                        onClick = { showSignOutConfirm = true },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Sign Out")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Permissions Section
            SettingsSection(title = "Reminders") {
                if (allPermissionsGranted && !permissionsExpanded) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("All permissions granted", fontWeight = FontWeight.Medium)
                        }
                        TextButton(onClick = { permissionsExpanded = true }) {
                            Text("Manage")
                        }
                    }
                } else {
                    PermissionRow(
                        label = "Notifications",
                        granted = notificationGranted,
                        onFixClick = {
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                                notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                            }
                        }
                    )

                    PermissionRow(
                        label = "Exact Alarms",
                        granted = exactAlarmGranted,
                        onFixClick = { PermissionHelper.openExactAlarmSettings(context) }
                    )

                    PermissionRow(
                        label = "Battery Optimization",
                        granted = batteryExempt,
                        onFixClick = { PermissionHelper.requestIgnoreBatteryOptimizations(context) }
                    )

                    if (allPermissionsGranted) {
                        TextButton(
                            onClick = { permissionsExpanded = false },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("Collapse")
                        }
                    }
                }
            }

            if (BuildConfig.DEBUG) {
                Spacer(modifier = Modifier.height(24.dp))
                OutlinedButton(
                    onClick = {
                        androidx.work.WorkManager.getInstance(context).enqueue(
                            androidx.work.OneTimeWorkRequestBuilder<com.ilaiyarasu.smartnote.util.AutoBackupWorker>().build()
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Debug: Run background sync")
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // Dialogs
    if (showDisablePinConfirm) {
        AlertDialog(
            onDismissRequest = { showDisablePinConfirm = false },
            title = { Text("Disable PIN Lock?") },
            text = { Text("Your notes will no longer be protected by a PIN.") },
            confirmButton = {
                TextButton(onClick = {
                    preferencesManager.clearPin()
                    pinEnabled = false
                    showDisablePinConfirm = false
                }) { Text("Disable") }
            },
            dismissButton = {
                TextButton(onClick = { showDisablePinConfirm = false }) { Text("Cancel") }
            }
        )
    }

    if (showRestoreConfirm) {
        AlertDialog(
            onDismissRequest = { showRestoreConfirm = false },
            title = { Text("Restore from Drive?") },
            text = { Text("This will replace all your current notes with the backed-up version from Google Drive. This action cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    val currentAccount = account
                    showRestoreConfirm = false
                    if (currentAccount != null) {
                        isRestoring = true
                        driveStatus = "Restoring..."
                        driveError = null
                        coroutineScope.launch {
                            driveBackupManager.restoreNotes(currentAccount).fold(
                                onSuccess = { restoredNotes ->
                                    viewModel.restoreAllNotes(restoredNotes)
                                    driveStatus = "Restored ${restoredNotes.size} notes"
                                },
                                onFailure = { driveError = it.message ?: "Restore failed" }
                            )
                            isRestoring = false
                        }
                    }
                }) { Text("Restore") }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreConfirm = false }) { Text("Cancel") }
            }
        )
    }

    if (showSignOutConfirm) {
        AlertDialog(
            onDismissRequest = { showSignOutConfirm = false },
            title = { Text("Sign Out?") },
            text = { Text("Automatic backup and sync will be disabled. Your local notes will remain safe.") },
            confirmButton = {
                TextButton(onClick = {
                    driveBackupManager.getSignInClient().signOut()
                    account = null
                    AutoSyncScheduler.cancelAll(context)
                    showSignOutConfirm = false
                }) { Text("Sign Out") }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutConfirm = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun ProfileCard(account: GoogleSignInAccount, onChangeAccount: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = account.email?.take(1)?.uppercase() ?: "?",
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.titleLarge
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = account.displayName ?: "User",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = account.email ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onChangeAccount) {
                Icon(Icons.Default.SwitchAccount, contentDescription = "Change Account")
            }
        }
    }
}

@Composable
fun SyncStatusCard(
    noteCount: Int,
    lastSync: Long,
    isSyncing: Boolean,
    status: String?,
    error: String?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val infiniteTransition = rememberInfiniteTransition(label = "SyncRotation")
                val rotation by infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = 360f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1000, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "Rotation"
                )

                Icon(
                    imageVector = Icons.Default.Sync,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp).let { if (isSyncing) it.rotate(rotation) else it },
                    tint = if (error != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (error != null) "Sync Issue" else if (isSyncing) "Synchronizing..." else "Cloud Sync Active",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (error != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            val noteLabel = if (noteCount == 1) "note" else "notes"
            Text("• $noteCount $noteLabel tracked", style = MaterialTheme.typography.bodySmall)
            
            if (lastSync > 0L) {
                val date = Date(lastSync)
                val format = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                Text("• Last backed up: ${format.format(date)}", style = MaterialTheme.typography.bodySmall)
            }

            if (status != null || error != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = status ?: error ?: "",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (error != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(4.dp)) {
                content()
            }
        }
    }
}

@Composable
fun SettingsRow(
    icon: ImageVector,
    label: String,
    description: String? = null,
    action: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            if (description != null) {
                Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        action()
    }
}

@Composable
private fun PermissionRow(label: String, granted: Boolean, onFixClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Switch(
            checked = granted,
            enabled = !granted,
            onCheckedChange = { if (!granted) onFixClick() }
        )
    }
}
