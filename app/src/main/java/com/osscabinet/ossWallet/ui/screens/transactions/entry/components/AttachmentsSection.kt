package com.osscabinet.ossWallet.ui.screens.transactions.entry.components

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.view.WindowManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddAPhoto
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.content.FileProvider
import com.osscabinet.ossWallet.ui.components.entry.EntrySection
import com.osscabinet.ossWallet.ui.screens.transactions.entry.AttachmentItem
import com.osscabinet.ossWallet.ui.screens.transactions.entry.AttachmentsUiState
import com.osscabinet.ossWallet.ui.theme.AppColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private val ThumbSize = 76.dp
private val FileTileWidth = 132.dp

/** Files attached to the transaction being edited: add a photo or any document, open, remove. */
@Composable
fun AttachmentsSection(
    state: AttachmentsUiState,
    onPicked: (String) -> Unit,
    onRemove: (AttachmentItem) -> Unit,
    onDismissError: () -> Unit,
    readBytes: suspend (AttachmentItem) -> ByteArray?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var menuOpen by remember { mutableStateOf(false) }
    var viewing by remember { mutableStateOf<AttachmentItem?>(null) }
    var cameraUri by remember { mutableStateOf<Uri?>(null) }
    var cameraMissing by remember { mutableStateOf(false) }

    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val uri = cameraUri
        if (ok && uri != null) onPicked(uri.toString())
        cameraUri = null
    }
    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) onPicked(uri.toString())
    }
    val pickFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) onPicked(uri.toString())
    }
    val scope = rememberCoroutineScope()
    var openError by remember { mutableStateOf<String?>(null) }

    fun launchCamera() {
        val dir = File(context.cacheDir, "shared").apply { mkdirs() }
        val file = File(dir, "capture_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        cameraUri = uri
        try {
            takePicture.launch(uri)
        } catch (_: ActivityNotFoundException) {
            cameraUri = null
            cameraMissing = true
        }
    }

    EntrySection(title = "ATTACHMENTS", icon = Icons.Rounded.AttachFile, modifier = modifier) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(state.items, key = { it.key }) { item ->
                AttachmentTile(
                    item, readBytes,
                    onClick = {
                        if (item.isImage) viewing = item
                        else scope.launch {
                            if (!openExternally(context, item, readBytes)) openError = "No app on this device can open that file"
                        }
                    },
                    onRemove = { onRemove(item) }
                )
            }
            if (state.canAdd) {
                item(key = "add") {
                    Box {
                        Box(
                            modifier = Modifier
                                .size(ThumbSize)
                                .clip(RoundedCornerShape(12.dp))
                                .background(AppColors.surfaceContainerHigh)
                                .clickable(enabled = !state.busy) { menuOpen = true }
                                .semanticsLabel("Add attachment"),
                            contentAlignment = Alignment.Center
                        ) {
                            if (state.busy) CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                            else Icon(Icons.Rounded.AttachFile, contentDescription = null, tint = AppColors.primary)
                        }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            DropdownMenuItem(
                                text = { Text("Take photo") },
                                leadingIcon = { Icon(Icons.Rounded.PhotoCamera, null) },
                                onClick = { menuOpen = false; launchCamera() }
                            )
                            DropdownMenuItem(
                                text = { Text("Choose photo") },
                                leadingIcon = { Icon(Icons.Rounded.PhotoLibrary, null) },
                                onClick = {
                                    menuOpen = false
                                    pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Attach file") },
                                leadingIcon = { Icon(Icons.Rounded.Description, null) },
                                onClick = { menuOpen = false; pickFile.launch(arrayOf("*/*")) }
                            )
                        }
                    }
                }
            }
        }
        val message = state.error ?: openError ?: if (cameraMissing) "No camera app available" else null
        if (message != null) {
            Text(
                message,
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.error,
                modifier = Modifier.clickable { cameraMissing = false; openError = null; onDismissError() }
            )
        } else if (state.items.isEmpty()) {
            Text(
                "Attach receipts, invoices, PDFs or any document. They're stored encrypted on this device.",
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.onSurfaceVariant
            )
        }
    }

    viewing?.let { item -> ImageViewer(item, readBytes, onDismiss = { viewing = null }) }
}

