package com.elocho.snooker.ui.match

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import com.elocho.snooker.ui.theme.SnookerBlack
import com.elocho.snooker.ui.theme.SnookerBlue
import com.elocho.snooker.ui.theme.SnookerBrown
import com.elocho.snooker.ui.theme.SnookerGreen
import com.elocho.snooker.ui.theme.SnookerPink
import com.elocho.snooker.ui.theme.SnookerRed
import com.elocho.snooker.ui.theme.SnookerYellow

private data class BallPoint(val x: Float, val y: Float)

@Composable
fun SnookerTableVisual(
    state: SnookerTableState,
    modifier: Modifier = Modifier
) {
    val redPackPositions = remember { createRedPackPoints() }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1C0E04)),
        elevation = CardDefaults.cardElevation(defaultElevation = 14.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(5.dp)) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val ballR = h * 0.030f

                // ── CUSHION FRAME ────────────────────────────────────────────
                drawRoundRect(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF6B3A18),
                            Color(0xFF3D1F08),
                            Color(0xFF6B3A18)
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(w, h)
                    ),
                    cornerRadius = CornerRadius(14f, 14f),
                    size = Size(w, h)
                )
                // Cushion inner highlight edge
                drawRoundRect(
                    color = Color(0x25FFFFFF),
                    cornerRadius = CornerRadius(14f, 14f),
                    size = Size(w, h),
                    style = Stroke(width = 1.5f)
                )

                // ── FELT PLAYING SURFACE ─────────────────────────────────────
                val ci = h * 0.072f          // cushion inset
                val fL = ci; val fT = ci
                val fW = w - ci * 2f; val fH = h - ci * 2f

                // Base felt colour
                drawRoundRect(
                    color = Color(0xFF1F7A47),
                    topLeft = Offset(fL, fT),
                    size = Size(fW, fH),
                    cornerRadius = CornerRadius(7f, 7f)
                )
                // Radial centre highlight (overhead light)
                drawRoundRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF31A863),
                            Color(0xFF237A4E),
                            Color(0xFF195C38)
                        ),
                        center = Offset(w * 0.5f, h * 0.5f),
                        radius = w * 0.52f
                    ),
                    topLeft = Offset(fL, fT),
                    size = Size(fW, fH),
                    cornerRadius = CornerRadius(7f, 7f)
                )
                // Vignette edges
                drawRoundRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0x00000000),
                            Color(0x00000000),
                            Color(0x33000000)
                        ),
                        center = Offset(w * 0.5f, h * 0.5f),
                        radius = w * 0.56f
                    ),
                    topLeft = Offset(fL, fT),
                    size = Size(fW, fH),
                    cornerRadius = CornerRadius(7f, 7f)
                )
                // Felt border glow
                drawRoundRect(
                    color = Color(0x28FFFFFF),
                    topLeft = Offset(fL, fT),
                    size = Size(fW, fH),
                    cornerRadius = CornerRadius(7f, 7f),
                    style = Stroke(width = 1.5f)
                )

                // ── POCKETS ──────────────────────────────────────────────────
                drawPockets(w, h, ci)

                // ── BAULK LINE ───────────────────────────────────────────────
                val baulkX = fL + fW * 0.265f
                drawLine(
                    color = Color(0x99FFFFFF),
                    start = Offset(baulkX, fT + fH * 0.06f),
                    end = Offset(baulkX, fT + fH * 0.94f),
                    strokeWidth = 1.4f
                )

                // ── D SEMI-CIRCLE ─────────────────────────────────────────────
                val dR = fH * 0.295f
                val dCY = fT + fH * 0.5f
                drawArc(
                    color = Color(0x99FFFFFF),
                    startAngle = -90f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset(baulkX - dR, dCY - dR),
                    size = Size(dR * 2f, dR * 2f),
                    style = Stroke(width = 1.4f)
                )

                // Reference spots (subtle)
                val spotY = fT + fH * 0.5f
                // Pink spot
                drawCircle(
                    color = Color(0x55FFFFFF), radius = 2.8f,
                    center = Offset(fL + fW * 0.735f, spotY)
                )
                // Black spot
                drawCircle(
                    color = Color(0x55FFFFFF), radius = 2.8f,
                    center = Offset(fL + fW * 0.908f, spotY)
                )

                // ── BALLS ─────────────────────────────────────────────────────
                // Draw order: cue → green/brown/yellow → blue → pink →
                //             reds → BLACK LAST (always on top of reds)

                // Cue ball
                if (state.cueVisible) drawBall3D(0.115f, 0.500f, Color(0xFFF5F5F5), ballR)

                // Green (3) – left of D
                if (state.greenVisible) drawBall3D(0.248f, 0.320f, SnookerGreen, ballR)
                // Brown (4) – centre of baulk line
                if (state.brownVisible) drawBall3D(0.248f, 0.500f, SnookerBrown, ballR)
                // Yellow (2) – right of D
                if (state.yellowVisible) drawBall3D(0.248f, 0.680f, SnookerYellow, ballR)

                // Blue (5) – centre of table
                if (state.blueVisible) drawBall3D(0.500f, 0.500f, SnookerBlue, ballR)

                // Pink (6) – just ahead of triangle
                if (state.pinkVisible) drawBall3D(0.720f, 0.500f, SnookerPink, ballR)

                // Red triangle (drawn BEFORE black so black sits on top)
                redPackPositions.take(state.redsRemaining.coerceIn(0, 15)).forEach { pt ->
                    drawBall3D(pt.x, pt.y, SnookerRed, ballR)
                }

                // *** BLACK drawn LAST — always visible above any overlapping reds ***
                if (state.blackVisible) drawBall3D(0.912f, 0.500f, SnookerBlack, ballR)

                // ── BALL LABELS ───────────────────────────────────────────────
                if (state.greenVisible)  drawBallLabel("3",  0.248f, 0.320f, h)
                if (state.brownVisible)  drawBallLabel("4",  0.248f, 0.500f, h)
                if (state.yellowVisible) drawBallLabel("2",  0.248f, 0.680f, h, dark = true)
                if (state.blueVisible)   drawBallLabel("5",  0.500f, 0.500f, h)
                if (state.pinkVisible)   drawBallLabel("6",  0.720f, 0.500f, h, dark = true)
                if (state.blackVisible)  drawBallLabel("7",  0.912f, 0.500f, h)

                // Reds remaining count badge
                if (state.redsRemaining > 0) {
                    drawRedsLabel("×${state.redsRemaining}", 0.810f, 0.735f, h)
                }
            }
        }
    }
}

