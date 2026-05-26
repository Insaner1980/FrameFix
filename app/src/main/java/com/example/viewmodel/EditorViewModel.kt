package com.example.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.AspectRatio
import com.example.model.BackgroundType
import com.example.model.LayoutMode
import com.example.util.ImageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class ExportState {
    object Idle : ExportState()
    object Exporting : ExportState()
    data class Success(val uri: Uri) : ExportState()
    data class Error(val message: String) : ExportState()
}

class EditorViewModel : ViewModel() {

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

    private val _zoomScale = MutableStateFlow(1f)
    val zoomScale = _zoomScale.asStateFlow()

    private val _relativePanX = MutableStateFlow(0f)
    val relativePanX = _relativePanX.asStateFlow()

    private val _relativePanY = MutableStateFlow(0f)
    val relativePanY = _relativePanY.asStateFlow()

    private val _rotationDegrees = MutableStateFlow(0)
    val rotationDegrees = _rotationDegrees.asStateFlow()

    private val _backgroundType = MutableStateFlow(BackgroundType.BLUR)
    val backgroundType = _backgroundType.asStateFlow()

    private val _customBackgroundColor = MutableStateFlow(android.graphics.Color.parseColor("#1e293b")) // Slate
    val customBackgroundColor = _customBackgroundColor.asStateFlow()

    private val _exportState = MutableStateFlow<ExportState>(ExportState.Idle)
    val exportState = _exportState.asStateFlow()

    fun loadImage(context: Context, uri: Uri) {
        _isLoading.value = true
        _exportState.value = ExportState.Idle
        _originalUri.value = uri
        
        // Reset transforms
        _zoomScale.value = 1f
        _relativePanX.value = 0f
        _relativePanY.value = 0f
        _rotationDegrees.value = 0

        viewModelScope.launch {
            val bitmap = withContext(Dispatchers.IO) {
                ImageUtils.loadBitmapFromUri(context, uri)
            }
            if (bitmap != null) {
                _originalBitmap.value = bitmap
                _rotatedBitmap.value = bitmap
                _blurredBgBitmap.value = withContext(Dispatchers.Default) {
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
        _zoomScale.value = 1f
    }

    fun setLayoutMode(mode: LayoutMode) {
        _layoutMode.value = mode
        // Reset pan & zoom to fit constraints naturally
        _relativePanX.value = 0f
        _relativePanY.value = 0f
        _zoomScale.value = 1f
    }

    fun updateZoom(zoom: Float) {
        // Limit zoom between 0.5x and 8.0x
        _zoomScale.value = zoom.coerceIn(0.5f, 8.0f)
    }

    fun updatePan(deltaX: Float, deltaY: Float, viewWidth: Float, viewHeight: Float) {
        if (viewWidth <= 0f || viewHeight <= 0f) return
        
        // Accumulate relative drag changes
        val updatedX = _relativePanX.value + (deltaX / viewWidth)
        val updatedY = _relativePanY.value + (deltaY / viewHeight)
        
        // Put moderate constraints on pan to stop complete out-of-screen loss
        _relativePanX.value = updatedX.coerceIn(-2.0f, 2.0f)
        _relativePanY.value = updatedY.coerceIn(-2.0f, 2.0f)
    }

    fun rotate90() {
        val base = _originalBitmap.value ?: return
        _isLoading.value = true
        viewModelScope.launch {
            val nextRot = (_rotationDegrees.value + 90) % 360
            _rotationDegrees.value = nextRot

            val rotated = withContext(Dispatchers.Default) {
                ImageUtils.rotateBitmap(base, nextRot)
            }
            _rotatedBitmap.value = rotated
            _blurredBgBitmap.value = withContext(Dispatchers.Default) {
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
            val R = _selectedAspectRatio.value.ratioValue
            val maxDim = maxOf(rotated.width, rotated.height).toFloat()
            
            val width: Int
            val height: Int
            if (R >= 1f) {
                width = maxDim.toInt()
                height = (maxDim / R).toInt()
            } else {
                height = maxDim.toInt()
                width = (maxDim * R).toInt()
            }
            
            val finalW = maxOf(100, width)
            val finalH = maxOf(100, height)

            val successUri = withContext(Dispatchers.IO) {
                try {
                    val exportBitmap = Bitmap.createBitmap(finalW, finalH, Bitmap.Config.ARGB_8888)
                    val canvas = android.graphics.Canvas(exportBitmap)
                    
                    ImageUtils.drawComposedCanvas(
                        canvas = canvas,
                        canvasW = finalW.toFloat(),
                        canvasH = finalH.toFloat(),
                        rotatedBitmap = rotated,
                        blurredBgBitmap = _blurredBgBitmap.value,
                        layoutMode = _layoutMode.value,
                        zoomScale = _zoomScale.value,
                        relativePanX = _relativePanX.value,
                        relativePanY = _relativePanY.value,
                        bgType = _backgroundType.value,
                        bgColor = _customBackgroundColor.value
                    )
                    
                    val fileName = "FrameFix_${System.currentTimeMillis()}"
                    val uri = ImageUtils.saveBitmapToMediaStore(context, exportBitmap, fileName)
                    exportBitmap.recycle()
                    uri
                } catch (e: Exception) {
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
