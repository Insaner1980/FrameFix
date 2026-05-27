package com.insaner1980.framefix.ui

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.Autorenew
import androidx.compose.material.icons.rounded.ColorLens
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.History
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.insaner1980.framefix.model.AspectRatio
import com.insaner1980.framefix.model.BackgroundType
import com.insaner1980.framefix.model.LayoutMode
import com.insaner1980.framefix.ui.theme.FrameFixColors
import com.insaner1980.framefix.util.CanvasBackground
import com.insaner1980.framefix.util.CanvasImages
import com.insaner1980.framefix.util.CanvasRenderSpec
import com.insaner1980.framefix.util.CanvasSize
import com.insaner1980.framefix.util.ImageUtils
import com.insaner1980.framefix.util.RelativePan
import com.insaner1980.framefix.viewmodel.EditorViewModel
import com.insaner1980.framefix.viewmodel.ExportState

private data class FramedCanvasState(
    val rotatedBitmap: Bitmap?,
    val blurredBgBitmap: Bitmap?,
    val aspectRatio: AspectRatio,
    val layoutMode: LayoutMode,
    val zoomScale: Float,
    val pan: RelativePan,
    val background: CanvasBackground,
)

private data class EditorCanvasState(val isLoading: Boolean, val frame: FramedCanvasState)

private data class EditorToolState(
    val selectedRatio: AspectRatio,
    val layoutMode: LayoutMode,
    val backgroundType: BackgroundType,
    val backgroundColor: Int,
    val rotationDegrees: Int,
)

private data class EditorSnackbarState(val message: String?, val isVisible: Boolean)

private data class EditorSnackbarController(val state: EditorSnackbarState, val dismiss: () -> Unit)

private data class EditorScreenUiState(
    val canvas: EditorCanvasState,
    val tools: EditorToolState,
    val exportState: ExportState,
    val activeTab: BottomTab,
    val snackbar: EditorSnackbarState,
)

private data class HeaderActions(val onBack: () -> Unit, val onReset: () -> Unit, val onExport: () -> Unit)

private data class ToolActions(
    val onRatioSelect: (AspectRatio) -> Unit,
    val onLayoutSelect: (LayoutMode) -> Unit,
    val onBackgroundTypeSelect: (BackgroundType) -> Unit,
    val onColorSelect: (Int) -> Unit,
    val onRotate: () -> Unit,
)

private data class EditorScreenActions(
    val header: HeaderActions,
    val onGesture: (Float, Offset) -> Unit,
    val tools: ToolActions,
    val onTabSelect: (BottomTab) -> Unit,
    val onDismissSnackbar: () -> Unit,
)

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(viewModel: EditorViewModel, onBack: () -> Unit, modifier: Modifier = Modifier) {
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
    val snackbarController = rememberEditorSnackbarController(
        exportState = exportState,
        onResetExportState = viewModel::resetExportState,
    )
    val frameState = FramedCanvasState(
        rotatedBitmap = rotatedBitmap,
        blurredBgBitmap = blurredBgBitmap,
        aspectRatio = selectedRatio,
        layoutMode = layoutMode,
        zoomScale = zoomScale,
        pan = RelativePan(relativePanX, relativePanY),
        background = CanvasBackground(bgType, bgColor),
    )
    val uiState = EditorScreenUiState(
        canvas = EditorCanvasState(isLoading, frameState),
        tools = EditorToolState(
            selectedRatio = selectedRatio,
            layoutMode = layoutMode,
            backgroundType = bgType,
            backgroundColor = bgColor,
            rotationDegrees = rotationDegrees,
        ),
        exportState = exportState,
        activeTab = activeTab,
        snackbar = snackbarController.state,
    )
    val actions = EditorScreenActions(
        header = HeaderActions(
            onBack = onBack,
            onReset = {
                viewModel.updateZoom(1f)
                viewModel.updatePan(-relativePanX * 100f, -relativePanY * 100f, 100f, 100f)
            },
            onExport = {
                viewModel.exportFinalImage(context) { _ -> }
            },
        ),
        onGesture = { deltaZoom, deltaPan ->
            viewModel.updateZoom(zoomScale * deltaZoom)
            viewModel.updatePan(deltaPan.x, deltaPan.y, 400f, 400f)
        },
        tools = ToolActions(
            onRatioSelect = viewModel::selectAspectRatio,
            onLayoutSelect = viewModel::setLayoutMode,
            onBackgroundTypeSelect = viewModel::setBackgroundType,
            onColorSelect = viewModel::setCustomBackgroundColor,
            onRotate = viewModel::rotate90,
        ),
        onTabSelect = { activeTab = it },
        onDismissSnackbar = snackbarController.dismiss,
    )

    EditorScreenContent(
        uiState = uiState,
        actions = actions,
        modifier = modifier,
    )
}

