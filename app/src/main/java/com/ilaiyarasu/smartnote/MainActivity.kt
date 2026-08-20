package com.ilaiyarasu.smartnoteapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.ilaiyarasu.smartnote.SmartnoteApplication
import com.ilaiyarasu.smartnote.navigation.SmartnoteNavGraph
import com.ilaiyarasu.smartnote.ui.theme.SmartnoteTheme
import com.ilaiyarasu.smartnote.util.AutoSyncScheduler
import com.ilaiyarasu.smartnote.util.DriveBackupManager
import com.ilaiyarasu.smartnote.viewmodel.NoteViewModel
import com.ilaiyarasu.smartnote.viewmodel.NoteViewModelFactory

class MainActivity : ComponentActivity() {
    private val viewModel: NoteViewModel by viewModels {
        NoteViewModelFactory((application as SmartnoteApplication).repository, application)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // AUTO-SYNC ON START: Trigger an update check every time the app opens
        val prefs = (application as SmartnoteApplication).preferencesManager
        val driveManager = DriveBackupManager(this)
        val currentAccount = driveManager.getLastSignedInAccount()
        
        // Ensure ViewModel starts with the correct account silo
        viewModel.setCurrentAccount(currentAccount?.email ?: "local")

        if (prefs.isAutoSyncEnabled && currentAccount != null) {
            AutoSyncScheduler.triggerImmediateSync(this)
        }

        setContent {
            SmartnoteTheme {
                SmartnoteNavGraph(
                    viewModel = viewModel,
                    preferencesManager = (application as SmartnoteApplication).preferencesManager
                )
            }
        }
    }
}