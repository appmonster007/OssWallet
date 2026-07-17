package com.osscabinet.ossWallet.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme

@Composable
fun AppLogo(
    modifier: Modifier = Modifier,
    contentColor: Color = Color.Unspecified
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(8.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val centerX = canvasWidth / 2
            
            val bodyWidth = canvasWidth * 0.04f
            val bodyHeight = canvasHeight * 0.4f
            val bodyX = centerX - (bodyWidth / 2)
            val bodyY = (canvasHeight - bodyHeight) / 2

            // Multi-colored Palette
            val topLeftColor = Color(0xFF8AB4F8) // Pastel Blue
            val topRightColor = Color(0xFFF28B82) // Pastel Red
            val bottomLeftColor = Color(0xFF81C995) // Pastel Green
            val bottomRightColor = Color(0xFF80DEEA) // Pastel Cyan
            val bodyColor = if (contentColor != Color.Unspecified) contentColor else Color(0xFF5F6368)

            // Dimensions for wings (Symmetric shapes)
            val topWingWidth = canvasWidth * 0.24f
            val topWingHeight = canvasHeight * 0.22f
            val bottomWingWidth = canvasWidth * 0.18f
            val bottomWingHeight = canvasHeight * 0.17f
            
            val topWingY = canvasHeight * 0.26f
            val bottomWingY = canvasHeight * 0.52f
            val wingGap = canvasWidth * 0.02f // Gap from center

            // Top Left Wing
            rotate(-25f, pivot = Offset(centerX - wingGap, canvasHeight * 0.48f)) {
                drawRoundRect(
                    color = topLeftColor,
                    topLeft = Offset(centerX - wingGap - topWingWidth, topWingY),
                    size = Size(topWingWidth, topWingHeight),
                    cornerRadius = CornerRadius(canvasWidth * 0.04f)
                )
            }

            // Top Right Wing
            rotate(25f, pivot = Offset(centerX + wingGap, canvasHeight * 0.48f)) {
                drawRoundRect(
                    color = topRightColor,
                    topLeft = Offset(centerX + wingGap, topWingY),
                    size = Size(topWingWidth, topWingHeight),
                    cornerRadius = CornerRadius(canvasWidth * 0.04f)
                )
            }

            // Bottom Left Wing
            rotate(20f, pivot = Offset(centerX - wingGap, canvasHeight * 0.52f)) {
                drawRoundRect(
                    color = bottomLeftColor,
                    topLeft = Offset(centerX - wingGap - bottomWingWidth, bottomWingY),
                    size = Size(bottomWingWidth, bottomWingHeight),
                    cornerRadius = CornerRadius(canvasWidth * 0.04f)
                )
            }

            // Bottom Right Wing
            rotate(-20f, pivot = Offset(centerX + wingGap, canvasHeight * 0.52f)) {
                drawRoundRect(
                    color = bottomRightColor,
                    topLeft = Offset(centerX + wingGap, bottomWingY),
                    size = Size(bottomWingWidth, bottomWingHeight),
                    cornerRadius = CornerRadius(canvasWidth * 0.04f)
                )
            }

            // Central Body
            drawRoundRect(
                color = bodyColor,
                topLeft = Offset(bodyX, bodyY),
                size = Size(bodyWidth, bodyHeight),
                cornerRadius = CornerRadius(bodyWidth / 2)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AppLogoPreview() {
    OssWalletTheme {
        Box(modifier = Modifier.size(200.dp).padding(20.dp)) {
            AppLogo()
        }
    }
}
