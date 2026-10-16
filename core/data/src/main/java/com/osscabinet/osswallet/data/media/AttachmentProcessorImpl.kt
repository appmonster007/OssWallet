package com.osscabinet.ossWallet.data.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.provider.OpenableColumns
import com.osscabinet.ossWallet.domain.model.AttachmentLimits
import com.osscabinet.ossWallet.domain.model.PreparedAttachment
import com.osscabinet.ossWallet.domain.repository.AttachmentProcessor
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

class AttachmentProcessorImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : AttachmentProcessor {

    override suspend fun prepare(source: String): PreparedAttachment = withContext(Dispatchers.Default) {
        val uri = Uri.parse(source)
        val resolver = context.contentResolver
        val name = displayNameOf(uri)

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth > 0 && bounds.outHeight > 0) {
            prepareImage(uri, bounds, name)
        } else {
            prepareFile(uri, name)
        }
    }

    private fun prepareFile(uri: Uri, name: String): PreparedAttachment {
        val declared = context.contentResolver.getType(uri)
        // A type that claims to be an image but didn't decode can't be previewed; don't pretend it can.
        val mime = when {
            declared == null || declared.startsWith("image/") -> AttachmentLimits.MIME_UNKNOWN
            else -> declared
        }
        val limit = AttachmentLimits.MAX_FILE_BYTES
        val bytes = context.contentResolver.openInputStream(uri)?.use { input ->
            val out = ByteArrayOutputStream()
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val n = input.read(buffer)
                if (n < 0) break
                out.write(buffer, 0, n)
                require(out.size() <= limit) { "That file is larger than ${limit / (1024 * 1024)} MB" }
            }
            out.toByteArray()
        } ?: error("Couldn't read the file")
        require(bytes.isNotEmpty()) { "That file is empty" }
        return PreparedAttachment(bytes, mime, name.ifBlank { "Attachment" })
    }

    private fun prepareImage(uri: Uri, bounds: BitmapFactory.Options, name: String): PreparedAttachment {
        val resolver = context.contentResolver
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
        val ours = isOurCapture(uri)
        deleteIfOurCapture(uri)
        val jpeg = ByteArrayOutputStream().use { out ->
            // Re-encoding drops EXIF, including any embedded GPS position.
            scaled.compress(Bitmap.CompressFormat.JPEG, AttachmentLimits.JPEG_QUALITY, out)
            scaled.recycle()
            out.toByteArray()
        }
        val label = if (ours || name.isBlank()) {
            "Photo " + java.text.SimpleDateFormat("yyyy-MM-dd HH-mm", java.util.Locale.US).format(java.util.Date())
        } else name.substringBeforeLast('.')
        return PreparedAttachment(jpeg, AttachmentLimits.MIME_JPEG, "$label.jpg")
    }

    private fun displayNameOf(uri: Uri): String = runCatching {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) c.getString(0) else null
        }
    }.getOrNull()?.trim().orEmpty()

    private fun isOurCapture(uri: Uri) = uri.authority == "${context.packageName}.fileprovider"

    /** Raw full-resolution camera captures are only a hand-off; don't leave them in the cache. */
    private fun deleteIfOurCapture(uri: Uri) {
        if (isOurCapture(uri)) {
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
