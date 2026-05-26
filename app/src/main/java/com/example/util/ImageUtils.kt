package com.example.util

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import com.example.model.BackgroundType
import com.example.model.LayoutMode
import java.io.InputStream

object ImageUtils {
    private const val TAG = "ImageUtils"

    fun loadBitmapFromUri(context: Context, uri: Uri): Bitmap? {
        return try {
            val contentResolver = context.contentResolver
            val inputStream: InputStream? = contentResolver.openInputStream(uri)
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeStream(inputStream, null, options)
            inputStream?.close()

            // To protect memory and stay fast, scale large images to max 2048px dimension
            val maxDimension = 2048
            var sampleSize = 1
            if (options.outWidth > maxDimension || options.outHeight > maxDimension) {
                val halfWidth = options.outWidth / 2
                val halfHeight = options.outHeight / 2
                while ((halfWidth / sampleSize) >= maxDimension || (halfHeight / sampleSize) >= maxDimension) {
                    sampleSize *= 2
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
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load bitmap from uri: $uri", e)
            null
        }
    }

    fun rotateBitmap(source: Bitmap, degrees: Int): Bitmap {
        if (degrees % 360 == 0) return source
        return try {
            val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
            Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to rotate bitmap", e)
            source
        }
    }

    fun createBlurredBitmap(source: Bitmap): Bitmap {
        return try {
            // Keep it small for super-fast offline box blur and perfect smooth look upon upscale
            val targetSize = 64
            val scaled = Bitmap.createScaledBitmap(source, targetSize, targetSize, true)
            blurBitmap(scaled, 4)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create blurred background", e)
            source
        }
    }

    private fun blurBitmap(src: Bitmap, radius: Int): Bitmap {
        val width = src.width
        val height = src.height
        val pixels = IntArray(width * height)
        src.getPixels(pixels, 0, width, 0, 0, width, height)

        val outPixels = IntArray(width * height)
        boxBlurH(pixels, outPixels, width, height, radius)
        boxBlurV(outPixels, pixels, width, height, radius)

        val dest = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
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
                        rSum += (p shr 16) and 0xFF
                        gSum += (p shr 8) and 0xFF
                        bSum += p and 0xFF
                        count++
                    }
                }
                d[rowOffset + x] = (0xFF shl 24) or ((rSum / count) shl 16) or ((gSum / count) shl 8) or (bSum / count)
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
                        rSum += (p shr 16) and 0xFF
                        gSum += (p shr 8) and 0xFF
                        bSum += p and 0xFF
                        count++
                    }
                }
                d[y * w + x] = (0xFF shl 24) or ((rSum / count) shl 16) or ((gSum / count) shl 8) or (bSum / count)
            }
        }
    }

    fun drawComposedCanvas(
        canvas: android.graphics.Canvas,
        canvasW: Float,
        canvasH: Float,
        rotatedBitmap: Bitmap?,
        blurredBgBitmap: Bitmap?,
        layoutMode: LayoutMode,
        zoomScale: Float,
        relativePanX: Float,
        relativePanY: Float,
        bgType: BackgroundType,
        bgColor: Int
    ) {
        // Build the drawing paint with bilinear interpolation
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        // 1. Render Background
        when (bgType) {
            BackgroundType.BLACK -> canvas.drawColor(android.graphics.Color.BLACK)
            BackgroundType.WHITE -> canvas.drawColor(android.graphics.Color.WHITE)
            BackgroundType.CUSTOM -> canvas.drawColor(bgColor)
            BackgroundType.BLUR -> {
                if (blurredBgBitmap != null) {
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
        }

        // 2. Render Rotated Main Image
        if (rotatedBitmap != null) {
            val imgW = rotatedBitmap.width.toFloat()
            val imgH = rotatedBitmap.height.toFloat()

            val scaleFit = minOf(canvasW / imgW, canvasH / imgH)
            val scaleFill = maxOf(canvasW / imgW, canvasH / imgH)

            val baseScale = if (layoutMode == LayoutMode.FIT) scaleFit else scaleFill
            val finalScale = baseScale * zoomScale

            val drawW = imgW * finalScale
            val drawH = imgH * finalScale

            val unpanX = (canvasW - drawW) / 2f
            val unpanY = (canvasH - drawH) / 2f

            // Offset translate using relative pan scale fractions
            val actualPanX = relativePanX * canvasW
            val actualPanY = relativePanY * canvasH

            val destRect = RectF(
                unpanX + actualPanX,
                unpanY + actualPanY,
                unpanX + actualPanX + drawW,
                unpanY + actualPanY + drawH
            )

            canvas.save()
            canvas.clipRect(0f, 0f, canvasW, canvasH)
            canvas.drawBitmap(rotatedBitmap, null, destRect, paint)
            canvas.restore()
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

        if (uri != null) {
            try {
                resolver.openOutputStream(uri)?.use { outputStream ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 95, outputStream)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.clear()
                    contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                    resolver.update(uri, contentValues, null, null)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save bitmap to MediaStore", e)
                try {
                    resolver.delete(uri, null, null)
                } catch (de: Exception) {
                    // Ignore
                }
                return null
            }
        }
        return uri
    }
}
