package com.osscabinet.ossWallet.data.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import com.osscabinet.ossWallet.domain.model.AttachmentLimits
import com.osscabinet.ossWallet.domain.repository.ReceiptImageProcessor
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

class ReceiptImageProcessorImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : ReceiptImageProcessor {

    override suspend fun prepare(source: String): ByteArray = withContext(Dispatchers.Default) {
        val uri = Uri.parse(source)
        val resolver = context.contentResolver

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        require(bounds.outWidth > 0 && bounds.outHeight > 0) { "That file isn't an image" }

        val decoded = resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply {
                inSampleSize = sampleSizeFor(bounds.outWidth, bounds.outHeight)
            })
        } ?: error("Couldn't read the image")

        val rotation = resolver.openInputStream(uri)?.use { exifRotation(it) } ?: 0
        val upright = if (rotation != 0) {
            Bitmap.createBitmap(
                decoded, 0, 0, decoded.width, decoded.height,
                Matrix().apply { postRotate(rotation.toFloat()) }, true
            ).also { if (it !== decoded) decoded.recycle() }
        } else decoded

        val scaled = scaleToEdge(upright)
        deleteIfOurCapture(uri)
        ByteArrayOutputStream().use { out ->
            // Re-encoding drops EXIF, including any embedded GPS position.
            scaled.compress(Bitmap.CompressFormat.JPEG, AttachmentLimits.JPEG_QUALITY, out)
            scaled.recycle()
            out.toByteArray()
        }
    }

    /** Raw full-resolution camera captures are only a hand-off; don't leave them in the cache. */
    private fun deleteIfOurCapture(uri: Uri) {
        if (uri.authority == "${context.packageName}.fileprovider") {
            runCatching { context.contentResolver.delete(uri, null, null) }
        }
    }

    private fun sampleSizeFor(w: Int, h: Int): Int {
        var sample = 1
        while (maxOf(w, h) / (sample * 2) >= AttachmentLimits.MAX_IMAGE_EDGE_PX) sample *= 2
        return sample
    }

    private fun scaleToEdge(bitmap: Bitmap): Bitmap {
        val longest = maxOf(bitmap.width, bitmap.height)
        if (longest <= AttachmentLimits.MAX_IMAGE_EDGE_PX) return bitmap
        val ratio = AttachmentLimits.MAX_IMAGE_EDGE_PX.toFloat() / longest
        return Bitmap.createScaledBitmap(
            bitmap, (bitmap.width * ratio).toInt(), (bitmap.height * ratio).toInt(), true
        ).also { if (it !== bitmap) bitmap.recycle() }
    }

    private fun exifRotation(stream: java.io.InputStream): Int = runCatching {
        when (ExifInterface(stream).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90
            ExifInterface.ORIENTATION_ROTATE_180 -> 180
            ExifInterface.ORIENTATION_ROTATE_270 -> 270
            else -> 0
        }
    }.getOrDefault(0)
}
