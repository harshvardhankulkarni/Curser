package app.curser.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.curser.core.HostOs
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

enum class Glyph { ChevronRight, ChevronLeft, Gear, Laptop, Monitor, Tablet, Bluetooth, Check, Plus, Keyboard, Mic }

fun HostOs.glyph(): Glyph = when (this) {
    HostOs.MAC -> Glyph.Laptop
    HostOs.WINDOWS -> Glyph.Monitor
    HostOs.IPAD -> Glyph.Tablet
}

@Composable
fun GlyphIcon(glyph: Glyph, tint: Color, modifier: Modifier = Modifier, size: Dp = 24.dp) {
    Canvas(modifier.size(size)) { drawGlyph(glyph, tint) }
}

/** All glyphs are drawn on a 24 x 24 grid scaled to the canvas. */
private fun DrawScope.drawGlyph(glyph: Glyph, tint: Color) {
    val u = size.minDimension / 24f
    fun o(x: Float, y: Float) = Offset(x * u, y * u)
    val stroke = Stroke(width = 1.8f * u, cap = StrokeCap.Round, join = StrokeJoin.Round)

    fun poly(vararg pts: Float, width: Float = 1.8f) {
        val p = Path()
        p.moveTo(pts[0] * u, pts[1] * u)
        var i = 2
        while (i < pts.size) {
            p.lineTo(pts[i] * u, pts[i + 1] * u)
            i += 2
        }
        drawPath(p, tint, style = Stroke(width = width * u, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }

    when (glyph) {
        Glyph.ChevronRight -> poly(9f, 5f, 16f, 12f, 9f, 19f, width = 2.2f)
        Glyph.ChevronLeft -> poly(15f, 5f, 8f, 12f, 15f, 19f, width = 2.4f)
        Glyph.Check -> poly(4.5f, 12.5f, 9.5f, 17.5f, 19.5f, 6.5f, width = 2.4f)
        Glyph.Plus -> {
            poly(12f, 5f, 12f, 19f, width = 2.2f)
            poly(5f, 12f, 19f, 12f, width = 2.2f)
        }
        Glyph.Bluetooth -> poly(7f, 7.5f, 17f, 16.5f, 12f, 21f, 12f, 3f, 17f, 7.5f, 7f, 16.5f)
        Glyph.Laptop -> {
            drawRoundRect(tint, o(4.5f, 5f), Size(15f * u, 10.5f * u), CornerRadius(1.6f * u), style = stroke)
            poly(2f, 18.5f, 22f, 18.5f)
        }
        Glyph.Monitor -> {
            drawRoundRect(tint, o(3f, 4f), Size(18f * u, 12f * u), CornerRadius(1.8f * u), style = stroke)
            poly(12f, 16f, 12f, 20f)
            poly(8f, 20f, 16f, 20f)
        }
        Glyph.Tablet -> {
            drawRoundRect(tint, o(5f, 2.5f), Size(14f * u, 19f * u), CornerRadius(2.6f * u), style = stroke)
            drawCircle(tint, radius = 0.9f * u, center = o(12f, 18.6f))
        }
        Glyph.Keyboard -> {
            drawRoundRect(tint, o(2.5f, 6f), Size(19f * u, 12f * u), CornerRadius(2.2f * u), style = stroke)
            for (x in listOf(6.5f, 10.2f, 13.8f, 17.5f)) drawCircle(tint, radius = 0.9f * u, center = o(x, 10f))
            poly(7.5f, 14.5f, 16.5f, 14.5f)
        }
        Glyph.Mic -> {
            drawRoundRect(tint, o(9f, 3f), Size(6f * u, 11f * u), CornerRadius(3f * u), style = stroke)
            val arc = Path()
            arc.moveTo(5.5f * u, 11f * u)
            arc.cubicTo(5.5f * u, 20f * u, 18.5f * u, 20f * u, 18.5f * u, 11f * u)
            drawPath(arc, tint, style = stroke)
            poly(12f, 17f, 12f, 21f)
        }
        Glyph.Gear -> {
            drawCircle(tint, radius = 6f * u, center = o(12f, 12f), style = stroke)
            drawCircle(tint, radius = 2.3f * u, center = o(12f, 12f), style = stroke)
            for (i in 0 until 8) {
                val a = i * (PI / 4).toFloat()
                drawLine(
                    tint,
                    o(12f + cos(a) * 6.6f, 12f + sin(a) * 6.6f),
                    o(12f + cos(a) * 9.6f, 12f + sin(a) * 9.6f),
                    strokeWidth = 2.6f * u,
                    cap = StrokeCap.Round,
                )
            }
        }
    }
}
