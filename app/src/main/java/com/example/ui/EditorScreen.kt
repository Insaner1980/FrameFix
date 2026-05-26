package com.example.ui

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.Autorenew
import androidx.compose.material.icons.rounded.ColorLens
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.History
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AspectRatio
import com.example.model.BackgroundType
import com.example.model.LayoutMode
import com.example.util.ImageUtils
import com.example.viewmodel.EditorViewModel
import com.example.viewmodel.ExportState

enum class BottomTab {
    Ratio, Layout, Background, Rotate
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    viewModel: EditorViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val isLoading by viewModel.isLoading.collectAsState()
    val rotatedBitmap by viewModel.rotatedBitmap.collectAsState()
    val blurredBgBitmap by viewModel.blurredBgBitmap.collectAsState()
    val selectedRatio by viewModel.selectedAspectRatio.collectAsState()
    val layoutMode by viewModel.layoutMode.collectAsState()
    val zoomScale by viewModel.zoomScale.collectAsState()
    val relativePanX by viewModel.relativePanX.collectAsState()
    val relativePanY by viewModel.relativePanY.collectAsState()
    val rotationDegrees by viewModel.rotationDegrees.collectAsState()
    val bgType by viewModel.backgroundType.collectAsState()
    val bgColor by viewModel.customBackgroundColor.collectAsState()
    val exportState by viewModel.exportState.collectAsState()

    var activeTab by remember { mutableStateOf(BottomTab.Ratio) }
    var snackbarMessage by remember { mutableStateOf<String?>(null) }
    var showSnackbar by remember { mutableStateOf(false) }

    // Handler for export state changes
    LaunchedEffect(exportState) {
        when (exportState) {
            is ExportState.Success -> {
                val filename = (exportState as ExportState.Success).uri.lastPathSegment ?: "image"
                snackbarMessage = "Successfully exported to Pictures/FrameFix!"
                showSnackbar = true
            }
            is ExportState.Error -> {
                snackbarMessage = (exportState as ExportState.Error).message
                showSnackbar = true
            }
            else -> {}
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF1C1B1F)) // Clean Minimalism Dark Charcoal Background
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 1. Navigation Header
            EditorHeader(
                onBack = onBack,
                onReset = {
                    viewModel.updateZoom(1f)
                    viewModel.updatePan(-relativePanX * 100f, -relativePanY * 100f, 100f, 100f)
                },
                onExport = {
                    viewModel.exportFinalImage(context) { _ -> }
                },
                isSaving = exportState is ExportState.Exporting
            )

