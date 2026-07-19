package com.osscabinet.ossWallet.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme

private val ivory = Color(0xFFECE8DD)
private val champagneGold = Color(0xFFD6C189)

@Composable
fun AppLogo(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(8.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val s = size.width / 108f

            fun f(v: Float) = v * s

            // Upper-left wing — ivory
            drawPath(
                path = Path().apply {
                    moveTo(f(54f), f(54f))
                    cubicTo(f(49f), f(49f), f(33f), f(33f), f(25f), f(27f))
                    cubicTo(f(18f), f(22f), f(19f), f(31f), f(25f), f(37f))
                    cubicTo(f(31f), f(43f), f(47f), f(51f), f(54f), f(54f))
                    close()
                },
                color = ivory
            )

            // Upper-right wing — ivory
            drawPath(
                path = Path().apply {
                    moveTo(f(54f), f(54f))
                    cubicTo(f(59f), f(49f), f(75f), f(33f), f(83f), f(27f))
                    cubicTo(f(90f), f(22f), f(89f), f(31f), f(83f), f(37f))
                    cubicTo(f(77f), f(43f), f(61f), f(51f), f(54f), f(54f))
                    close()
                },
                color = ivory
            )

            // Lower-left wing — champagne gold
            drawPath(
                path = Path().apply {
                    moveTo(f(54f), f(54f))
                    cubicTo(f(49f), f(59f), f(35f), f(67f), f(29f), f(73f))
                    cubicTo(f(22f), f(79f), f(24f), f(86f), f(30f), f(83f))
                    cubicTo(f(38f), f(79f), f(49f), f(63f), f(54f), f(54f))
                    close()
                },
                color = champagneGold
            )

            // Lower-right wing — champagne gold
            drawPath(
                path = Path().apply {
                    moveTo(f(54f), f(54f))
                    cubicTo(f(59f), f(59f), f(73f), f(67f), f(79f), f(73f))
                    cubicTo(f(86f), f(79f), f(84f), f(86f), f(78f), f(83f))
                    cubicTo(f(70f), f(79f), f(59f), f(63f), f(54f), f(54f))
                    close()
                },
                color = champagneGold
            )

            // Center dot
            drawCircle(
                color = ivory.copy(alpha = 0.6f),
                radius = f(2f),
                center = androidx.compose.ui.geometry.Offset(f(54f), f(54f))
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0E1015)
@Composable
fun AppLogoPreview() {
    OssWalletTheme {
        Box(modifier = Modifier.size(200.dp).padding(20.dp)) {
            AppLogo()
        }
    }
}
