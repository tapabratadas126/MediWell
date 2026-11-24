package com.mediwell.app.data.ocr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.Log
import java.io.ByteArrayOutputStream
import java.io.InputStream
import kotlin.math.max

object ImageUtils {
    private const val TAG = "ImageUtils"

    /**
     * Safely reads and downsamples an image from a Uri into a Base64 string suitable for OCR.space.
     * Prevents OutOfMemoryErrors and ensures the payload does not exceed API limits.
     */
    fun uriToBase64(context: Context, uri: Uri, maxDimension: Int = 1600): String? {
        return try {
            val contentResolver = context.contentResolver

            // 1. Decode bounds only to calculate scale factor
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            }

            val width = options.outWidth
            val height = options.outHeight
            if (width <= 0 || height <= 0) {
                Log.e(TAG, "Invalid image dimensions: $width x $height")
                return null
            }

            // Calculate inSampleSize
            var sampleSize = 1
            val maxSide = max(width, height)
            while (maxSide / sampleSize > maxDimension * 1.5) {
                sampleSize *= 2
            }

            // 2. Decode the downscaled bitmap
            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.RGB_565
            }

            val decodedBitmap: Bitmap? = contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOptions)
            } ?: return null

            // 3. Scale precisely if still larger than maxDimension
            val currentMax = max(decodedBitmap.width, decodedBitmap.height)
            val finalBitmap = if (currentMax > maxDimension) {
                val scale = maxDimension.toFloat() / currentMax.toFloat()
                val targetW = (decodedBitmap.width * scale).toInt()
                val targetH = (decodedBitmap.height * scale).toInt()
                val scaled = Bitmap.createScaledBitmap(decodedBitmap, targetW, targetH, true)
                if (scaled != decodedBitmap) {
                    decodedBitmap.recycle()
                }
                scaled
            } else {
                decodedBitmap
            }

            val outputStream = ByteArrayOutputStream()
            finalBitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
            val bytes = outputStream.toByteArray()
            finalBitmap.recycle()

            Base64.encodeToString(bytes, Base64.NO_WRAP)
        } catch (e: Throwable) {
            Log.e(TAG, "Error converting image Uri to Base64: ${e.message}", e)
            null
        }
    }

    /**
     * Safely compresses a Bitmap from camera preview into Base64.
     */
    fun bitmapToBase64(bitmap: Bitmap, maxDimension: Int = 1600): String? {
        return try {
            val currentMax = max(bitmap.width, bitmap.height)
            val finalBitmap = if (currentMax > maxDimension) {
                val scale = maxDimension.toFloat() / currentMax.toFloat()
                val targetW = (bitmap.width * scale).toInt()
                val targetH = (bitmap.height * scale).toInt()
                Bitmap.createScaledBitmap(bitmap, targetW, targetH, true)
            } else {
                bitmap
            }

            val outputStream = ByteArrayOutputStream()
            finalBitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
            val bytes = outputStream.toByteArray()
            if (finalBitmap != bitmap) {
                finalBitmap.recycle()
            }
            Base64.encodeToString(bytes, Base64.NO_WRAP)
        } catch (e: Throwable) {
            Log.e(TAG, "Error converting Bitmap to Base64: ${e.message}", e)
            null
        }
    }
}