            // 2. Large Interactive Preview Area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color(0xFF141218)) // Clean Minimalism Canvas Area
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color(0xFF4D96FF)) // Clean Minimalism Blue Accent
                } else if (rotatedBitmap != null) {
                    AspectFramedCanvas(
                        rotatedBitmap = rotatedBitmap,
                        blurredBgBitmap = blurredBgBitmap,
                        aspectRatio = selectedRatio,
                        layoutMode = layoutMode,
                        zoomScale = zoomScale,
                        relativePanX = relativePanX,
                        relativePanY = relativePanY,
                        bgType = bgType,
                        bgColor = bgColor,
                        onGesture = { deltaZoom, deltaPan ->
                            viewModel.updateZoom(zoomScale * deltaZoom)
                            // Map drag pixels back to scale ratios. Dynamic canvas is calculated in BoxWithConstraints
                            viewModel.updatePan(deltaPan.x, deltaPan.y, 400f, 400f)
                        }
                    )
                } else {
                    Text(
                        text = "No image loaded.",
                        color = Color(0xFFE6E1E5),
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // 3. Dynamic Sub-control Panel (Animates contextually above bottom menu)
            Surface(
                color = Color(0xFF2B2930), // Clean Minimalism Drawer Background
                tonalElevation = 8.dp,
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = Color(0xFF49454F).copy(alpha = 0.3f), // Subtle Muted Line Outline
                        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                ) {
                    // Title index / indicator of the active tool
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = when (activeTab) {
                                BottomTab.Ratio -> "Aspect Ratio"
                                BottomTab.Layout -> "Canvas Layout"
                                BottomTab.Background -> "Background Style"
                                BottomTab.Rotate -> "Quick Transform"
                            },
                            color = Color(0xFFE6E1E5),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.testTag("subpanel_title")
                        )

                        // If Custom Ratio is selected and tab is Ratio, show stepper controls
                        if (activeTab == BottomTab.Ratio && selectedRatio is AspectRatio.Custom) {
                            val custom = selectedRatio as AspectRatio.Custom
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("W: ", color = Color(0xFF938F99), fontSize = 12.sp)
                                IconButton(
                                    onClick = {
                                        val nw = maxOf(1f, custom.w - 1f)
                                        viewModel.selectAspectRatio(AspectRatio.Custom(nw, custom.h))
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Remove,
                                        contentDescription = "Dec W",
                                        tint = Color(0xFFE6E1E5),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Text(
                                    "${custom.w.toInt()}",
                                    color = Color(0xFFE6E1E5),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp).testTag("custom_w_label")
                                )
                                IconButton(
                                    onClick = {
                                        val nw = minOf(30f, custom.w + 1f)
                                        viewModel.selectAspectRatio(AspectRatio.Custom(nw, custom.h))
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Inc W",
                                        tint = Color(0xFFE6E1E5),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Text("H: ", color = Color(0xFF938F99), fontSize = 12.sp)
                                IconButton(
                                    onClick = {
                                        val nh = maxOf(1f, custom.h - 1f)
                                        viewModel.selectAspectRatio(AspectRatio.Custom(custom.w, nh))
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Remove,
                                        contentDescription = "Dec H",
                                        tint = Color(0xFFE6E1E5),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Text(
                                    "${custom.h.toInt()}",
                                    color = Color(0xFFE6E1E5),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp).testTag("custom_h_label")
                                )
                                IconButton(
                                    onClick = {
                                        val nh = minOf(30f, custom.h + 1f)
                                        viewModel.selectAspectRatio(AspectRatio.Custom(custom.w, nh))
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Inc H",
                                        tint = Color(0xFFE6E1E5),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = Color(0xFF49454F).copy(alpha = 0.3f), thickness = 1.dp)

                    // Tab dependent controller content
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .padding(vertical = 12.dp)
                    ) {
                        when (activeTab) {
                            BottomTab.Ratio -> RatioSelectorPanel(
                                selectedRatio = selectedRatio,
                                onSelect = { viewModel.selectAspectRatio(it) }
                            )
                            BottomTab.Layout -> LayoutSelectorPanel(
                                selectedMode = layoutMode,
                                onSelect = { viewModel.setLayoutMode(it) }
                            )
                            BottomTab.Background -> BackgroundSelectorPanel(
                                bgType = bgType,
                                selectedColor = bgColor,
                                onTypeSelect = { viewModel.setBackgroundType(it) },
                                onColorSelect = { viewModel.setCustomBackgroundColor(it) }
                            )
                            BottomTab.Rotate -> QuickRotatePanel(
                                currentRot = rotationDegrees,
                                onRotate = { viewModel.rotate90() }
                            )
                        }
                    }
                }
            }

            // 4. Primary Bottom Navigation Toolbar
            EditorBottomBar(
                activeTab = activeTab,
                onTabSelect = { activeTab = it }
            )
        }

        // Animated Success/Error Toast Message Box
        AnimatedVisibility(
            visible = showSnackbar,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 100.dp)
        ) {
            Surface(
                color = if (snackbarMessage?.contains("fail") == true) Color(0xFFEF4444) else Color(0xFF10B981),
                shape = RoundedCornerShape(12.dp),
                tonalElevation = 6.dp,
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .widthIn(max = 450.dp)
                    .testTag("app_snackbar")
            ) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = snackbarMessage ?: "",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "Dismiss",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clickable {
                                showSnackbar = false
                                viewModel.resetExportState()
                            }
                            .padding(start = 12.dp, top = 4.dp, bottom = 4.dp)
                            .testTag("snackbar_action")
                    )
                }
            }
        }
    }
}

