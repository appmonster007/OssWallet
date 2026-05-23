package com.osscabinet.ossWallet.data.web

import android.graphics.Bitmap
import qrcode.QRCode

object QRCodeGenerator {
    // withSize() sets per-cell pixels, not total image size.
    // A typical QR code has ~25-33 modules; cellSizePx=10 → ~330px total image.
    fun generate(text: String, cellSizePx: Int = 10): Bitmap {
        return QRCode.ofSquares()
            .withSize(cellSizePx)
            .build(text)
            .render()
            .nativeImage() as Bitmap
    }
}
