package com.insaner1980.framefix

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
import com.insaner1980.framefix.ui.EditorScreen
import com.insaner1980.framefix.ui.HomeScreen
import com.insaner1980.framefix.ui.theme.FrameFixTheme
import com.insaner1980.framefix.viewmodel.EditorViewModel

private const val SCREEN_FADE_IN_MS = 250
private const val SCREEN_FADE_OUT_MS = 200

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FrameFixTheme {
                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("main_scaffold"),
                ) { innerPadding ->
                    val context = LocalContext.current
                    val editorViewModel: EditorViewModel = viewModel()
                    val originalUri by editorViewModel.originalUri.collectAsState()

                    // Single-Screen Slate Animation controller
                    AnimatedContent(
                        targetState = originalUri,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(SCREEN_FADE_IN_MS)) togetherWith
                                fadeOut(animationSpec = tween(SCREEN_FADE_OUT_MS))
                        },
                        label = "screen_transition",
                    ) { uri ->
                        if (uri == null) {
                            HomeScreen(
                                onImageSelect = { pickedUri ->
                                    editorViewModel.loadImage(context, pickedUri)
                                },
                                modifier = Modifier.padding(innerPadding),
                            )
                        } else {
                            BackHandler {
                                editorViewModel.clearImage()
                            }

                            EditorScreen(
                                viewModel = editorViewModel,
                                onBack = {
                                    editorViewModel.clearImage()
                                },
                                modifier = Modifier.padding(innerPadding),
                            )
                        }
                    }
                }
            }
        }
    }
}