@Composable
private fun rememberEditorSnackbarController(
    exportState: ExportState,
    onResetExportState: () -> Unit,
): EditorSnackbarController {
    var snackbarMessage by remember { mutableStateOf<String?>(null) }
    var showSnackbar by remember { mutableStateOf(false) }

    LaunchedEffect(exportState) {
        when (exportState) {
            is ExportState.Success -> {
                snackbarMessage = "Successfully exported to Pictures/FrameFix!"
                showSnackbar = true
            }

            is ExportState.Error -> {
                snackbarMessage = exportState.message
                showSnackbar = true
            }

            else -> {}
        }
    }

    return EditorSnackbarController(
        state = EditorSnackbarState(
            message = snackbarMessage,
            isVisible = showSnackbar,
        ),
        dismiss = {
            showSnackbar = false
            onResetExportState()
        },
    )
}

@Composable
private fun EditorScreenContent(
    uiState: EditorScreenUiState,
    actions: EditorScreenActions,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(FrameFixColors.Background)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            EditorHeader(
                onBack = actions.header.onBack,
                onReset = actions.header.onReset,
                onExport = actions.header.onExport,
                isSaving = uiState.exportState is ExportState.Exporting,
            )
            EditorPreview(
                canvasState = uiState.canvas,
                onGesture = actions.onGesture,
                modifier = Modifier.weight(1f),
            )
            EditorToolPanel(
                toolState = uiState.tools,
                activeTab = uiState.activeTab,
                actions = actions.tools,
            )
            EditorBottomBar(
                activeTab = uiState.activeTab,
                onTabSelect = actions.onTabSelect,
            )
        }

        EditorSnackbarHost(
            snackbarState = uiState.snackbar,
            onDismiss = actions.onDismissSnackbar,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun EditorPreview(
    canvasState: EditorCanvasState,
    onGesture: (Float, Offset) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(FrameFixColors.Canvas)
            .padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        when {
            canvasState.isLoading -> {
                CircularProgressIndicator(color = FrameFixColors.Accent)
            }

            canvasState.frame.rotatedBitmap != null -> {
                AspectFramedCanvas(
                    canvasState = canvasState.frame,
                    onGesture = onGesture,
                )
            }

            else -> {
                Text(
                    text = "No image loaded.",
                    color = FrameFixColors.OnDark,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun EditorToolPanel(
    toolState: EditorToolState,
    activeTab: BottomTab,
    actions: ToolActions,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = FrameFixColors.Surface,
        tonalElevation = 8.dp,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = FrameFixColors.Outline.copy(alpha = 0.3f),
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
        ) {
            EditorToolHeader(
                activeTab = activeTab,
                selectedRatio = toolState.selectedRatio,
                onRatioSelect = actions.onRatioSelect,
            )
            HorizontalDivider(color = FrameFixColors.Outline.copy(alpha = 0.3f), thickness = 1.dp)
            EditorToolContent(
                toolState = toolState,
                activeTab = activeTab,
                actions = actions,
            )
        }
    }
}

@Composable
private fun EditorToolHeader(
    activeTab: BottomTab,
    selectedRatio: AspectRatio,
    onRatioSelect: (AspectRatio) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = activeTab.title,
            color = FrameFixColors.OnDark,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.testTag("subpanel_title"),
        )

        if (activeTab == BottomTab.Ratio && selectedRatio is AspectRatio.Custom) {
            CustomRatioStepper(custom = selectedRatio, onRatioSelect = onRatioSelect)
        }
    }
}

private val BottomTab.title: String
    get() = when (this) {
        BottomTab.Ratio -> "Aspect Ratio"
        BottomTab.Layout -> "Canvas Layout"
        BottomTab.Background -> "Background Style"
        BottomTab.Rotate -> "Quick Transform"
    }

@Composable
private fun CustomRatioStepper(
    custom: AspectRatio.Custom,
    onRatioSelect: (AspectRatio) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RatioValueStepper(
            label = "W:",
            value = custom.w,
            valueTestTag = "custom_w_label",
            onDecrease = {
                onRatioSelect(AspectRatio.Custom(maxOf(1f, custom.w - 1f), custom.h))
            },
            onIncrease = {
                onRatioSelect(AspectRatio.Custom(minOf(30f, custom.w + 1f), custom.h))
            },
        )
        Spacer(modifier = Modifier.width(12.dp))
        RatioValueStepper(
            label = "H:",
            value = custom.h,
            valueTestTag = "custom_h_label",
            onDecrease = {
                onRatioSelect(AspectRatio.Custom(custom.w, maxOf(1f, custom.h - 1f)))
            },
            onIncrease = {
                onRatioSelect(AspectRatio.Custom(custom.w, minOf(30f, custom.h + 1f)))
            },
        )
    }
}

