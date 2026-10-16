package com.osscabinet.ossWallet.ui.screens.transactions.entry.components

import android.content.ActivityNotFoundException
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddAPhoto
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
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
import com.osscabinet.ossWallet.ui.screens.transactions.entry.ReceiptItem
import com.osscabinet.ossWallet.ui.screens.transactions.entry.ReceiptsUiState
import com.osscabinet.ossWallet.ui.theme.AppColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

private val ThumbSize = 76.dp

/** Receipt photos for the transaction being edited: add from camera/gallery, view, remove. */
@Composable
fun ReceiptsSection(
    state: ReceiptsUiState,
    onPicked: (String) -> Unit,
    onRemove: (ReceiptItem) -> Unit,
    onDismissError: () -> Unit,
    readBytes: suspend (ReceiptItem) -> ByteArray?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var menuOpen by remember { mutableStateOf(false) }
    var viewing by remember { mutableStateOf<ReceiptItem?>(null) }
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

    EntrySection(title = "RECEIPTS", icon = Icons.Rounded.ReceiptLong, modifier = modifier) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(state.items, key = { it.key }) { item ->
                Thumbnail(item, readBytes, onClick = { viewing = item }, onRemove = { onRemove(item) })
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
                                .semanticsLabel("Add receipt"),
                            contentAlignment = Alignment.Center
                        ) {
                            if (state.busy) CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                            else Icon(Icons.Rounded.AddAPhoto, contentDescription = null, tint = AppColors.primary)
                        }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            DropdownMenuItem(
                                text = { Text("Take photo") },
                                leadingIcon = { Icon(Icons.Rounded.PhotoCamera, null) },
                                onClick = { menuOpen = false; launchCamera() }
                            )
                            DropdownMenuItem(
                                text = { Text("Choose from gallery") },
                                leadingIcon = { Icon(Icons.Rounded.PhotoLibrary, null) },
                                onClick = {
                                    menuOpen = false
                                    pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                }
                            )
                        }
                    }
                }
            }
        }
        val message = state.error ?: if (cameraMissing) "No camera app available" else null
        if (message != null) {
            Text(
                message,
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.error,
                modifier = Modifier.clickable { cameraMissing = false; onDismissError() }
            )
        } else if (state.items.isEmpty()) {
            Text(
                "Attach photos of receipts or invoices. They're stored encrypted on this device.",
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.onSurfaceVariant
            )
        }
    }

    viewing?.let { item -> ReceiptViewer(item, readBytes, onDismiss = { viewing = null }) }
}

@Composable
private fun Thumbnail(
    item: ReceiptItem,
    readBytes: suspend (ReceiptItem) -> ByteArray?,
    onClick: () -> Unit,
    onRemove: () -> Unit
) {
    val bitmap = rememberReceiptBitmap(item, readBytes, sampleSize = 4)
    Box(Modifier.size(ThumbSize)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(12.dp))
                .background(AppColors.surfaceContainerHigh)
                .clickable(onClick = onClick)
                .semanticsLabel("Receipt"),
            contentAlignment = Alignment.Center
        ) {
            val image = bitmap
            if (image != null) {
                Image(image, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            } else {
                Icon(Icons.Rounded.ReceiptLong, contentDescription = null, tint = AppColors.onSurfaceVariant)
            }
        }
        // Not an IconButton: its 48dp minimum touch target would draw the scrim far outside the thumbnail.
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
            Icon(Icons.Rounded.Close, contentDescription = "Remove receipt", tint = Color.White, modifier = Modifier.size(14.dp))
        }
    }
}

@Composable
private fun ReceiptViewer(item: ReceiptItem, readBytes: suspend (ReceiptItem) -> ByteArray?, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        // A dialog is its own window, so the activity's privacy flag doesn't cover it.
        val window = (LocalView.current.parent as? DialogWindowProvider)?.window
        SideEffect { window?.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE) }

        val bitmap = rememberReceiptBitmap(item, readBytes, sampleSize = 1)
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
                    contentDescription = "Receipt photo",
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
private fun rememberReceiptBitmap(
    item: ReceiptItem,
    readBytes: suspend (ReceiptItem) -> ByteArray?,
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
