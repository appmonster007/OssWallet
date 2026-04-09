package com.osscabinet.gleemanager.ui.components

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.IOException

@Composable
fun OCRScanner(
    onResult: (Double?) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val recognizer = remember { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val image = InputImage.fromFilePath(context, uri)
                recognizer.process(image)
                    .addOnSuccessListener { visionText ->
                        val amount = extractAmount(visionText.text)
                        onResult(amount)
                    }
                    .addOnFailureListener {
                        onResult(null)
                    }
            } catch (e: IOException) {
                onResult(null)
            }
        } else {
            onDismiss()
        }
    }

    LaunchedEffect(Unit) {
        launcher.launch("image/*")
    }
}

fun extractAmount(text: String): Double? {
    // Basic regex to find potential currency amounts
    val regex = Regex("""\d+[.,]\d{2}""")
    val matches = regex.findAll(text)
    
    // Often the largest amount on a receipt is the total
    return matches.map { it.value.replace(",", ".").toDouble() }.maxOrNull()
}
