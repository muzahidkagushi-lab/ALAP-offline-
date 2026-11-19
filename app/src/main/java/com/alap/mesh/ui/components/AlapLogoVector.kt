package com.alap.mesh.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.withTransform

@Composable
fun AlapLogoVector(
    modifier: Modifier = Modifier,
    color: Color = Color.White,
    tlOffset: Offset = Offset.Zero,
    tlScale: Float = 1f,
    tlAlpha: Float = 1f,
    brOffset: Offset = Offset.Zero,
    brScale: Float = 1f,
    brAlpha: Float = 1f
) {
    Canvas(modifier = modifier) {
        val sx = size.width / 200f
        val sy = size.height / 200f

        val tlBubblePath = Path().apply {
            moveTo(44f, 34f)
            lineTo(114f, 34f)
            cubicTo(125f, 34f, 133f, 42f, 133f, 53f)
            lineTo(133f, 55f)
            lineTo(108f, 55f)
            cubicTo(94f, 55f, 84f, 61f, 80f, 74f)
            lineTo(58f, 140f)
            lineTo(54f, 140f)
            lineTo(37f, 156f)
            cubicTo(34f, 159f, 30f, 157f, 30f, 153f)
            lineTo(30f, 138f)
            cubicTo(23f, 135f, 19f, 128f, 19f, 120f)
            lineTo(19f, 53f)
            cubicTo(19f, 42f, 27f, 34f, 44f, 34f)
            close()
        }

        val tlInnerTrianglePath = Path().apply {
            moveTo(103f, 77f)
            lineTo(93f, 102f)
            lineTo(112f, 102f)
            lineTo(112f, 77f)
            cubicTo(112f, 73f, 105f, 72f, 103f, 77f)
            close()
        }

        val brBubblePath = Path().apply {
            moveTo(134f, 66f)
            lineTo(162f, 66f)
            cubicTo(173f, 66f, 181f, 74f, 181f, 85f)
            lineTo(181f, 135f)
            cubicTo(181f, 146f, 173f, 154f, 162f, 154f)
            lineTo(153f, 154f)
            lineTo(166f, 170f)
            cubicTo(169f, 173f, 165f, 177f, 161f, 175f)
            lineTo(134f, 154f)
            lineTo(75f, 154f)
            lineTo(84f, 126f)
            lineTo(116f, 126f)
            lineTo(116f, 138f)
            cubicTo(116f, 143f, 120f, 146f, 125f, 146f)
            cubicTo(130f, 146f, 134f, 143f, 134f, 138f)
            lineTo(134f, 66f)
            close()
        }

        withTransform({
            scale(scaleX = sx, scaleY = sy, pivot = Offset.Zero)
            translate(left = tlOffset.x, top = tlOffset.y)
            scale(scaleX = tlScale, scaleY = tlScale, pivot = Offset(76f, 95f))
        }) {
            drawPath(path = tlBubblePath, color = color.copy(alpha = tlAlpha))
            drawPath(path = tlInnerTrianglePath, color = color.copy(alpha = tlAlpha))
        }

        withTransform({
            scale(scaleX = sx, scaleY = sy, pivot = Offset.Zero)
            translate(left = brOffset.x, top = brOffset.y)
            scale(scaleX = brScale, scaleY = brScale, pivot = Offset(128f, 120f))
        }) {
            drawPath(path = brBubblePath, color = color.copy(alpha = brAlpha))
        }
    }
}
