package dev.jarful.ui.jar

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.jarful.ui.i18n.LocalStrings
import dev.jarful.ui.theme.JarfulColors
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * The transparent jar (FR-5). Draws [count] crumpled paper balls stacked from the bottom.
 * When [dropSignal] changes, the newest ball falls in from the top.
 */
@Composable
fun JarView(count: Int, dropSignal: Int, modifier: Modifier = Modifier, height: Dp = 260.dp, showLabel: Boolean = true) {
    val strings = LocalStrings.current
    val drop = remember { Animatable(1f) }
    var lastSignal by remember { mutableIntStateOf(dropSignal) }
    LaunchedEffect(dropSignal) {
        if (dropSignal != lastSignal) {
            lastSignal = dropSignal
            drop.snapTo(0f)
            drop.animateTo(1f, tween(650, easing = FastOutLinearInEasing))
        }
    }
    val glass = MaterialTheme.colorScheme.tertiary
    val outline = MaterialTheme.colorScheme.onSurfaceVariant
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Canvas(modifier = Modifier.fillMaxWidth().height(height)) {
            drawJar(count, drop.value, glass, outline)
        }
        if (showLabel) {
            Text(strings.loopsToday(count), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

private fun DrawScope.drawJar(count: Int, dropProgress: Float, glass: Color, outline: Color) {
    val w = size.width; val h = size.height
    val jarW = minOf(w * 0.8f, h * 0.7f)
    val left = (w - jarW) / 2f
    val neckH = h * 0.12f
    val bodyTop = neckH + h * 0.04f
    val bodyRect = Rect(left, bodyTop, left + jarW, h - 4f)
    val body = Path().apply { addRoundRect(androidx.compose.ui.geometry.RoundRect(bodyRect, CornerRadius(jarW * 0.12f))) }

    // glass fill + outline
    drawPath(body, glass.copy(alpha = 0.16f))
    // lid / neck
    val neckW = jarW * 0.7f
    drawRoundRect(outline.copy(alpha = 0.35f), Offset(left + (jarW - neckW) / 2f, 2f), Size(neckW, neckH), CornerRadius(8f))

    // paper balls
    val r = (jarW / 7.2f).coerceAtLeast(8f)
    val cols = ((jarW - r) / (r * 1.9f)).toInt().coerceAtLeast(1)
    val rnd = Random(42)
    val positions = ArrayList<Offset>()
    for (i in 0 until count) {
        val row = i / cols; val col = i % cols
        val stagger = if (row % 2 == 1) r * 0.95f else 0f
        val jitterX = (rnd.nextFloat() - 0.5f) * r * 0.5f
        val jitterY = (rnd.nextFloat() - 0.5f) * r * 0.2f
        val x = bodyRect.left + r * 1.15f + col * r * 1.9f + stagger + jitterX
        val y = bodyRect.bottom - r * 1.1f - row * r * 1.7f + jitterY
        positions.add(Offset(x.coerceIn(bodyRect.left + r, bodyRect.right - r), y))
    }
    clipPath(body) {
        positions.forEachIndexed { i, p ->
            val isNew = i == count - 1 && dropProgress < 1f
            val y = if (isNew) (-r) + (p.y + r) * dropProgress else p.y
            val rot = if (isNew) dropProgress * 2f * PI.toFloat() else (i * 37 % 360) / 180f * PI.toFloat()
            drawPaperBall(Offset(p.x, y), r * (0.85f + (i * 13 % 7) / 20f), rot)
        }
    }
    drawPath(body, outline.copy(alpha = 0.8f), style = Stroke(width = 3f))
    // highlight
    drawLine(Color.White.copy(alpha = 0.6f), Offset(bodyRect.left + jarW * 0.12f, bodyTop + 24f), Offset(bodyRect.left + jarW * 0.12f, bodyRect.bottom - 40f), strokeWidth = 5f)
}

private fun DrawScope.drawPaperBall(center: Offset, r: Float, rotation: Float) {
    drawCircle(JarfulColors.PaperBallShade, r, center + Offset(2f, 3f))
    drawCircle(JarfulColors.PaperBall, r, center)
    // crumple creases: a few short chords
    val n = 6
    for (k in 0 until n) {
        val a = rotation + k * (2f * PI.toFloat() / n)
        val len = r * 0.7f
        val cx = center.x + cos(a) * r * 0.35f; val cy = center.y + sin(a) * r * 0.35f
        val dx = cos(a + 1.1f) * len / 2f; val dy = sin(a + 1.1f) * len / 2f
        drawLine(JarfulColors.PaperBallShade, Offset(cx - dx, cy - dy), Offset(cx + dx, cy + dy), strokeWidth = 1.5f)
    }
    drawCircle(Color.White.copy(alpha = 0.5f), r * 0.25f, center + Offset(-r * 0.3f, -r * 0.3f))
}

@Suppress("unused")
private fun dist(a: Offset, b: Offset): Float { val dx = a.x - b.x; val dy = a.y - b.y; return sqrt(dx * dx + dy * dy) }
