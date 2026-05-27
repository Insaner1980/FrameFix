package com.insaner1980.framefix.util

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale
import androidx.core.graphics.withClip
import com.insaner1980.framefix.model.BackgroundType
import com.insaner1980.framefix.model.LayoutMode
import java.io.IOException
import java.io.InputStream

data class CanvasSize(val width: Float, val height: Float)

data class CanvasImages(val rotatedBitmap: Bitmap?, val blurredBgBitmap: Bitmap?)

data class RelativePan(val x: Float, val y: Float)

data class CanvasBackground(val type: BackgroundType, val color: Int)

data class CanvasRenderSpec(
    val size: CanvasSize,
    val images: CanvasImages,
    val layoutMode: LayoutMode,
    val zoomScale: Float,
    val pan: RelativePan,
    val background: CanvasBackground,
)

object ImageUtils {
    private const val TAG = "ImageUtils"
    private const val FULL_ROTATION_DEGREES = 360
    private const val MAX_IMAGE_DIMENSION = 2048
    private const val SAMPLE_SIZE_BASE = 2
    private const val BLUR_TARGET_SIZE = 64
    private const val BLUR_RADIUS = 4
    private const val RED_SHIFT = 16
    private const val GREEN_SHIFT = 8
    private const val ALPHA_SHIFT = 24
    private const val COLOR_CHANNEL_MASK = 0xFF
    private const val JPEG_QUALITY = 95

    fun loadBitmapFromUri(context: Context, uri: Uri): Bitmap? = try {
        val contentResolver = context.contentResolver
        val inputStream: InputStream? = contentResolver.openInputStream(uri)
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeStream(inputStream, null, options)
        inputStream?.close()

        // To protect memory and stay fast, scale large images to max 2048px dimension
        var sampleSize = 1
        if (options.outWidth > MAX_IMAGE_DIMENSION || options.outHeight > MAX_IMAGE_DIMENSION) {
            val halfWidth = options.outWidth / SAMPLE_SIZE_BASE
            val halfHeight = options.outHeight / SAMPLE_SIZE_BASE
            while (
                (halfWidth / sampleSize) >= MAX_IMAGE_DIMENSION ||
                (halfHeight / sampleSize) >= MAX_IMAGE_DIMENSION
            ) {
                sampleSize *= SAMPLE_SIZE_BASE
            }
        }

        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inMutable = true
        }
        val decodeStream = contentResolver.openInputStream(uri)
        val result = BitmapFactory.decodeStream(decodeStream, null, decodeOptions)
        decodeStream?.close()
        result
    } catch (e: IOException) {
        Log.e(TAG, "Failed to load bitmap from uri: $uri", e)
        null
    } catch (e: SecurityException) {
        Log.e(TAG, "Failed to load bitmap from uri: $uri", e)
        null
    }

    fun rotateBitmap(source: Bitmap, degrees: Int): Bitmap {
        if (degrees % FULL_ROTATION_DEGREES == 0) return source
        return try {
            val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
            Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "Failed to rotate bitmap", e)
            source
        }
    }

    fun createBlurredBitmap(source: Bitmap): Bitmap = try {
        // Keep it small for super-fast offline box blur and perfect smooth look upon upscale
        val scaled = source.scale(BLUR_TARGET_SIZE, BLUR_TARGET_SIZE, true)
        blurBitmap(scaled, BLUR_RADIUS)
    } catch (e: IllegalArgumentException) {
        Log.e(TAG, "Failed to create blurred background", e)
        source
    }

    private fun blurBitmap(src: Bitmap, radius: Int): Bitmap {
        val width = src.width
        val height = src.height
        val pixels = IntArray(width * height)
        src.getPixels(pixels, 0, width, 0, 0, width, height)

        val outPixels = IntArray(width * height)
        boxBlurH(pixels, outPixels, width, height, radius)
        boxBlurV(outPixels, pixels, width, height, radius)

        val dest = createBitmap(width, height, Bitmap.Config.ARGB_8888)
        dest.setPixels(pixels, 0, width, 0, 0, width, height)
        return dest
    }

    private fun boxBlurH(s: IntArray, d: IntArray, w: Int, h: Int, r: Int) {
        for (y in 0 until h) {
            val rowOffset = y * w
            for (x in 0 until w) {
                var rSum = 0
                var gSum = 0
                var bSum = 0
                var count = 0
                for (dx in -r..r) {
                    val nx = x + dx
                    if (nx in 0 until w) {
                        val p = s[rowOffset + nx]
                        rSum += (p shr RED_SHIFT) and COLOR_CHANNEL_MASK
                        gSum += (p shr GREEN_SHIFT) and COLOR_CHANNEL_MASK
                        bSum += p and COLOR_CHANNEL_MASK
                        count++
                    }
                }
                d[rowOffset + x] =
                    (COLOR_CHANNEL_MASK shl ALPHA_SHIFT) or
                    ((rSum / count) shl RED_SHIFT) or
                    ((gSum / count) shl GREEN_SHIFT) or
                    (bSum / count)
            }
        }
    }

    private fun boxBlurV(s: IntArray, d: IntArray, w: Int, h: Int, r: Int) {
        for (x in 0 until w) {
            for (y in 0 until h) {
                var rSum = 0
                var gSum = 0
                var bSum = 0
                var count = 0
                for (dy in -r..r) {
                    val ny = y + dy
                    if (ny in 0 until h) {
                        val p = s[ny * w + x]
                        rSum += (p shr RED_SHIFT) and COLOR_CHANNEL_MASK
                        gSum += (p shr GREEN_SHIFT) and COLOR_CHANNEL_MASK
                        bSum += p and COLOR_CHANNEL_MASK
                        count++
                    }
                }
                d[y * w + x] =
                    (COLOR_CHANNEL_MASK shl ALPHA_SHIFT) or
                    ((rSum / count) shl RED_SHIFT) or
                    ((gSum / count) shl GREEN_SHIFT) or
                    (bSum / count)
            }
        }
    }

    fun drawComposedCanvas(canvas: Canvas, renderSpec: CanvasRenderSpec) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        drawBackground(canvas, renderSpec, paint)
        renderSpec.images.rotatedBitmap?.let { rotatedBitmap ->
            drawForegroundImage(canvas, renderSpec, rotatedBitmap, paint)
        }
    }

    fun saveBitmapToMediaStore(context: Context, bitmap: Bitmap, fileName: String): Uri? {
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "$fileName.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/FrameFix")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

        var savedUri = uri

        if (savedUri != null) {
            try {
                resolver.openOutputStream(savedUri)?.use { outputStream ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, outputStream)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.clear()
                    contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                    resolver.update(savedUri, contentValues, null, null)
                }
            } catch (e: IOException) {
                Log.e(TAG, "Failed to save bitmap to MediaStore", e)
                deleteFailedExport(context, savedUri)
                savedUri = null
            } catch (e: SecurityException) {
                Log.e(TAG, "Failed to save bitmap to MediaStore", e)
                deleteFailedExport(context, savedUri)
                savedUri = null
            }
        }
        return savedUri
    }

    private fun deleteFailedExport(context: Context, uri: Uri) {
        try {
            context.contentResolver.delete(uri, null, null)
        } catch (e: IllegalArgumentException) {
            Log.w(TAG, "Failed to delete incomplete export: $uri", e)
        } catch (e: SecurityException) {
            Log.w(TAG, "Failed to delete incomplete export: $uri", e)
        }
    }
}