@Composable
private fun RatioValueStepper(
    label: String,
    value: Float,
    valueTestTag: String,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = FrameFixColors.Muted, fontSize = 12.sp)
        RatioStepButton(
            icon = Icons.Default.Remove,
            contentDescription = "Decrease $label",
            onClick = onDecrease,
        )
        Text(
            "${value.toInt()}",
            color = FrameFixColors.OnDark,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 4.dp).testTag(valueTestTag),
        )
        RatioStepButton(
            icon = Icons.Default.Add,
            contentDescription = "Increase $label",
            onClick = onIncrease,
        )
    }
}

@Composable
private fun RatioStepButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(28.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = FrameFixColors.OnDark,
            modifier = Modifier.size(16.dp),
        )
    }
}

@Composable
private fun EditorToolContent(toolState: EditorToolState, activeTab: BottomTab, actions: ToolActions) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp)
            .padding(vertical = 12.dp),
    ) {
        when (activeTab) {
            BottomTab.Ratio -> RatioSelectorPanel(
                selectedRatio = toolState.selectedRatio,
                onSelect = actions.onRatioSelect,
            )

            BottomTab.Layout -> LayoutSelectorPanel(
                selectedMode = toolState.layoutMode,
                onSelect = actions.onLayoutSelect,
            )

            BottomTab.Background -> BackgroundSelectorPanel(
                bgType = toolState.backgroundType,
                selectedColor = toolState.backgroundColor,
                onTypeSelect = actions.onBackgroundTypeSelect,
                onColorSelect = actions.onColorSelect,
            )

            BottomTab.Rotate -> QuickRotatePanel(
                currentRot = toolState.rotationDegrees,
                onRotate = actions.onRotate,
            )
        }
    }
}

@Composable
private fun EditorSnackbarHost(
    snackbarState: EditorSnackbarState,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = snackbarState.isVisible,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        modifier = modifier.padding(bottom = 100.dp),
    ) {
        Surface(
            color = snackbarColor(snackbarState.message),
            shape = RoundedCornerShape(12.dp),
            tonalElevation = 6.dp,
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .widthIn(max = 450.dp)
                .testTag("app_snackbar"),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = snackbarState.message.orEmpty(),
                    color = FrameFixColors.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = "Dismiss",
                    color = FrameFixColors.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clickable { onDismiss() }
                        .padding(start = 12.dp, top = 4.dp, bottom = 4.dp)
                        .testTag("snackbar_action"),
                )
            }
        }
    }
}

