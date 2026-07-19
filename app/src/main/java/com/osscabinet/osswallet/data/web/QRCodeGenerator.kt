package com.osscabinet.ossWallet.data.web

import android.graphics.Bitmap
import qrcode.QRCode
import qrcode.color.Colors

object QRCodeGenerator {
    /**
     * Generates a QR code bitmap.
     * [color] sets the color of the QR modules (dots).
     * [backgroundColor] sets the color of the background/quiet zone.
     */
    fun generate(
        text: String,
        cellSizePx: Int = 10,
        color: Int = Colors.BLACK,
        backgroundColor: Int = Colors.WHITE
    ): Bitmap {
        return QRCode.ofSquares()
            .withSize(cellSizePx)
            .withColor(color)
            .withBackgroundColor(backgroundColor)
            .build(text)
            .render()
            .nativeImage() as Bitmap
    }
}
