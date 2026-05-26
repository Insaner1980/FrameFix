package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.EditorScreen
import com.example.ui.HomeScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.EditorViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("main_scaffold")
                ) { innerPadding ->
                    val context = LocalContext.current
                    val editorViewModel: EditorViewModel = viewModel()
                    val originalUri by editorViewModel.originalUri.collectAsState()

                    // Single-Screen Slate Animation controller
                    AnimatedContent(
                        targetState = originalUri,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(250)) togetherWith fadeOut(animationSpec = tween(200))
                        },
                        label = "screen_transition"
                    ) { uri ->
                        if (uri == null) {
                            HomeScreen(
                                onImageSelected = { pickedUri ->
                                    editorViewModel.loadImage(context, pickedUri)
                                },
                                modifier = Modifier.padding(innerPadding)
                            )
                        } else {
                            // Register system back button callback for immersive navigation
                            BackHandler {
                                editorViewModel.loadImage(context, android.net.Uri.EMPTY)
                            }

                            EditorScreen(
                                viewModel = editorViewModel,
                                onBack = {
                                    editorViewModel.loadImage(context, android.net.Uri.EMPTY)
                                },
                                modifier = Modifier.padding(innerPadding)
                            )
                        }
                    }
                }
            }
        }
    }
}
