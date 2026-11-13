package com.spendingbuddy.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** A theme-aware loading/error surface for real local app startup. */
@Composable
fun StartupSurface(failed: Boolean = false, onRetry: () -> Unit = {}) {
    val tokens = LocalPennyTokens.current
    val reducedMotion = LocalReducedMotion.current

    Box(
        Modifier.fillMaxSize().background(tokens.bg),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.widthIn(max = 320.dp).fillMaxWidth().padding(horizontal = 24.dp)
        ) {
            PennyCircleMark(Modifier.size(88.dp))
            Spacer(Modifier.height(22.dp))
            Text(
                "PENNY CIRCLE",
                style = MaterialTheme.typography.labelLarge.copy(fontFamily = PennyType.Mono, letterSpacing = 1.8.sp),
                color = tokens.muted
            )
            Spacer(Modifier.height(20.dp))

            if (failed) {
                Text(
                    "The app couldn't finish starting.",
                    style = MaterialTheme.typography.titleLarge,
                    color = tokens.text,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Your data was not reset. Try again when you're ready.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = tokens.muted,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(20.dp))
                Button(onClick = onRetry) { Text("Try again") }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    if (reducedMotion) {
                        StartupGlyph(Modifier.size(26.dp).semantics { contentDescription = "Getting things ready" })
                    } else {
                        CircularProgressIndicator(
                            Modifier.size(26.dp).semantics { contentDescription = "Getting things ready" },
                            color = tokens.accent,
                            strokeWidth = 2.5.dp
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "Getting things ready",
                        style = MaterialTheme.typography.titleMedium,
                        color = tokens.text
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "Opening your local notebook.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = tokens.muted,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun PennyCircleMark(modifier: Modifier = Modifier) {
    val t = LocalPennyTokens.current
    Canvas(
        modifier.semantics { contentDescription = "Penny Circle emblem" }.then(modifier)
    ) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val r = size.minDimension * 0.39f
        val stroke = size.minDimension * 0.035f
        drawCircle(t.surface, r + stroke * 1.8f, center = androidx.compose.ui.geometry.Offset(cx, cy))
        drawCircle(t.accent, r, center = androidx.compose.ui.geometry.Offset(cx, cy), style = Stroke(stroke))
        val sunRadius = r * 0.42f
        drawArc(
            color = t.accent2,
            startAngle = 180f,
            sweepAngle = -180f,
            useCenter = true,
            topLeft = androidx.compose.ui.geometry.Offset(cx - sunRadius, cy - sunRadius),
            size = androidx.compose.ui.geometry.Size(sunRadius * 2f, sunRadius * 2f)
        )
        drawLine(
            color = t.accent,
            start = androidx.compose.ui.geometry.Offset(cx - r * 0.62f, cy),
            end = androidx.compose.ui.geometry.Offset(cx + r * 0.62f, cy),
            strokeWidth = stroke * 0.72f,
            cap = StrokeCap.Round
        )
        drawCircle(t.accent3, radius = stroke * 0.72f, center = androidx.compose.ui.geometry.Offset(cx - r * 0.26f, cy + r * 0.35f))
        drawCircle(t.accent3, radius = stroke * 0.72f, center = androidx.compose.ui.geometry.Offset(cx, cy + r * 0.35f))
        drawCircle(t.accent3, radius = stroke * 0.72f, center = androidx.compose.ui.geometry.Offset(cx + r * 0.26f, cy + r * 0.35f))
    }
}

@Composable
private fun StartupGlyph(modifier: Modifier = Modifier) {
    val accent = LocalPennyTokens.current.accent
    Canvas(modifier) {
        val width = size.minDimension * 0.1f
        drawCircle(accent.copy(alpha = 0.24f), style = Stroke(width))
        drawArc(
            color = accent,
            startAngle = -90f,
            sweepAngle = 90f,
            useCenter = false,
            style = Stroke(width, cap = StrokeCap.Round)
        )
    }
}