private fun snackbarColor(message: String?): Color =
    if (message?.contains("fail") == true) FrameFixColors.Error else FrameFixColors.Success

@Composable
fun EditorHeader(
    onBack: () -> Unit,
    onReset: () -> Unit,
    onExport: () -> Unit,
    isSaving: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .background(FrameFixColors.Background) // Clean Minimalism App Header
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.testTag("back_button"),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Back",
                    tint = FrameFixColors.OnDark,
                )
            }
            Text(
                text = "FrameFix Canvas",
                color = FrameFixColors.OnDark,
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.testTag("header_title"),
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onReset,
                modifier = Modifier.testTag("reset_button"),
            ) {
                Icon(
                    imageVector = Icons.Rounded.History,
                    contentDescription = "Recenter",
                    tint = FrameFixColors.Muted,
                )
            }

            IconButton(
                onClick = onExport,
                enabled = !isSaving,
                modifier = Modifier
                    .background(
                        if (isSaving) FrameFixColors.Outline else FrameFixColors.Accent, // Active Blue vs Inactive
                        shape = RoundedCornerShape(20.dp), // Circularpill shape
                    )
                    .size(width = 72.dp, height = 38.dp)
                    .testTag("export_button"),
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        color = FrameFixColors.Accent,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(16.dp),
                    )
                } else {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Export",
                            color = FrameFixColors.Background,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
        }
    }
}

@Composable
private fun AspectFramedCanvas(
    canvasState: FramedCanvasState,
    onGesture: (Float, Offset) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .testTag("canvas_viewer_container"),
        contentAlignment = Alignment.Center,
    ) {
        val frameSize = constrainedFrameSize(
            maxWidth = maxWidth.value,
            maxHeight = maxHeight.value,
            ratio = canvasState.aspectRatio.ratioValue,
        )

        Surface(
            modifier = Modifier
                .size(width = frameSize.width.dp, height = frameSize.height.dp)
                .border(width = 1.dp, color = FrameFixColors.Muted.copy(alpha = 0.2f), shape = RoundedCornerShape(2.dp))
                .clip(RoundedCornerShape(2.dp))
                .pointerInput(canvasState.aspectRatio, canvasState.layoutMode) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        onGesture(zoom, pan)
                    }
                }
                .testTag("drawing_canvas_surface"),
            color = FrameFixColors.Black,
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val canvasW = size.width
                val canvasH = size.height

                val nativeCanvas = drawContext.canvas.nativeCanvas
                ImageUtils.drawComposedCanvas(
                    canvas = nativeCanvas,
                    renderSpec = canvasState.toRenderSpec(canvasW, canvasH),
                )
            }
        }
    }
}

private fun constrainedFrameSize(maxWidth: Float, maxHeight: Float, ratio: Float): CanvasSize {
    val limitW = maxWidth * 0.95f
    val limitH = maxHeight * 0.95f
    return if (ratio >= limitW / limitH) {
        CanvasSize(width = limitW, height = limitW / ratio)
    } else {
        CanvasSize(width = limitH * ratio, height = limitH)
    }
}

private fun FramedCanvasState.toRenderSpec(canvasW: Float, canvasH: Float): CanvasRenderSpec = CanvasRenderSpec(
    size = CanvasSize(canvasW, canvasH),
    images = CanvasImages(rotatedBitmap, blurredBgBitmap),
    layoutMode = layoutMode,
    zoomScale = zoomScale,
    pan = pan,
    background = background,
)

private val AspectRatioPresets = listOf(
    AspectRatio.Ratio9_16,
    AspectRatio.Ratio4_5,
    AspectRatio.Ratio3_4,
    AspectRatio.Ratio2_3,
    AspectRatio.Ratio1_1,
    AspectRatio.Ratio16_9,
    AspectRatio.Ratio4_3,
    AspectRatio.Ratio3_2,
    AspectRatio.Custom(3f, 2f),
)