@Composable
fun EditorHeader(
    onBack: () -> Unit,
    onReset: () -> Unit,
    onExport: () -> Unit,
    isSaving: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .background(Color(0xFF1C1B1F)) // Clean Minimalism App Header
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.testTag("back_button")
            ) {
                Icon(
                    imageVector = Icons.Rounded.ArrowBack,
                    contentDescription = "Back",
                    tint = Color(0xFFE6E1E5)
                )
            }
            Text(
                text = "FrameFix Canvas",
                color = Color(0xFFE6E1E5),
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.testTag("header_title")
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onReset,
                modifier = Modifier.testTag("reset_button")
            ) {
                Icon(
                    imageVector = Icons.Rounded.History,
                    contentDescription = "Recenter",
                    tint = Color(0xFF938F99)
                )
            }

            IconButton(
                onClick = onExport,
                enabled = !isSaving,
                modifier = Modifier
                    .background(
                        if (isSaving) Color(0xFF49454F) else Color(0xFF4D96FF), // Active Blue vs Inactive
                        shape = RoundedCornerShape(20.dp) // Circularpill shape
                    )
                    .size(width = 72.dp, height = 38.dp)
                    .testTag("export_button")
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        color = Color(0xFF4D96FF),
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(16.dp)
                    )
                } else {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Export",
                            color = Color(0xFF1C1B1F),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
        }
    }
}

@Composable
fun AspectFramedCanvas(
    rotatedBitmap: android.graphics.Bitmap?,
    blurredBgBitmap: android.graphics.Bitmap?,
    aspectRatio: AspectRatio,
    layoutMode: LayoutMode,
    zoomScale: Float,
    relativePanX: Float,
    relativePanY: Float,
    bgType: BackgroundType,
    bgColor: Int,
    onGesture: (Float, Offset) -> Unit
) {
    // 100% responsive fluid target canvas sizing
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .testTag("canvas_viewer_container"),
        contentAlignment = Alignment.Center
    ) {
        val totalMaxW = maxWidth.value
        val totalMaxH = maxHeight.value

        val limitW = totalMaxW * 0.95f
        val limitH = totalMaxH * 0.95f

        val ratio = aspectRatio.ratioValue

        val finalW: Float
        val finalH: Float

        if (ratio >= limitW / limitH) {
            finalW = limitW
            finalH = limitW / ratio
        } else {
            finalH = limitH
            finalW = limitH * ratio
        }

        Surface(
            modifier = Modifier
                .size(width = finalW.dp, height = finalH.dp)
                .border(width = 1.dp, color = Color(0xFF938F99).copy(alpha = 0.2f), shape = RoundedCornerShape(2.dp))
                .clip(RoundedCornerShape(2.dp))
                .pointerInput(aspectRatio, layoutMode) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        onGesture(zoom, pan)
                    }
                }
                .testTag("drawing_canvas_surface"),
            color = Color.Black
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val canvasW = size.width
                val canvasH = size.height

                val nativeCanvas = drawContext.canvas.nativeCanvas
                ImageUtils.drawComposedCanvas(
                    canvas = nativeCanvas,
                    canvasW = canvasW,
                    canvasH = canvasH,
                    rotatedBitmap = rotatedBitmap,
                    blurredBgBitmap = blurredBgBitmap,
                    layoutMode = layoutMode,
                    zoomScale = zoomScale,
                    relativePanX = relativePanX,
                    relativePanY = relativePanY,
                    bgType = bgType,
                    bgColor = bgColor
                )
            }
        }
    }
}

