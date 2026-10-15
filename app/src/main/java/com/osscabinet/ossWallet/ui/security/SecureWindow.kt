package com.osscabinet.ossWallet.ui.security

import android.view.Window
import android.view.WindowManager

/** FLAG_SECURE blocks screenshots/screen-recording and blanks the recents thumbnail. */
fun Window.setSecure(secure: Boolean) {
    if (secure) addFlags(WindowManager.LayoutParams.FLAG_SECURE)
    else clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
}
