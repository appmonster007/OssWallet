package com.osscabinet.ossWallet.ui.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Primitive dimension values for the Aeon Design Language.
 * Standardizes all spatial values to a strict 2dp grid.
 */
enum class AeonSize(val value: Dp) {
    D2(2.dp), D4(4.dp), D6(6.dp), D8(8.dp), D10(10.dp),
    D12(12.dp), D14(14.dp), D16(16.dp), D18(18.dp), D20(20.dp),
    D22(22.dp), D24(24.dp), D26(26.dp), D28(28.dp), D30(30.dp),
    D32(32.dp), D34(34.dp), D36(36.dp), D38(38.dp), D40(40.dp),
    D42(42.dp), D44(44.dp), D46(46.dp), D48(48.dp), D50(50.dp),
    D52(52.dp), D54(54.dp), D56(56.dp), D58(58.dp), D60(60.dp),
    D64(64.dp) // Extended to 64dp for existing large icon sizes
}
