package com.osscabinet.gleemanager.ui.util

import androidx.compose.ui.graphics.Color

fun Color.isDark(): Boolean {
    val luminance = 0.299 * red + 0.587 * green + 0.114 * blue
    return luminance < 0.5
}
