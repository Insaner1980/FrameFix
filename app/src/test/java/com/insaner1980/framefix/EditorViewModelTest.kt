package com.insaner1980.framefix

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Looper
import com.insaner1980.framefix.model.AspectRatio
import com.insaner1980.framefix.model.BackgroundType
import com.insaner1980.framefix.model.LayoutMode
import com.insaner1980.framefix.viewmodel.EditorViewModel
import com.insaner1980.framefix.viewmodel.ExportState
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class EditorViewModelTest {

    @Test
    fun `initial state uses editor defaults`() {
        val viewModel = EditorViewModel()

        assertFalse(viewModel.isLoading.value)
        assertNull(viewModel.originalUri.value)
        assertNull(viewModel.originalBitmap.value)
        assertNull(viewModel.rotatedBitmap.value)
        assertNull(viewModel.blurredBgBitmap.value)
        assertEquals(AspectRatio.Ratio9_16, viewModel.selectedAspectRatio.value)
        assertEquals(LayoutMode.FIT, viewModel.layoutMode.value)
        assertEquals(1f, viewModel.zoomScale.value, 0.001f)
        assertEquals(0f, viewModel.relativePanX.value, 0.001f)
        assertEquals(0f, viewModel.relativePanY.value, 0.001f)
        assertEquals(0, viewModel.rotationDegrees.value)
        assertEquals(BackgroundType.BLUR, viewModel.backgroundType.value)
        assertEquals(0xFF1E293B.toInt(), viewModel.customBackgroundColor.value)
        assertEquals(ExportState.Idle, viewModel.exportState.value)
    }

    @Test
    fun `clear image resets editor state to home`() {
        val viewModel = EditorViewModel()

        viewModel.clearImage()

        assertNull(viewModel.originalUri.value)
        assertNull(viewModel.originalBitmap.value)
        assertNull(viewModel.rotatedBitmap.value)
        assertNull(viewModel.blurredBgBitmap.value)
        assertFalse(viewModel.isLoading.value)
        assertEquals(ExportState.Idle, viewModel.exportState.value)
    }

    @Test
    fun `select aspect ratio resets pan and zoom`() {
        val viewModel = EditorViewModel()
        viewModel.updateZoom(4f)
        viewModel.updatePan(deltaX = 50f, deltaY = -25f, viewWidth = 100f, viewHeight = 100f)

        viewModel.selectAspectRatio(AspectRatio.Ratio1_1)

        assertEquals(AspectRatio.Ratio1_1, viewModel.selectedAspectRatio.value)
        assertEquals(1f, viewModel.zoomScale.value, 0.001f)
        assertEquals(0f, viewModel.relativePanX.value, 0.001f)
        assertEquals(0f, viewModel.relativePanY.value, 0.001f)
    }

    @Test
    fun `set layout mode resets pan and zoom`() {
        val viewModel = EditorViewModel()
        viewModel.updateZoom(3f)
        viewModel.updatePan(deltaX = -40f, deltaY = 80f, viewWidth = 100f, viewHeight = 100f)

        viewModel.setLayoutMode(LayoutMode.FILL)

        assertEquals(LayoutMode.FILL, viewModel.layoutMode.value)
        assertEquals(1f, viewModel.zoomScale.value, 0.001f)
        assertEquals(0f, viewModel.relativePanX.value, 0.001f)
        assertEquals(0f, viewModel.relativePanY.value, 0.001f)
    }

    @Test
    fun `zoom is clamped to supported range`() {
        val viewModel = EditorViewModel()

        viewModel.updateZoom(0.1f)
        assertEquals(0.5f, viewModel.zoomScale.value, 0.001f)

        viewModel.updateZoom(12f)
        assertEquals(8f, viewModel.zoomScale.value, 0.001f)

        viewModel.updateZoom(2.25f)
        assertEquals(2.25f, viewModel.zoomScale.value, 0.001f)
    }

    @Test
    fun `pan ignores invalid viewport and clamps valid movement`() {
        val viewModel = EditorViewModel()

        viewModel.updatePan(deltaX = 50f, deltaY = 50f, viewWidth = 0f, viewHeight = 100f)
        assertEquals(0f, viewModel.relativePanX.value, 0.001f)
        assertEquals(0f, viewModel.relativePanY.value, 0.001f)

        viewModel.updatePan(deltaX = 500f, deltaY = -500f, viewWidth = 100f, viewHeight = 100f)
        assertEquals(2f, viewModel.relativePanX.value, 0.001f)
        assertEquals(-2f, viewModel.relativePanY.value, 0.001f)
    }

    @Test
    fun `background controls update current selections`() {
        val viewModel = EditorViewModel()
        val customColor = 0xFF336699.toInt()

        viewModel.setBackgroundType(BackgroundType.CUSTOM)
        viewModel.setCustomBackgroundColor(customColor)

        assertEquals(BackgroundType.CUSTOM, viewModel.backgroundType.value)
        assertEquals(customColor, viewModel.customBackgroundColor.value)
    }

    @Test
    fun `load image stores decoded bitmap and resets transforms`() {
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<Context>()
        val uri = Uri.fromFile(writeBitmapFile(context, width = 6, height = 4, color = Color.CYAN))
        val viewModel = testViewModel()
        viewModel.updateZoom(4f)
        viewModel.updatePan(deltaX = 50f, deltaY = -25f, viewWidth = 100f, viewHeight = 100f)

        viewModel.loadImage(context, uri)
        idleMainLooper()

        assertFalse(viewModel.isLoading.value)
        assertEquals(uri, viewModel.originalUri.value)
        assertEquals(6, viewModel.originalBitmap.value?.width)
        assertEquals(4, viewModel.rotatedBitmap.value?.height)
        assertEquals(64, viewModel.blurredBgBitmap.value?.width)
        assertEquals(1f, viewModel.zoomScale.value, 0.001f)
        assertEquals(0f, viewModel.relativePanX.value, 0.001f)
        assertEquals(0f, viewModel.relativePanY.value, 0.001f)
    }

    @Test
    fun `load image clears loading when decode fails`() {
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<Context>()
        val uri = Uri.fromFile(File(context.cacheDir, "missing-viewmodel-source.png"))
        val viewModel = testViewModel()

        viewModel.loadImage(context, uri)
        idleMainLooper()

        assertFalse(viewModel.isLoading.value)
        assertEquals(uri, viewModel.originalUri.value)
        assertNull(viewModel.originalBitmap.value)
        assertNull(viewModel.rotatedBitmap.value)
        assertNull(viewModel.blurredBgBitmap.value)
    }

    @Test
    fun `rotate loaded image updates rotation bitmap and blur`() {
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<Context>()
        val viewModel = testViewModel()
        viewModel.loadImage(context, Uri.fromFile(writeBitmapFile(context, width = 2, height = 3, color = Color.BLUE)))
        idleMainLooper()

        viewModel.rotate90()
        idleMainLooper()

        assertFalse(viewModel.isLoading.value)
        assertEquals(90, viewModel.rotationDegrees.value)
        assertEquals(3, viewModel.rotatedBitmap.value?.width)
        assertEquals(2, viewModel.rotatedBitmap.value?.height)
        assertEquals(64, viewModel.blurredBgBitmap.value?.height)
    }

    @Test
    fun `rotate without loaded image leaves state idle`() {
        val viewModel = EditorViewModel()

        viewModel.rotate90()

        assertEquals(0, viewModel.rotationDegrees.value)
        assertFalse(viewModel.isLoading.value)
    }

    @Test
    fun `export without loaded image does not complete`() {
        val viewModel = EditorViewModel()
        var completed = false

        viewModel.exportFinalImage(context = androidx.test.core.app.ApplicationProvider.getApplicationContext()) {
            completed = true
        }

        assertFalse(completed)
        assertEquals(ExportState.Idle, viewModel.exportState.value)
    }

    private fun testViewModel(): EditorViewModel = EditorViewModel(
        ioDispatcher = Dispatchers.Unconfined,
        defaultDispatcher = Dispatchers.Unconfined,
    )

    private fun writeBitmapFile(context: Context, width: Int, height: Int, color: Int): File {
        val file = File.createTempFile("framefix-viewmodel-source", ".png", context.cacheDir)
        Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            .apply { eraseColor(color) }
            .compressTo(file)
        return file
    }

    private fun Bitmap.compressTo(file: File) {
        file.outputStream().use { outputStream ->
            compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        }
    }

    private fun idleMainLooper() {
        shadowOf(Looper.getMainLooper()).idle()
    }
}
