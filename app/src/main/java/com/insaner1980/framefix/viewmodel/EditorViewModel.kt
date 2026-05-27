package com.insaner1980.framefix.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.core.graphics.createBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.insaner1980.framefix.model.AspectRatio
import com.insaner1980.framefix.model.BackgroundType
import com.insaner1980.framefix.model.LayoutMode
import com.insaner1980.framefix.util.CanvasBackground
import com.insaner1980.framefix.util.CanvasImages
import com.insaner1980.framefix.util.CanvasRenderSpec
import com.insaner1980.framefix.util.CanvasSize
import com.insaner1980.framefix.util.ImageUtils
import com.insaner1980.framefix.util.RelativePan
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "EditorViewModel"
private const val DEFAULT_ZOOM = 1f
private const val MIN_ZOOM = 0.5f
private const val MAX_ZOOM = 8.0f
private const val MAX_RELATIVE_PAN = 2.0f
private const val QUARTER_TURN_DEGREES = 90
private const val FULL_TURN_DEGREES = 360
private const val MIN_EXPORT_DIMENSION = 100

sealed class ExportState {
    object Idle : ExportState()
    object Exporting : ExportState()
    data class Success(val uri: Uri) : ExportState()
    data class Error(val message: String) : ExportState()
}

class EditorViewModel(
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _originalUri = MutableStateFlow<Uri?>(null)
    val originalUri = _originalUri.asStateFlow()

    private val _originalBitmap = MutableStateFlow<Bitmap?>(null)
    val originalBitmap = _originalBitmap.asStateFlow()

    private val _rotatedBitmap = MutableStateFlow<Bitmap?>(null)
    val rotatedBitmap = _rotatedBitmap.asStateFlow()

    private val _blurredBgBitmap = MutableStateFlow<Bitmap?>(null)
    val blurredBgBitmap = _blurredBgBitmap.asStateFlow()

    private val _selectedAspectRatio = MutableStateFlow<AspectRatio>(AspectRatio.Ratio9_16)
    val selectedAspectRatio = _selectedAspectRatio.asStateFlow()

    private val _layoutMode = MutableStateFlow(LayoutMode.FIT)
    val layoutMode = _layoutMode.asStateFlow()

    private val _zoomScale = MutableStateFlow(DEFAULT_ZOOM)
    val zoomScale = _zoomScale.asStateFlow()

    private val _relativePanX = MutableStateFlow(0f)
    val relativePanX = _relativePanX.asStateFlow()

    private val _relativePanY = MutableStateFlow(0f)
    val relativePanY = _relativePanY.asStateFlow()

    private val _rotationDegrees = MutableStateFlow(0)
    val rotationDegrees = _rotationDegrees.asStateFlow()

    private val _backgroundType = MutableStateFlow(BackgroundType.BLUR)
    val backgroundType = _backgroundType.asStateFlow()

    private val _customBackgroundColor = MutableStateFlow(0xFF1E293B.toInt())
    val customBackgroundColor = _customBackgroundColor.asStateFlow()

    private val _exportState = MutableStateFlow<ExportState>(ExportState.Idle)
    val exportState = _exportState.asStateFlow()

    fun clearImage() {
        _isLoading.value = false
        _exportState.value = ExportState.Idle
        _originalUri.value = null
        _originalBitmap.value = null
        _rotatedBitmap.value = null
        _blurredBgBitmap.value = null
        _zoomScale.value = DEFAULT_ZOOM
        _relativePanX.value = 0f
        _relativePanY.value = 0f
        _rotationDegrees.value = 0
    }

    fun loadImage(context: Context, uri: Uri) {
        _isLoading.value = true
        _exportState.value = ExportState.Idle
        _originalUri.value = uri

        // Reset transforms
        _zoomScale.value = DEFAULT_ZOOM
        _relativePanX.value = 0f
        _relativePanY.value = 0f
        _rotationDegrees.value = 0

        viewModelScope.launch {
            val bitmap = withContext(ioDispatcher) {
                ImageUtils.loadBitmapFromUri(context, uri)
            }
            if (bitmap != null) {
                _originalBitmap.value = bitmap
                _rotatedBitmap.value = bitmap
                _blurredBgBitmap.value = withContext(defaultDispatcher) {
                    ImageUtils.createBlurredBitmap(bitmap)
                }
            }
            _isLoading.value = false
        }
    }

    fun selectAspectRatio(ratio: AspectRatio) {
        _selectedAspectRatio.value = ratio
        // When aspect ratio changes, reset basic offsets so position is centered
        _relativePanX.value = 0f
        _relativePanY.value = 0f
        _zoomScale.value = DEFAULT_ZOOM
    }

    fun setLayoutMode(mode: LayoutMode) {
        _layoutMode.value = mode
        // Reset pan & zoom to fit constraints naturally
        _relativePanX.value = 0f
        _relativePanY.value = 0f
        _zoomScale.value = DEFAULT_ZOOM
    }

    fun updateZoom(zoom: Float) {
        // Limit zoom between 0.5x and 8.0x
        _zoomScale.value = zoom.coerceIn(MIN_ZOOM, MAX_ZOOM)
    }

    fun updatePan(deltaX: Float, deltaY: Float, viewWidth: Float, viewHeight: Float) {
        if (viewWidth <= 0f || viewHeight <= 0f) return

        // Accumulate relative drag changes
        val updatedX = _relativePanX.value + (deltaX / viewWidth)
        val updatedY = _relativePanY.value + (deltaY / viewHeight)

        // Put moderate constraints on pan to stop complete out-of-screen loss
        _relativePanX.value = updatedX.coerceIn(-MAX_RELATIVE_PAN, MAX_RELATIVE_PAN)
        _relativePanY.value = updatedY.coerceIn(-MAX_RELATIVE_PAN, MAX_RELATIVE_PAN)
    }

    fun rotate90() {
        val base = _originalBitmap.value ?: return
        _isLoading.value = true
        viewModelScope.launch {
            val nextRot = (_rotationDegrees.value + QUARTER_TURN_DEGREES) % FULL_TURN_DEGREES
            _rotationDegrees.value = nextRot

            val rotated = withContext(defaultDispatcher) {
                ImageUtils.rotateBitmap(base, nextRot)
            }
            _rotatedBitmap.value = rotated
            _blurredBgBitmap.value = withContext(defaultDispatcher) {
                ImageUtils.createBlurredBitmap(rotated)
            }
            _isLoading.value = false
        }
    }

    fun setBackgroundType(type: BackgroundType) {
        _backgroundType.value = type
    }

    fun setCustomBackgroundColor(hexColor: Int) {
        _customBackgroundColor.value = hexColor
    }

    fun resetExportState() {
        _exportState.value = ExportState.Idle
    }

    fun exportFinalImage(context: Context, onComplete: (Uri?) -> Unit) {
        val rotated = _rotatedBitmap.value ?: return
        _exportState.value = ExportState.Exporting

        viewModelScope.launch {
            val ratioValue = _selectedAspectRatio.value.ratioValue
            val maxDim = maxOf(rotated.width, rotated.height).toFloat()

            val width: Int
            val height: Int
            if (ratioValue >= 1f) {
                width = maxDim.toInt()
                height = (maxDim / ratioValue).toInt()
            } else {
                height = maxDim.toInt()
                width = (maxDim * ratioValue).toInt()
            }

            val finalW = maxOf(MIN_EXPORT_DIMENSION, width)
            val finalH = maxOf(MIN_EXPORT_DIMENSION, height)

            val successUri = withContext(ioDispatcher) {
                try {
                    val exportBitmap = createBitmap(finalW, finalH, Bitmap.Config.ARGB_8888)
                    val canvas = android.graphics.Canvas(exportBitmap)

                    ImageUtils.drawComposedCanvas(
                        canvas = canvas,
                        renderSpec = CanvasRenderSpec(
                            size = CanvasSize(finalW.toFloat(), finalH.toFloat()),
                            images = CanvasImages(rotated, _blurredBgBitmap.value),
                            layoutMode = _layoutMode.value,
                            zoomScale = _zoomScale.value,
                            pan = RelativePan(_relativePanX.value, _relativePanY.value),
                            background = CanvasBackground(_backgroundType.value, _customBackgroundColor.value),
                        ),
                    )

                    val fileName = "FrameFix_${System.currentTimeMillis()}"
                    val uri = ImageUtils.saveBitmapToMediaStore(context, exportBitmap, fileName)
                    exportBitmap.recycle()
                    uri
                } catch (e: IllegalArgumentException) {
                    Log.e(TAG, "Failed to export image", e)
                    null
                } catch (e: IllegalStateException) {
                    Log.e(TAG, "Failed to export image", e)
                    null
                }
            }

            if (successUri != null) {
                _exportState.value = ExportState.Success(successUri)
                onComplete(successUri)
            } else {
                _exportState.value = ExportState.Error("Export failed: Could not compile and save image.")
                onComplete(null)
            }
        }
    }
}