@Composable
fun RatioSelectorPanel(
    selectedRatio: AspectRatio,
    onSelect: (AspectRatio) -> Unit
) {
    val presets = listOf(
        AspectRatio.Ratio9_16,
        AspectRatio.Ratio4_5,
        AspectRatio.Ratio3_4,
        AspectRatio.Ratio2_3,
        AspectRatio.Ratio1_1,
        AspectRatio.Ratio16_9,
        AspectRatio.Ratio4_3,
        AspectRatio.Ratio3_2,
        AspectRatio.Custom(3f, 2f)
    )

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .testTag("ratio_lazy_row")
    ) {
        items(presets) { preset ->
            val isSelected = when {
                preset is AspectRatio.Custom && selectedRatio is AspectRatio.Custom -> true
                preset::class == selectedRatio::class -> true
                else -> false
            }

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) Color(0xFF4D96FF) else Color(0xFF49454F) // M3 Blue vs Slate-Charcoal
                ),
                border = androidx.compose.foundation.BorderStroke(
                    width = 1.dp,
                    color = if (isSelected) Color(0xFF4D96FF) else Color(0xFF49454F).copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(16.dp), // Styled modern curved preset corners
                modifier = Modifier
                    .size(width = 80.dp, height = 100.dp)
                    .clickable { onSelect(preset) }
                    .testTag("ratio_card_${preset.label.lowercase()}")
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Small responsive miniature box contour inside the card
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .border(
                                width = 1.5.dp,
                                color = if (isSelected) Color(0xFF1C1B1F).copy(alpha = 0.8f) else Color(0xFFE6E1E5).copy(alpha = 0.4f),
                                shape = RoundedCornerShape(4.dp)
                             )
                            .padding(4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // Graphic simulation representation of the aspect ratio size
                        Box(
                            modifier = Modifier
                                .fillMaxHeight(
                                    if (preset.ratioValue < 1f) 1f else 1f / preset.ratioValue
                                )
                                .fillMaxWidth(
                                    if (preset.ratioValue >= 1f) 1f else preset.ratioValue
                                )
                                .background(
                                    if (isSelected) Color(0xFF1C1B1F).copy(alpha = 0.2f) else Color(0xFFE6E1E5).copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(2.dp)
                                )
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = if (preset is AspectRatio.Custom) "Custom" else preset.label,
                        color = if (isSelected) Color(0xFF1C1B1F) else Color(0xFFE6E1E5),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}



@Composable
fun LayoutSelectorPanel(
    selectedMode: LayoutMode,
    onSelect: (LayoutMode) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Fit layout card option
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (selectedMode == LayoutMode.FIT) Color(0xFF4D96FF) else Color(0xFF49454F)
            ),
            border = androidx.compose.foundation.BorderStroke(
                width = 1.dp,
                color = if (selectedMode == LayoutMode.FIT) Color(0xFF4D96FF) else Color(0xFF49454F).copy(alpha = 0.3f)
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .weight(1f)
                .height(96.dp)
                .clickable { onSelect(LayoutMode.FIT) }
                .testTag("fit_mode_card")
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(12.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Miniature shape contour of layout mode (Fitting completely inside)
                Canvas(modifier = Modifier.size(24.dp)) {
                    // Canvas boundary
                    drawRoundRect(
                        color = if (selectedMode == LayoutMode.FIT) Color(0xFF1C1B1F).copy(alpha = 0.5f) else Color(0xFF938F99),
                        size = size,
                        cornerRadius = CornerRadius(4f, 4f),
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                    // Image fit simulation (leaves background)
                    drawRoundRect(
                        color = if (selectedMode == LayoutMode.FIT) Color(0xFF1C1B1F) else Color(0xFFE6E1E5).copy(alpha = 0.6f),
                        topLeft = Offset(size.width * 0.15f, size.height * 0.15f),
                        size = Size(size.width * 0.7f, size.height * 0.7f),
                        cornerRadius = CornerRadius(2f, 2f)
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Fit Canvas",
                    color = if (selectedMode == LayoutMode.FIT) Color(0xFF1C1B1F) else Color(0xFFE6E1E5),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Fill layout card option
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (selectedMode == LayoutMode.FILL) Color(0xFF4D96FF) else Color(0xFF49454F)
            ),
            border = androidx.compose.foundation.BorderStroke(
                width = 1.dp,
                color = if (selectedMode == LayoutMode.FILL) Color(0xFF4D96FF) else Color(0xFF49454F).copy(alpha = 0.3f)
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .weight(1f)
                .height(96.dp)
                .clickable { onSelect(LayoutMode.FILL) }
                .testTag("fill_mode_card")
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(12.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Miniature outline representing full image fill and visual crop
                Canvas(modifier = Modifier.size(24.dp)) {
                    // Canvas boundary
                    drawRoundRect(
                        color = if (selectedMode == LayoutMode.FILL) Color(0xFF1C1B1F).copy(alpha = 0.5f) else Color(0xFF938F99),
                        size = size,
                        cornerRadius = CornerRadius(4f, 4f),
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                    // Image overfill simulation
                    drawRoundRect(
                        color = if (selectedMode == LayoutMode.FILL) Color(0xFF1C1B1F) else Color(0xFFE6E1E5).copy(alpha = 0.6f),
                        topLeft = Offset(-size.width * 0.1f, -size.height * 0.1f),
                        size = Size(size.width * 1.2f, size.height * 1.2f),
                        cornerRadius = CornerRadius(2f, 2f)
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Fill & Crop",
                    color = if (selectedMode == LayoutMode.FILL) Color(0xFF1C1B1F) else Color(0xFFE6E1E5),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun BackgroundSelectorPanel(
    bgType: BackgroundType,
    selectedColor: Int,
    onTypeSelect: (BackgroundType) -> Unit,
    onColorSelect: (Int) -> Unit
) {
    var rawHue by remember { mutableStateOf(180f) }

    val presetColors = listOf(
        Color(0xFF1E293B), // Slate Grey
        Color(0xFF0F172A), // Midnight
        Color(0xFF14532D), // Forest
        Color(0xFF1E3A8A), // Blue Depth
        Color(0xFF7C2D12), // Rust Red
        Color(0xFF78350F), // Ochre Amber
        Color(0xFF451A03), // Espresso
        Color(0xFF581C87)  // Royal Plum
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        // Upper background category picker
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Option 1: Ambient Blur
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (bgType == BackgroundType.BLUR) Color(0xFF4D96FF) else Color(0xFF1C1B1F)
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, if (bgType == BackgroundType.BLUR) Color(0xFF4D96FF) else Color(0xFF49454F).copy(alpha = 0.3f)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .clickable { onTypeSelect(BackgroundType.BLUR) }
                    .testTag("bg_blur_card")
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Blur",
                        color = if (bgType == BackgroundType.BLUR) Color(0xFF1C1B1F) else Color(0xFFE6E1E5),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Option 2: Black Solid
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (bgType == BackgroundType.BLACK) Color(0xFF4D96FF) else Color(0xFF1C1B1F)
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, if (bgType == BackgroundType.BLACK) Color(0xFF4D96FF) else Color(0xFF49454F).copy(alpha = 0.3f)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .clickable { onTypeSelect(BackgroundType.BLACK) }
                    .testTag("bg_black_card")
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Box(modifier = Modifier.size(10.dp).background(Color.Black, CircleShape).border(1.dp, if (bgType == BackgroundType.BLACK) Color(0xFF1C1B1F) else Color.White, CircleShape))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Black",
                        color = if (bgType == BackgroundType.BLACK) Color(0xFF1C1B1F) else Color(0xFFE6E1E5),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Option 3: White Solid
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (bgType == BackgroundType.WHITE) Color(0xFF4D96FF) else Color(0xFF1C1B1F)
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, if (bgType == BackgroundType.WHITE) Color(0xFF4D96FF) else Color(0xFF49454F).copy(alpha = 0.3f)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .clickable { onTypeSelect(BackgroundType.WHITE) }
                    .testTag("bg_white_card")
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Box(modifier = Modifier.size(10.dp).background(Color.White, CircleShape).border(1.dp, Color.LightGray, CircleShape))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "White",
                        color = if (bgType == BackgroundType.WHITE) Color(0xFF1C1B1F) else Color(0xFFE6E1E5),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Option 4: Preset / Custom Palette
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (bgType == BackgroundType.CUSTOM) Color(0xFF4D96FF) else Color(0xFF1C1B1F)
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, if (bgType == BackgroundType.CUSTOM) Color(0xFF4D96FF) else Color(0xFF49454F).copy(alpha = 0.3f)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .clickable { onTypeSelect(BackgroundType.CUSTOM) }
                    .testTag("bg_custom_card")
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(
                                brush = Brush.sweepGradient(listOf(Color.Red, Color.Green, Color.Blue, Color.Red)),
                                shape = CircleShape
                            )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Preset",
                        color = if (bgType == BackgroundType.CUSTOM) Color(0xFF1C1B1F) else Color(0xFFE6E1E5),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Predefined solid dot chips or a gorgeous Hue picker when Custom is active
        if (bgType == BackgroundType.CUSTOM) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().testTag("custom_colors_lazy_row")
            ) {
                // Predefined solid color palette dots
                items(presetColors) { col ->
                    val isDotSelected = selectedColor == android.graphics.Color.rgb(
                        (col.red * 255f).toInt(),
                        (col.green * 255f).toInt(),
                        (col.blue * 255f).toInt()
                    )
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(col, CircleShape)
                            .border(
                                width = if (isDotSelected) 2.5.dp else 1.dp,
                                color = if (isDotSelected) Color.White else Color(0xFF475569),
                                shape = CircleShape
                            )
                            .clickable {
                                val argb = android.graphics.Color.rgb(
                                    (col.red * 255f).toInt(),
                                    (col.green * 255f).toInt(),
                                    (col.blue * 255f).toInt()
                                )
                                onColorSelect(argb)
                            }
                    )
                }

                // Add nice slide-in fine hue adjust slider as an inline item
                item {
                    Spacer(modifier = Modifier.width(6.dp))
                    Row(
                        modifier = Modifier
                            .width(130.dp)
                            .height(28.dp)
                            .background(Color(0xFF1C1B1F), RoundedCornerShape(14.dp)) // Clean Minimalism Dark Base
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                    Slider(
                        value = rawHue,
                        onValueChange = {
                            rawHue = it
                            val argb = android.graphics.Color.HSVToColor(floatArrayOf(it, 0.75f, 0.55f))
                            onColorSelect(argb)
                        },
                        valueRange = 0f..360f,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF4D96FF) // Blue Thumb
                        ),
                        track = { _ ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .background(
                                        brush = Brush.horizontalGradient(
                                            listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red)
                                        ),
                                        shape = RoundedCornerShape(3.dp)
                                    )
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("hue_gradient_slider")
                    )
                    }
                }
            }
        } else {
            // Blank description placeholder
            Text(
                text = when (bgType) {
                    BackgroundType.BLUR -> "Fills empty borders with a beautifully smooth blurred projection of current image."
                    BackgroundType.BLACK -> "Frames empty borders with high-contrast, professional, solid deep matte black."
                    BackgroundType.WHITE -> "Frames empty borders with sterile, gallery-standard solid neutral pure white."
                    else -> ""
                },
                color = Color(0xFF938F99), // Clean Minimalism Muted Gray
                fontSize = 11.sp,
                lineHeight = 16.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).testTag("bg_description_label")
            )
        }
    }
}

@Composable
fun QuickRotatePanel(
    currentRot: Int,
    onRotate: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF49454F)), // Clean Minimalism Charcoal Preset Card
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .width(200.dp)
                .height(64.dp)
                .clickable { onRotate() }
                .testTag("rotate_action_card")
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Autorenew,
                    contentDescription = "Rotate Icon",
                    tint = Color(0xFF4D96FF),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("Rotate 90°", color = Color(0xFFE6E1E5), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text("Current orientation: $currentRot°", color = Color(0xFF938F99), fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
fun EditorBottomBar(
    activeTab: BottomTab,
    onTabSelect: (BottomTab) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .background(Color(0xFF1C1B1F)), // Clean Minimalism Bottom Bar Background
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        // Tab 1: Ratio
        BottomTabButton(
            label = "Ratio",
            icon = Icons.Rounded.AspectRatio,
            isActive = activeTab == BottomTab.Ratio,
            onClick = { onTabSelect(BottomTab.Ratio) },
            testTag = "toolbar_tab_ratio"
        )

        // Tab 2: Layout
        BottomTabButton(
            label = "Layout",
            icon = Icons.Rounded.GridView,
            isActive = activeTab == BottomTab.Layout,
            onClick = { onTabSelect(BottomTab.Layout) },
            testTag = "toolbar_tab_layout"
        )

        // Tab 3: Background
        BottomTabButton(
            label = "BG",
            icon = Icons.Rounded.ColorLens,
            isActive = activeTab == BottomTab.Background,
            onClick = { onTabSelect(BottomTab.Background) },
            testTag = "toolbar_tab_background"
        )

        // Tab 4: Rotate
        BottomTabButton(
            label = "Rotate",
            icon = Icons.Rounded.Autorenew,
            isActive = activeTab == BottomTab.Rotate,
            onClick = { onTabSelect(BottomTab.Rotate) },
            testTag = "toolbar_tab_rotate"
        )
    }
}

@Composable
fun BottomTabButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isActive: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .size(width = 76.dp, height = 64.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .testTag(testTag)
    ) {
        // Pill visual active outline container
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(if (isActive) Color(0xFF4A4458) else Color.Transparent)
                .padding(horizontal = 16.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isActive) Color(0xFF4D96FF) else Color(0xFF938F99),
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = label,
            color = if (isActive) Color(0xFF4D96FF) else Color(0xFF938F99),
            fontSize = 11.sp,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium
        )
    }
}