// ── RED TRIANGLE ──────────────────────────────────────────────────────────────
// apex at 0.758 · rows go to x≈0.870 · black is at 0.912 → clear 4% gap
private fun createRedPackPoints(): List<BallPoint> {
    val apexX = 0.758f
    val apexY = 0.500f
    val dx    = 0.028f   // row-to-row horizontal gap
    val dy    = 0.049f   // ball-to-ball vertical gap within a row

    val points = mutableListOf<BallPoint>()
    for (row in 1..5) {
        val x      = apexX + (row - 1) * dx
        val yStart = apexY - ((row - 1) * dy) / 2f
        for (col in 0 until row) {
            points.add(BallPoint(x, yStart + col * dy))
        }
    }
    return points
}

// ── 3D BALL RENDERER ─────────────────────────────────────────────────────────
private fun DrawScope.drawBall3D(
    nx: Float, ny: Float,
    color: Color,
    radius: Float
) {
    val cx = nx * size.width
    val cy = ny * size.height

    // Soft drop shadow
    drawCircle(
        color = Color.Black.copy(alpha = 0.42f),
        radius = radius * 1.08f,
        center = Offset(cx + radius * 0.20f, cy + radius * 0.24f)
    )

    // Ball body – radial gradient for sphere illusion
    val litColor = Color(
        red   = (color.red   * 1.35f).coerceAtMost(1f),
        green = (color.green * 1.35f).coerceAtMost(1f),
        blue  = (color.blue  * 1.35f).coerceAtMost(1f),
        alpha = 1f
    )
    val darkColor = Color(
        red   = color.red   * 0.50f,
        green = color.green * 0.50f,
        blue  = color.blue  * 0.50f,
        alpha = 1f
    )
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(litColor, color, darkColor),
            center = Offset(cx - radius * 0.26f, cy - radius * 0.26f),
            radius = radius * 1.55f
        ),
        radius = radius,
        center = Offset(cx, cy)
    )

    // Thin rim darkening at edge
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0x00000000), Color(0x00000000), Color(0x4D000000)),
            center = Offset(cx, cy),
            radius = radius
        ),
        radius = radius,
        center = Offset(cx, cy)
    )

    // Primary specular highlight (sharp, upper-left)
    drawCircle(
        color = Color.White.copy(alpha = 0.78f),
        radius = radius * 0.26f,
        center = Offset(cx - radius * 0.30f, cy - radius * 0.34f)
    )

    // Secondary soft glow highlight
    drawCircle(
        color = Color.White.copy(alpha = 0.18f),
        radius = radius * 0.56f,
        center = Offset(cx - radius * 0.16f, cy - radius * 0.18f)
    )
}