@Composable
fun RatioSelectorPanel(selectedRatio: AspectRatio, onSelect: (AspectRatio) -> Unit, modifier: Modifier = Modifier) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .testTag("ratio_lazy_row"),
    ) {
        items(AspectRatioPresets) { preset ->
            AspectRatioOptionCard(
                preset = preset,
                isSelected = isAspectRatioSelected(preset, selectedRatio),
                onSelect = onSelect,
            )
        }
    }
}

@Composable
private fun AspectRatioOptionCard(preset: AspectRatio, isSelected: Boolean, onSelect: (AspectRatio) -> Unit) {
    SelectableOptionCard(
        isSelected = isSelected,
        modifier = Modifier
            .size(width = 80.dp, height = 100.dp)
            .testTag("ratio_card_${preset.label.lowercase()}"),
        onClick = { onSelect(preset) },
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AspectRatioPreview(preset = preset, isSelected = isSelected)
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = preset.displayLabel,
                color = ratioLabelColor(isSelected),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun AspectRatioPreview(preset: AspectRatio, isSelected: Boolean) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .border(
                width = 1.5.dp,
                color = ratioPreviewBorderColor(isSelected),
                shape = RoundedCornerShape(4.dp),
            )
            .padding(4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight(ratioPreviewHeight(preset))
                .fillMaxWidth(ratioPreviewWidth(preset))
                .background(
                    color = ratioPreviewFillColor(isSelected),
                    shape = RoundedCornerShape(2.dp),
                ),
        )
    }
}

private fun isAspectRatioSelected(preset: AspectRatio, selectedRatio: AspectRatio): Boolean =
    (preset is AspectRatio.Custom && selectedRatio is AspectRatio.Custom) ||
        preset::class == selectedRatio::class

private val AspectRatio.displayLabel: String
    get() = if (this is AspectRatio.Custom) "Custom" else label

private fun ratioPreviewWidth(preset: AspectRatio): Float = if (preset.ratioValue >= 1f) 1f else preset.ratioValue

private fun ratioPreviewHeight(preset: AspectRatio): Float = if (preset.ratioValue < 1f) 1f else 1f / preset.ratioValue

private fun ratioPreviewBorderColor(isSelected: Boolean): Color =
    if (isSelected) FrameFixColors.Background.copy(alpha = 0.8f) else FrameFixColors.OnDark.copy(alpha = 0.4f)

private fun ratioPreviewFillColor(isSelected: Boolean): Color =
    if (isSelected) FrameFixColors.Background.copy(alpha = 0.2f) else FrameFixColors.OnDark.copy(alpha = 0.1f)

private fun ratioLabelColor(isSelected: Boolean): Color =
    if (isSelected) FrameFixColors.Background else FrameFixColors.OnDark

