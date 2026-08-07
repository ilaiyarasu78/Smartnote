package com.ilaiyarasu.smartnote

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.ilaiyarasu.smartnote.navigation.SmartnoteNavGraph
import com.ilaiyarasu.smartnote.ui.theme.SmartnoteTheme
import com.ilaiyarasu.smartnote.viewmodel.NoteViewModel
import com.ilaiyarasu.smartnote.viewmodel.NoteViewModelFactory

class MainActivity : ComponentActivity() {
    private val viewModel: NoteViewModel by viewModels {
        NoteViewModelFactory((application as SmartnoteApplication).repository, application)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
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