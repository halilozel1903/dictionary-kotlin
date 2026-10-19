package io.github.halilozel1903.dictionary.sample

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke

/** Small drawn icons, so the sample needs no icon library. */
@Composable
fun SpeakerIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val s = size.minDimension
        val body = Path().apply {
            moveTo(s * 0.12f, s * 0.38f)
            lineTo(s * 0.3f, s * 0.38f)
            lineTo(s * 0.52f, s * 0.18f)
            lineTo(s * 0.52f, s * 0.82f)
            lineTo(s * 0.3f, s * 0.62f)
            lineTo(s * 0.12f, s * 0.62f)
            close()
        }
        drawPath(body, color)
        val stroke = Stroke(width = s * 0.08f, cap = StrokeCap.Round)
        drawArc(color, -50f, 100f, false, Offset(s * 0.42f, s * 0.32f), Size(s * 0.36f, s * 0.36f), style = stroke)
        drawArc(color, -55f, 110f, false, Offset(s * 0.36f, s * 0.16f), Size(s * 0.64f, s * 0.68f), style = stroke)
    }
}

@Composable
fun SearchIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val s = size.minDimension
        drawCircle(color, radius = s * 0.26f, center = Offset(s * 0.42f, s * 0.42f), style = Stroke(width = s * 0.1f))
        drawLine(color, Offset(s * 0.62f, s * 0.62f), Offset(s * 0.86f, s * 0.86f), s * 0.12f, StrokeCap.Round)
    }
}

@Composable
fun BookIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val s = size.minDimension
        val stroke = Stroke(width = s * 0.08f, cap = StrokeCap.Round)
        val left = Path().apply {
            moveTo(s * 0.5f, s * 0.28f)
            quadraticTo(s * 0.32f, s * 0.18f, s * 0.1f, s * 0.22f)
            lineTo(s * 0.1f, s * 0.78f)
            quadraticTo(s * 0.32f, s * 0.74f, s * 0.5f, s * 0.84f)
            close()
        }
        val right = Path().apply {
            moveTo(s * 0.5f, s * 0.28f)
            quadraticTo(s * 0.68f, s * 0.18f, s * 0.9f, s * 0.22f)
            lineTo(s * 0.9f, s * 0.78f)
            quadraticTo(s * 0.68f, s * 0.74f, s * 0.5f, s * 0.84f)
            close()
        }
        drawPath(left, color, style = stroke)
        drawPath(right, color, style = stroke)
    }
}

@Composable
fun ClockIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val s = size.minDimension
        drawCircle(color, radius = s * 0.38f, center = Offset(s * 0.5f, s * 0.5f), style = Stroke(width = s * 0.09f))
        drawLine(color, Offset(s * 0.5f, s * 0.5f), Offset(s * 0.5f, s * 0.28f), s * 0.09f, StrokeCap.Round)
        drawLine(color, Offset(s * 0.5f, s * 0.5f), Offset(s * 0.66f, s * 0.6f), s * 0.09f, StrokeCap.Round)
    }
}
