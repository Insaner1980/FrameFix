package com.insaner1980.framefix

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.insaner1980.framefix.model.BackgroundType
import com.insaner1980.framefix.model.LayoutMode
import com.insaner1980.framefix.util.CanvasBackground
import com.insaner1980.framefix.util.CanvasImages
import com.insaner1980.framefix.util.CanvasRenderSpec
import com.insaner1980.framefix.util.CanvasSize
import com.insaner1980.framefix.util.ImageUtils
import com.insaner1980.framefix.util.RelativePan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class ImageUtilsTest {

    @Test
    fun `full rotation returns original bitmap`() {
        val source = solidBitmap(width = 2, height = 3, color = Color.RED)

        val rotated = ImageUtils.rotateBitmap(source, 360)

        assertSame(source, rotated)
    }

    @Test
    fun `quarter rotation swaps bitmap dimensions`() {
        val source = solidBitmap(width = 2, height = 3, color = Color.BLUE)

        val rotated = ImageUtils.rotateBitmap(source, 90)

        assertEquals(3, rotated.width)
        assertEquals(2, rotated.height)
    }

    @Test
    fun `blurred bitmap uses fixed preview size and keeps solid color`() {
        val blurred = ImageUtils.createBlurredBitmap(solidBitmap(width = 4, height = 8, color = Color.RED))

        assertEquals(64, blurred.width)
        assertEquals(64, blurred.height)
        assertEquals(Color.RED, blurred.getPixel(32, 32))
    }

    @Test
    fun `draw composed canvas fills custom background when image is missing`() {
        val output = solidBitmap(width = 4, height = 4, color = Color.TRANSPARENT)

        ImageUtils.drawComposedCanvas(
            canvas = Canvas(output),
            renderSpec = renderSpec(
                width = 4f,
                height = 4f,
                images = CanvasImages(rotatedBitmap = null, blurredBgBitmap = null),
                background = CanvasBackground(BackgroundType.CUSTOM, Color.MAGENTA),
            ),
        )

        assertEquals(Color.MAGENTA, output.getPixel(2, 2))
    }

    @Test
    fun `draw composed canvas uses dark fallback when blurred background is missing`() {
        val output = solidBitmap(width = 4, height = 4, color = Color.TRANSPARENT)

        ImageUtils.drawComposedCanvas(
            canvas = Canvas(output),
            renderSpec = renderSpec(
                width = 4f,
                height = 4f,
                images = CanvasImages(rotatedBitmap = null, blurredBgBitmap = null),
                background = CanvasBackground(BackgroundType.BLUR, Color.TRANSPARENT),
            ),
        )

        assertEquals(Color.DKGRAY, output.getPixel(2, 2))
    }

    @Test
    fun `draw composed canvas stretches provided blurred background`() {
        val output = solidBitmap(width = 4, height = 2, color = Color.TRANSPARENT)
        val blurred = solidBitmap(width = 1, height = 1, color = Color.YELLOW)

        ImageUtils.drawComposedCanvas(
            canvas = Canvas(output),
            renderSpec = renderSpec(
                width = 4f,
                height = 2f,
                images = CanvasImages(rotatedBitmap = null, blurredBgBitmap = blurred),
                background = CanvasBackground(BackgroundType.BLUR, Color.TRANSPARENT),
            ),
        )

        assertEquals(Color.YELLOW, output.getPixel(0, 0))
        assertEquals(Color.YELLOW, output.getPixel(3, 1))
    }

    @Test
    fun `draw composed canvas centers fit image over background`() {
        val output = solidBitmap(width = 4, height = 2, color = Color.TRANSPARENT)
        val foreground = solidBitmap(width = 1, height = 1, color = Color.RED)

        ImageUtils.drawComposedCanvas(
            canvas = Canvas(output),
            renderSpec = renderSpec(
                width = 4f,
                height = 2f,
                images = CanvasImages(rotatedBitmap = foreground, blurredBgBitmap = null),
                layoutMode = LayoutMode.FIT,
                background = CanvasBackground(BackgroundType.BLACK, Color.TRANSPARENT),
            ),
        )

        assertEquals(Color.BLACK, output.getPixel(0, 1))
        assertEquals(Color.RED, output.getPixel(2, 1))
    }

    @Test
    fun `draw composed canvas scales fill image to cover frame`() {
        val output = solidBitmap(width = 4, height = 2, color = Color.TRANSPARENT)
        val foreground = solidBitmap(width = 1, height = 1, color = Color.GREEN)

        ImageUtils.drawComposedCanvas(
            canvas = Canvas(output),
            renderSpec = renderSpec(
                width = 4f,
                height = 2f,
                images = CanvasImages(rotatedBitmap = foreground, blurredBgBitmap = null),
                layoutMode = LayoutMode.FILL,
                background = CanvasBackground(BackgroundType.WHITE, Color.TRANSPARENT),
            ),
        )

        assertEquals(Color.GREEN, output.getPixel(0, 1))
        assertEquals(Color.GREEN, output.getPixel(3, 1))
    }

    @Test
    fun `load bitmap decodes file uri`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val file = writeBitmapFile(context, width = 5, height = 3, color = Color.CYAN)

        val bitmap = ImageUtils.loadBitmapFromUri(context, Uri.fromFile(file))

        assertNotNull(bitmap)
        assertEquals(5, bitmap?.width)
        assertEquals(3, bitmap?.height)
    }

    @Test
    fun `load bitmap downsamples large source`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val file = writeBitmapFile(context, width = 4096, height = 512, color = Color.LTGRAY)

        val bitmap = ImageUtils.loadBitmapFromUri(context, Uri.fromFile(file))

        assertNotNull(bitmap)
        assertEquals(2048, bitmap?.width)
        assertEquals(256, bitmap?.height)
    }

    @Test
    fun `load bitmap returns null for missing uri`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val missingFile = File(context.cacheDir, "missing-framefix-source.png")

        val bitmap = ImageUtils.loadBitmapFromUri(context, Uri.fromFile(missingFile))

        assertNull(bitmap)
    }

    @Test
    fun `save bitmap to media store returns content uri`() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        val uri =
            ImageUtils.saveBitmapToMediaStore(
                context,
                solidBitmap(width = 2, height = 2, color = Color.WHITE),
                "framefix-test",
            )

        assertNotNull(uri)
        assertEquals("content", uri?.scheme)
    }

    private fun solidBitmap(width: Int, height: Int, color: Int): Bitmap =
        Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply { eraseColor(color) }

    private fun writeBitmapFile(context: Context, width: Int, height: Int, color: Int): File {
        val file = File.createTempFile("framefix-source", ".png", context.cacheDir)
        file.outputStream().use { outputStream ->
            solidBitmap(width = width, height = height, color = color)
                .compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        }
        return file
    }

    private fun renderSpec(
        width: Float,
        height: Float,
        images: CanvasImages,
        layoutMode: LayoutMode = LayoutMode.FIT,
        zoomScale: Float = 1f,
        pan: RelativePan = RelativePan(0f, 0f),
        background: CanvasBackground,
    ): CanvasRenderSpec = CanvasRenderSpec(
        size = CanvasSize(width, height),
        images = images,
        layoutMode = layoutMode,
        zoomScale = zoomScale,
        pan = pan,
        background = background,
    )
}