@Composable
fun LayoutSelectorPanel(selectedMode: LayoutMode, onSelect: (LayoutMode) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LayoutModeCard(
            label = "Fit Canvas",
            mode = LayoutMode.FIT,
            selectedMode = selectedMode,
            imageTopLeftFraction = Offset(0.15f, 0.15f),
            imageSizeFraction = Size(0.7f, 0.7f),
            modifier = Modifier
                .weight(1f)
                .height(96.dp)
                .testTag("fit_mode_card"),
            onSelect = onSelect,
        )

        LayoutModeCard(
            label = "Fill & Crop",
            mode = LayoutMode.FILL,
            selectedMode = selectedMode,
            imageTopLeftFraction = Offset(-0.1f, -0.1f),
            imageSizeFraction = Size(1.2f, 1.2f),
            modifier = Modifier
                .weight(1f)
                .height(96.dp)
                .testTag("fill_mode_card"),
            onSelect = onSelect,
        )
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun BackgroundSelectorPanel(
    bgType: BackgroundType,
    selectedColor: Int,
    onTypeSelect: (BackgroundType) -> Unit,
    onColorSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var rawHue by remember { mutableFloatStateOf(180f) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        BackgroundTypeSelectorRow(bgType = bgType, onTypeSelect = onTypeSelect)
        Spacer(modifier = Modifier.height(12.dp))
        BackgroundCustomization(
            bgType = bgType,
            selectedColor = selectedColor,
            rawHue = rawHue,
            onHueChange = { rawHue = it },
            onColorSelect = onColorSelect,
        )
    }
}

@Composable
private fun BackgroundTypeSelectorRow(bgType: BackgroundType, onTypeSelect: (BackgroundType) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        BackgroundTypeCard(
            label = "Blur",
            type = BackgroundType.BLUR,
            selectedType = bgType,
            modifier = Modifier
                .weight(1f)
                .height(44.dp)
                .testTag("bg_blur_card"),
            onTypeSelect = onTypeSelect,
        )
        BackgroundTypeCard(
            label = "Black",
            type = BackgroundType.BLACK,
            selectedType = bgType,
            modifier = Modifier
                .weight(1f)
                .height(44.dp)
                .testTag("bg_black_card"),
            onTypeSelect = onTypeSelect,
        ) { isSelected ->
            SolidColorDot(
                color = FrameFixColors.Black,
                borderColor = if (isSelected) FrameFixColors.Background else FrameFixColors.White,
            )
        }
        BackgroundTypeCard(
            label = "White",
            type = BackgroundType.WHITE,
            selectedType = bgType,
            modifier = Modifier
                .weight(1f)
                .height(44.dp)
                .testTag("bg_white_card"),
            onTypeSelect = onTypeSelect,
        ) {
            SolidColorDot(
                color = FrameFixColors.White,
                borderColor = FrameFixColors.LightBorder,
            )
        }
        BackgroundTypeCard(
            label = "Preset",
            type = BackgroundType.CUSTOM,
            selectedType = bgType,
            modifier = Modifier
                .weight(1f)
                .height(44.dp)
                .testTag("bg_custom_card"),
            onTypeSelect = onTypeSelect,
        ) {
            PresetColorDot()
        }
    }
}

@Composable
private fun BackgroundCustomization(
    bgType: BackgroundType,
    selectedColor: Int,
    rawHue: Float,
    onHueChange: (Float) -> Unit,
    onColorSelect: (Int) -> Unit,
) {
    if (bgType == BackgroundType.CUSTOM) {
        PresetBackgroundControls(
            selectedColor = selectedColor,
            rawHue = rawHue,
            onHueChange = onHueChange,
            onColorSelect = onColorSelect,
        )
    } else {
        BackgroundDescription(bgType = bgType)
    }
}

@Composable
private fun PresetBackgroundControls(
    selectedColor: Int,
    rawHue: Float,
    onHueChange: (Float) -> Unit,
    onColorSelect: (Int) -> Unit,
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().testTag("custom_colors_lazy_row"),
    ) {
        items(FrameFixColors.BackgroundPresets) { color ->
            PresetColorChip(
                color = color,
                isSelected = selectedColor == color.toArgb(),
                onColorSelect = onColorSelect,
            )
        }
        item {
            Spacer(modifier = Modifier.width(6.dp))
            HueSlider(
                rawHue = rawHue,
                onHueChange = onHueChange,
                onColorSelect = onColorSelect,
            )
        }
    }
}

@Composable
private fun PresetColorChip(color: Color, isSelected: Boolean, onColorSelect: (Int) -> Unit) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .background(color, CircleShape)
            .border(
                width = if (isSelected) 2.5.dp else 1.dp,
                color = if (isSelected) FrameFixColors.White else FrameFixColors.ColorChipBorder,
                shape = CircleShape,
            )
            .clickable { onColorSelect(color.toArgb()) },
    )
}

@Composable
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
private fun HueSlider(rawHue: Float, onHueChange: (Float) -> Unit, onColorSelect: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .width(130.dp)
            .height(28.dp)
            .background(FrameFixColors.Background, RoundedCornerShape(14.dp))
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Slider(
            value = rawHue,
            onValueChange = {
                onHueChange(it)
                onColorSelect(android.graphics.Color.HSVToColor(floatArrayOf(it, 0.75f, 0.55f)))
            },
            valueRange = 0f..360f,
            colors = SliderDefaults.colors(thumbColor = FrameFixColors.Accent),
            track = { _ -> HueSliderTrack() },
            modifier = Modifier
                .weight(1f)
                .testTag("hue_gradient_slider"),
        )
    }
}