private fun drawBackground(canvas: Canvas, renderSpec: CanvasRenderSpec, paint: Paint) {
    when (renderSpec.background.type) {
        BackgroundType.BLACK -> canvas.drawColor(android.graphics.Color.BLACK)

        BackgroundType.WHITE -> canvas.drawColor(android.graphics.Color.WHITE)

        BackgroundType.CUSTOM -> canvas.drawColor(renderSpec.background.color)

        BackgroundType.BLUR -> {
            drawBlurredBackground(canvas, renderSpec, paint)
        }
    }
}

private fun drawBlurredBackground(canvas: Canvas, renderSpec: CanvasRenderSpec, paint: Paint) {
    val blurredBgBitmap = renderSpec.images.blurredBgBitmap
    if (blurredBgBitmap != null) {
        val canvasW = renderSpec.size.width
        val canvasH = renderSpec.size.height
        val bgScale = maxOf(canvasW / blurredBgBitmap.width, canvasH / blurredBgBitmap.height)
        val bgDrawW = blurredBgBitmap.width * bgScale
        val bgDrawH = blurredBgBitmap.height * bgScale
        val bgX = (canvasW - bgDrawW) / 2f
        val bgY = (canvasH - bgDrawH) / 2f
        val bgDestRect = RectF(bgX, bgY, bgX + bgDrawW, bgY + bgDrawH)
        canvas.drawBitmap(blurredBgBitmap, null, bgDestRect, paint)
    } else {
        canvas.drawColor(android.graphics.Color.DKGRAY)
    }
}

private fun drawForegroundImage(canvas: Canvas, renderSpec: CanvasRenderSpec, rotatedBitmap: Bitmap, paint: Paint) {
    val canvasW = renderSpec.size.width
    val canvasH = renderSpec.size.height
    val imgW = rotatedBitmap.width.toFloat()
    val imgH = rotatedBitmap.height.toFloat()

    val scaleFit = minOf(canvasW / imgW, canvasH / imgH)
    val scaleFill = maxOf(canvasW / imgW, canvasH / imgH)
    val baseScale = if (renderSpec.layoutMode == LayoutMode.FIT) scaleFit else scaleFill
    val finalScale = baseScale * renderSpec.zoomScale
    val drawW = imgW * finalScale
    val drawH = imgH * finalScale
    val unpanX = (canvasW - drawW) / 2f
    val unpanY = (canvasH - drawH) / 2f
    val actualPanX = renderSpec.pan.x * canvasW
    val actualPanY = renderSpec.pan.y * canvasH

    val destRect = RectF(
        unpanX + actualPanX,
        unpanY + actualPanY,
        unpanX + actualPanX + drawW,
        unpanY + actualPanY + drawH,
    )

    canvas.withClip(0f, 0f, canvasW, canvasH) {
        canvas.drawBitmap(rotatedBitmap, null, destRect, paint)
    }
}
