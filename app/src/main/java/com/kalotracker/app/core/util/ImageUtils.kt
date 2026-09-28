package com.kalotracker.app.core.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.camera.core.ImageProxy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer

data class ProcessedImage(
    val localUri: String,
    val compressedBytes: ByteArray
)

object ImageUtils {

    private const val MAX_DIMENSION = 1024
    private const val JPEG_QUALITY = 80

    /**
     * Converts CameraX ImageProxy to an efficiently downscaled and compressed JPEG,
     * writes it to app internal storage for local caching/thumbnails,
     * and returns the local file URI and bytes for API transmission.
     */
    suspend fun processAndSaveImage(
        context: Context,
        imageProxy: ImageProxy
    ): ProcessedImage = withContext(Dispatchers.IO) {
        val rotationDegrees = imageProxy.imageInfo.rotationDegrees
        val buffer: ByteBuffer = imageProxy.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        imageProxy.close()

        processBytes(context, bytes, rotationDegrees)
    }

    /**
     * Downscales and compresses raw image bytes, saving to local app storage.
     */
    suspend fun processBytes(
        context: Context,
        rawBytes: ByteArray,
        rotationDegrees: Int = 0
    ): ProcessedImage = withContext(Dispatchers.IO) {
        // Decode bounds only first to check memory requirements
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, options)

        // Calculate inSampleSize — downsample until BOTH dimensions fit within MAX_DIMENSION
        val (width, height) = options.outWidth to options.outHeight
        var sampleSize = 1
        if (width > MAX_DIMENSION || height > MAX_DIMENSION) {
            while ((width / (sampleSize * 2)) > MAX_DIMENSION || (height / (sampleSize * 2)) > MAX_DIMENSION) {
                sampleSize *= 2
            }
        }

        // Decode downsampled bitmap
        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.RGB_565 // Half memory compared to ARGB_8888
        }
        val sampledBitmap = BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, decodeOptions)
            ?: throw IllegalStateException("Could not decode image bytes")

        // Rotate if camera orientation requires it
        val finalBitmap = if (rotationDegrees != 0) {
            val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
            val rotated = Bitmap.createBitmap(
                sampledBitmap, 0, 0, sampledBitmap.width, sampledBitmap.height, matrix, true
            )
            if (rotated != sampledBitmap) {
                sampledBitmap.recycle()
            }
            rotated
        } else {
            sampledBitmap
        }

        // Compress to JPEG
        val outputStream = ByteArrayOutputStream()
        finalBitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, outputStream)
        val compressedBytes = outputStream.toByteArray()

        // Save to internal storage: filesDir/meals/meal_{epoch}.jpg
        val mealsDir = File(context.filesDir, "meals").apply {
            if (!exists()) mkdirs()
        }
        val file = File(mealsDir, "meal_${System.currentTimeMillis()}.jpg")
        FileOutputStream(file).use { fos ->
            fos.write(compressedBytes)
            fos.flush()
        }

        finalBitmap.recycle()

        ProcessedImage(
            localUri = file.absolutePath,
            compressedBytes = compressedBytes
        )
    }
}