@Composable
private fun HueSliderTrack() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(6.dp)
            .background(
                brush = Brush.horizontalGradient(
                    listOf(
                        Color.Red,
                        Color.Yellow,
                        Color.Green,
                        Color.Cyan,
                        Color.Blue,
                        Color.Magenta,
                        Color.Red,
                    ),
                ),
                shape = RoundedCornerShape(3.dp),
            ),
    )
}

@Composable
private fun BackgroundDescription(bgType: BackgroundType) {
    Text(
        text = backgroundDescriptionText(bgType),
        color = FrameFixColors.Muted,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).testTag("bg_description_label"),
    )
}

private fun backgroundDescriptionText(bgType: BackgroundType): String = when (bgType) {
    BackgroundType.BLUR ->
        "Fills empty borders with a beautifully smooth blurred projection of current image."

    BackgroundType.BLACK ->
        "Frames empty borders with high-contrast, professional, solid deep matte black."

    BackgroundType.WHITE ->
        "Frames empty borders with sterile, gallery-standard solid neutral pure white."

    BackgroundType.CUSTOM -> ""
}

@Composable
fun QuickRotatePanel(currentRot: Int, onRotate: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = FrameFixColors.Outline),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .width(200.dp)
                .height(64.dp)
                .clickable { onRotate() }
                .testTag("rotate_action_card"),
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Autorenew,
                    contentDescription = "Rotate Icon",
                    tint = FrameFixColors.Accent,
                    modifier = Modifier.size(24.dp),
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("Rotate 90°", color = FrameFixColors.OnDark, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text("Current orientation: $currentRot°", color = FrameFixColors.Muted, fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
fun EditorBottomBar(activeTab: BottomTab, onTabSelect: (BottomTab) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp)
            .background(FrameFixColors.Background), // Clean Minimalism Bottom Bar Background
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceAround,
    ) {
        // Tab 1: Ratio
        BottomTabButton(
            label = "Ratio",
            icon = Icons.Rounded.AspectRatio,
            isActive = activeTab == BottomTab.Ratio,
            onClick = { onTabSelect(BottomTab.Ratio) },
            testTag = "toolbar_tab_ratio",
        )

        // Tab 2: Layout
        BottomTabButton(
            label = "Layout",
            icon = Icons.Rounded.GridView,
            isActive = activeTab == BottomTab.Layout,
            onClick = { onTabSelect(BottomTab.Layout) },
            testTag = "toolbar_tab_layout",
        )

        // Tab 3: Background
        BottomTabButton(
            label = "BG",
            icon = Icons.Rounded.ColorLens,
            isActive = activeTab == BottomTab.Background,
            onClick = { onTabSelect(BottomTab.Background) },
            testTag = "toolbar_tab_background",
        )

        // Tab 4: Rotate
        BottomTabButton(
            label = "Rotate",
            icon = Icons.Rounded.Autorenew,
            isActive = activeTab == BottomTab.Rotate,
            onClick = { onTabSelect(BottomTab.Rotate) },
            testTag = "toolbar_tab_rotate",
        )
    }
}

@Composable
fun BottomTabButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isActive: Boolean,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .size(width = 76.dp, height = 64.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .testTag(testTag),
    ) {
        // Pill visual active outline container
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(if (isActive) FrameFixColors.ActivePill else Color.Transparent)
                .padding(horizontal = 16.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isActive) FrameFixColors.Accent else FrameFixColors.Muted,
                modifier = Modifier.size(22.dp),
            )
        }
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = label,
            color = if (isActive) FrameFixColors.Accent else FrameFixColors.Muted,
            fontSize = 11.sp,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
        )
    }
}
