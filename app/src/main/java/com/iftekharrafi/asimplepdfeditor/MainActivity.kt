package com.iftekharrafi.asimplepdfeditor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.iftekharrafi.asimplepdfeditor.navigation.RootGraph
import com.iftekharrafi.asimplepdfeditor.ui.theme.ASimplePdfEditorTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ASimplePdfEditorTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) {innerPadding ->

                    RootGraph(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