@Composable
private fun AttachmentTile(
    item: AttachmentItem,
    readBytes: suspend (AttachmentItem) -> ByteArray?,
    onClick: () -> Unit,
    onRemove: () -> Unit
) {
    val bitmap = if (item.isImage) rememberAttachmentBitmap(item, readBytes, sampleSize = 4) else null
    Box(Modifier.size(width = if (item.isImage) ThumbSize else FileTileWidth, height = ThumbSize)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(12.dp))
                .background(AppColors.surfaceContainerHigh)
                .clickable(onClick = onClick)
                .semanticsLabel(item.displayName),
            contentAlignment = Alignment.Center
        ) {
            val image = bitmap
            if (image != null) {
                Image(image, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            } else if (item.isImage) {
                Icon(Icons.Rounded.AttachFile, contentDescription = null, tint = AppColors.onSurfaceVariant)
            } else {
                Column(Modifier.fillMaxSize().padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 6.dp), verticalArrangement = Arrangement.SpaceBetween) {
                    Icon(fileIcon(item), contentDescription = null, tint = AppColors.primary, modifier = Modifier.size(20.dp))
                    Column {
                        Text(item.displayName, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(formatSize(item.sizeBytes), style = MaterialTheme.typography.labelSmall, color = AppColors.onSurfaceVariant)
                    }
                }
            }
        }
        // Not an IconButton: its 48dp minimum touch target would draw the scrim far outside the tile.
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp)
                .size(22.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.6f))
                .clickable(onClick = onRemove),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.Close, contentDescription = "Remove attachment", tint = Color.White, modifier = Modifier.size(14.dp))
        }
    }
}

private fun fileIcon(item: AttachmentItem): ImageVector =
    if (item.mimeType == "application/pdf") Icons.Rounded.PictureAsPdf else Icons.Rounded.Description

private fun formatSize(bytes: Long): String = when {
    bytes >= 1024 * 1024 -> "%.1f MB".format(bytes / (1024.0 * 1024.0))
    bytes >= 1024 -> "${bytes / 1024} KB"
    else -> "$bytes B"
}

/**
 * Hands the decrypted file to another app. It has to leave encrypted storage for that, so it goes
 * to the app cache, which the exporter-style purge below keeps short-lived.
 */
private suspend fun openExternally(
    context: android.content.Context,
    item: AttachmentItem,
    readBytes: suspend (AttachmentItem) -> ByteArray?
): Boolean {
    val bytes = readBytes(item) ?: return false
    val uri = withContext(Dispatchers.IO) {
        val dir = File(context.cacheDir, "shared").apply { mkdirs() }
        val cutoff = System.currentTimeMillis() - 60L * 60 * 1000
        dir.listFiles()?.filter { it.name.startsWith(OPEN_PREFIX) && it.lastModified() < cutoff }?.forEach { it.deleteRecursively() }
        // Own folder per open, so the file can keep a readable name (viewers show it as the title).
        // The display name is user data, so it is reduced to safe characters before it touches a path.
        val safe = item.displayName.replace(Regex("[^\\p{L}\\p{N}._ -]+"), "_").trim('.', ' ').takeLast(80).ifBlank { "Attachment" }
        val folder = File(dir, "$OPEN_PREFIX${System.currentTimeMillis()}").apply { mkdirs() }
        val file = File(folder, safe)
        file.writeBytes(bytes)
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }
    val view = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, item.mimeType)
        clipData = ClipData.newRawUri(item.displayName, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    return try {
        context.startActivity(Intent.createChooser(view, item.displayName).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))
        true
    } catch (_: ActivityNotFoundException) {
        false
    }
}

private const val OPEN_PREFIX = "OpenAtt_"

@Composable
private fun ImageViewer(item: AttachmentItem, readBytes: suspend (AttachmentItem) -> ByteArray?, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        // A dialog is its own window, so the activity's privacy flag doesn't cover it.
        val window = (LocalView.current.parent as? DialogWindowProvider)?.window
        SideEffect { window?.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE) }

        val bitmap = rememberAttachmentBitmap(item, readBytes, sampleSize = 1)
        var scale by remember { mutableStateOf(1f) }
        var offsetX by remember { mutableStateOf(0f) }
        var offsetY by remember { mutableStateOf(0f) }
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            val image = bitmap
            if (image == null) {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
            } else {
                Image(
                    image,
                    contentDescription = "Attachment photo",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                scale = (scale * zoom).coerceIn(1f, 5f)
                                if (scale == 1f) { offsetX = 0f; offsetY = 0f } else { offsetX += pan.x; offsetY += pan.y }
                            }
                        }
                        .graphicsLayer(scaleX = scale, scaleY = scale, translationX = offsetX, translationY = offsetY)
                )
            }
            IconButton(onClick = onDismiss, modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)) {
                Icon(Icons.Rounded.Close, contentDescription = "Close", tint = Color.White)
            }
        }
    }
}

@Composable
private fun rememberAttachmentBitmap(
    item: AttachmentItem,
    readBytes: suspend (AttachmentItem) -> ByteArray?,
    sampleSize: Int
): ImageBitmap? = produceState<ImageBitmap?>(null, item.key, sampleSize) {
    value = withContext(Dispatchers.Default) {
        readBytes(item)?.let { bytes ->
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sampleSize })
                ?.asImageBitmap()
        }
    }
}.value

private fun Modifier.semanticsLabel(label: String): Modifier =
    this.then(Modifier.semantics { contentDescription = label })