// ── BALL NUMBER LABEL ────────────────────────────────────────────────────────
private fun DrawScope.drawBallLabel(
    text: String,
    nx: Float, ny: Float,
    canvasH: Float,
    dark: Boolean = false
) {
    val textSize = canvasH * 0.034f
    val paint = Paint().apply {
        isAntiAlias = true
        textAlign   = Paint.Align.CENTER
        this.textSize = textSize
        typeface    = Typeface.DEFAULT_BOLD
        color = if (dark) android.graphics.Color.argb(220, 30, 20, 0)
                else android.graphics.Color.WHITE
        if (!dark) setShadowLayer(2.5f, 0.5f, 0.5f, android.graphics.Color.BLACK)
    }
    // vertically centre text inside the ball
    val textOffset = textSize * 0.36f
    drawContext.canvas.nativeCanvas.drawText(
        text,
        nx * size.width,
        ny * size.height + textOffset,
        paint
    )
}

// ── REDS REMAINING BADGE ──────────────────────────────────────────────────────
private fun DrawScope.drawRedsLabel(text: String, nx: Float, ny: Float, canvasH: Float) {
    val paint = Paint().apply {
        isAntiAlias = true
        textAlign   = Paint.Align.CENTER
        textSize    = canvasH * 0.032f
        typeface    = Typeface.DEFAULT_BOLD
        color       = android.graphics.Color.WHITE
        setShadowLayer(3f, 0f, 1f, android.graphics.Color.BLACK)
        alpha = 200
    }
    drawContext.canvas.nativeCanvas.drawText(
        text,
        nx * size.width,
        ny * size.height,
        paint
    )
}

// ── POCKETS ──────────────────────────────────────────────────────────────────
private fun DrawScope.drawPockets(w: Float, h: Float, inset: Float) {
    val pr   = inset * 0.90f
    val half = w * 0.5f

    val positions = listOf(
        Offset(inset * 0.46f, inset * 0.46f),
        Offset(half,           inset * 0.30f),
        Offset(w - inset * 0.46f, inset * 0.46f),
        Offset(inset * 0.46f, h - inset * 0.46f),
        Offset(half,           h - inset * 0.30f),
        Offset(w - inset * 0.46f, h - inset * 0.46f)
    )

    positions.forEach { p ->
        // Outer dark ring
        drawCircle(color = Color(0xFF110A03), radius = pr,        center = p)
        // Deep pocket interior
        drawCircle(color = Color(0xFF060302), radius = pr * 0.68f, center = p)
        // Inner leather-net hint gradient
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0x00000000), Color(0x55000000)),
                center = p, radius = pr * 0.68f
            ),
            radius = pr * 0.68f,
            center = p
        )
        // Subtle rim catchlight
        drawCircle(
            color = Color(0x20FFFFFF),
            radius = pr,
            center = p,
            style = Stroke(width = 1f)
        )
    }
}
